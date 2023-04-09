package edu.alibaba.mpc4j.s2pc.opf.opprf.bopprf.table;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.s2pc.opf.opprf.bopprf.AbstractBopprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.opprf.bopprf.table.TableBopprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Table Batch OPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
public class TableBopprfReceiver extends AbstractBopprfReceiver {
    /**
     * the OPRF receiver
     */
    private final OprfReceiver oprfReceiver;

    public TableBopprfReceiver(Rpc receiverRpc, Party senderParty, TableBopprfConfig config) {
        super(TableBopprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        oprfReceiver = OprfFactory.createOprfReceiver(receiverRpc, senderParty, config.getOprfConfig());
        addSubPtos(oprfReceiver);
    }

    @Override
    public void init(int maxBatchSize, int maxPointNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPointNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        oprfReceiver.init(maxBatchSize, maxPointNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] opprf(int l, byte[][] inputArray, int targetNum) throws MpcAbortException {
        setPtoInput(l, inputArray, targetNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // OPRF
        OprfReceiverOutput oprfReceiverOutput = oprfReceiver.oprf(inputArray);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Receiver runs OPRF");

        // receive vs
        DataPacketHeader vsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SEND_VS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> vsPayload = rpc.receive(vsHeader).getPayload();
        // receive tables
        DataPacketHeader tablesHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SENDER_SENDS_TABLES.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> tablesPayload = rpc.receive(tablesHeader).getPayload();

        stopWatch.start();
        byte[][] outputArray = handleTablesPayload(oprfReceiverOutput, vsPayload, tablesPayload);
        stopWatch.stop();
        long tableTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, tableTime, "Receiver handles tables");

        logPhaseInfo(PtoState.PTO_END);
        return outputArray;
    }

    private byte[][] handleTablesPayload(OprfReceiverOutput oprfReceiverOutput,
                                         List<byte[]> vsPayload, List<byte[]> tablesPayload) throws MpcAbortException {
        // parse keys
        MpcAbortPreconditions.checkArgument(vsPayload.size() == batchSize);
        byte[][] vs = vsPayload.toArray(new byte[0][]);
        // parse tables, m = 2^{log_2(n+1)}
        int m = 1 << ((int) Math.ceil(DoubleUtils.log2(maxBatchPointNum + 1)));
        MpcAbortPreconditions.checkArgument(tablesPayload.size() == batchSize * m);
        byte[][] flatTables = tablesPayload.toArray(new byte[0][]);
        byte[][][] tables = new byte[batchSize][][];
        for (int batchIndex = 0; batchIndex < batchSize; batchIndex++) {
            tables[batchIndex] = new byte[m][];
            System.arraycopy(flatTables, batchIndex * m, tables[batchIndex], 0, m);
        }
        // the random oracle H: {0, 1}^* → {0, 1}^m
        Prf h = PrfFactory.createInstance(envType, Integer.BYTES);
        h.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        // The PRF maps (random) inputs to {0, 1}^l, we only need to set an empty key
        Prf prf = PrfFactory.createInstance(envType, byteL);
        prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        // compute the output for each batch index
        IntStream batchIntStream = IntStream.range(0, batchSize);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        return batchIntStream
            .mapToObj(batchIndex -> {
                byte[] fq = oprfReceiverOutput.getPrf(batchIndex);
                byte[] lq = prf.getBytes(fq);
                // R computes h = H(F(k, q) || v), and outputs (T, v, T_h ⊕ F(k, q)).
                byte[] fqv = ByteBuffer.allocate(fq.length + CommonConstants.BLOCK_BYTE_LENGTH)
                    .put(fq)
                    .put(vs[batchIndex])
                    .array();
                int hi = h.getInteger(fqv, m);
                return BytesUtils.xor(tables[batchIndex][hi], lq);
            })
            .toArray(byte[][]::new);
    }
}
