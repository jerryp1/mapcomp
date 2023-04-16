package edu.alibaba.mpc4j.s2pc.opf.opprf.bopprf.table;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.s2pc.opf.opprf.bopprf.AbstractBopprfSender;
import edu.alibaba.mpc4j.s2pc.opf.opprf.bopprf.table.TableBopprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Table Batch OPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
public class TableBopprfSender extends AbstractBopprfSender {
    /**
     * the OPRF sender
     */
    private final OprfSender oprfSender;
    /**
     * vs
     */
    private byte[][] vs;
    /**
     * tables
     */
    private byte[][][] tables;

    public TableBopprfSender(Rpc senderRpc, Party receiverParty, TableBopprfConfig config) {
        super(TableBopprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        oprfSender = OprfFactory.createOprfSender(senderRpc, receiverParty, config.getOprfConfig());
        addSubPtos(oprfSender);
    }

    @Override
    public void init(int maxBatchSize, int maxPointNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPointNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        oprfSender.init(maxBatchSize, maxPointNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void opprf(int l, byte[][][] inputArrays, byte[][][] targetArrays) throws MpcAbortException {
        setPtoInput(l, inputArrays, targetArrays);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // OPRF
        OprfSenderOutput oprfSenderOutput = oprfSender.oprf(batchSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Sender runs OPRF");

        stopWatch.start();
        // generate table
        generateTables(oprfSenderOutput);
        // send vs
        List<byte[]> vsPayload = Arrays.stream(vs).collect(Collectors.toList());
        DataPacketHeader vsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_VS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(vsHeader, vsPayload));
        // send tables
        List<byte[]> tablesPayload = Arrays.stream(tables)
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        DataPacketHeader tablesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SENDS_TABLES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(tablesHeader, tablesPayload));
        vs = null;
        tables = null;
        stopWatch.stop();
        long tableTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, tableTime, "Sender sends tables");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void generateTables(OprfSenderOutput oprfSenderOutput) {
        int m = 1 << ((int) Math.ceil(DoubleUtils.log2(maxBatchPointNum + 1)));
        // the random oracle H: {0, 1}^* → {0, 1}^m
        Prf h = PrfFactory.createInstance(envType, Integer.BYTES);
        h.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        // The PRF maps (random) inputs to {0, 1}^l, we only need to set an empty key
        Prf prf = PrfFactory.createInstance(envType, byteL);
        prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        // generate the table for each batch index.
        vs = new byte[batchSize][];
        tables = new byte[batchSize][][];
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        batchIntStream.forEach(batchIndex -> {
            // x_1, ..., x_n
            byte[][] x = inputArrays[batchIndex];
            // y_1, ..., y_n
            byte[][] y = targetArrays[batchIndex];
            assert x.length == y.length;
            int n = x.length;
            // F(k, x_i)
            byte[][] fx = Arrays.stream(x)
                .map(xi -> oprfSenderOutput.getPrf(batchIndex, xi))
                .toArray(byte[][]::new);
            // F(k, x_i) mapped to {0, 1}^l
            byte[][] lx = Arrays.stream(fx)
                .map(prf::getBytes)
                .peek(prfOutput -> BytesUtils.reduceByteArray(prfOutput, l))
                .toArray(byte[][]::new);
            vs[batchIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            boolean distinct = false;
            int[] hs = null;
            while (!distinct) {
                // S samples v ← {0, 1}^κ until {H(F(k, x_i) || v) | i ∈ [n]} are all distinct.
                secureRandom.nextBytes(vs[batchIndex]);
                hs = Arrays.stream(fx)
                    .map(fxi ->
                        ByteBuffer.allocate(fxi.length + CommonConstants.BLOCK_BYTE_LENGTH)
                            .put(fxi)
                            .put(vs[batchIndex])
                            .array())
                    .mapToInt(fxiv -> h.getInteger(fxiv, m))
                    .toArray();
                long distinctCount = Arrays.stream(hs).distinct().count();
                distinct = (distinctCount == n);
            }
            // For i ∈ [n], S computes h_i = H(F(k, xi) || v), and sets T_{hi} = F(k, x_i) ⊕ y_i.
            tables[batchIndex] = new byte[m][];
            for (int i = 0; i < n; i++) {
                tables[batchIndex][hs[i]] = BytesUtils.xor(lx[i], y[i]);
                BytesUtils.reduceByteArray(tables[batchIndex][hs[i]], l);
            }
            // For j ∈ {0, 1}^m \ {h_i | i ∈ [n]}, S sets T_j ← {0, 1}^r.
            for (int i = 0; i < tables[batchIndex].length; i++) {
                if (tables[batchIndex][i] == null) {
                    tables[batchIndex][i] = new byte[byteL];
                    BytesUtils.randomByteArray(byteL, l, secureRandom);
                }
            }
        });
    }
}
