package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfReceiver;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.AbstractScpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.ScpsiServerOutput;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19.Psty19ScpsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PSTY19 server-payload circuit PSI server.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class Psty19ScpsiServer extends AbstractScpsiServer {
    /**
     * batched OPPRF receiver
     */
    private final BopprfReceiver bopprfReceiver;
    /**
     * private equality test sender
     */
    private final PeqtParty peqtSender;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * cuckoo hash num
     */
    private final int cuckooHashNum;
    /**
     * cuckoo hash bin
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;

    public Psty19ScpsiServer(Rpc serverRpc, Party clientParty, Psty19ScpsiConfig config) {
        super(Psty19ScpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        bopprfReceiver = BopprfFactory.createReceiver(serverRpc, clientParty, config.getBopprfConfig());
        addSubPtos(bopprfReceiver);
        peqtSender = PeqtFactory.createSender(serverRpc, clientParty, config.getPeqtConfig());
        addSubPtos(peqtSender);
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init batched OPPRF, where β_max = (1 + ε) * n_s, max_point_num = hash_num * n_c
        int maxBeta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerElementSize);
        int maxPointNum = cuckooHashNum * maxClientElementSize;
        bopprfReceiver.init(maxBeta, maxPointNum);
        // init private equality test, where maxL = σ + log_2(β_max) + log_2(max_point_num)
        int maxL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(maxBeta) + LongUtils.ceilLog2(maxPointNum);
        peqtSender.init(maxL, maxBeta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public ScpsiServerOutput psi(Set<ByteBuffer> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // β = (1 + ε) * n_s
        int beta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverElementSize);
        // point_num = hash_num * n_c
        int pointNum = cuckooHashNum * clientElementSize;
        // l = σ + log_2(β) + log_2(point_num)
        int l = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(beta) + LongUtils.ceilLog2(pointNum);
        // P1 inserts items into no-stash cuckoo hash bin Table_1 with β bins.
        List<byte[]> cuckooHashKeyPayload = generateCuckooHashKeyPayload();
        // P1 sends the cuckoo hash bin keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long binTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, binTime, "Server inserts cuckoo hash");

        stopWatch.start();
        // The parties invoke a batched OPPRF.
        // P1 inputs Table_1[1], . . . , Table_1[β] and receives y_1^*, ..., y_β^*
        byte[][] inputArray = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                HashBinEntry<ByteBuffer> item = cuckooHashBin.getHashBinEntry(batchIndex);
                byte[] itemBytes = cuckooHashBin.getHashBinEntry(batchIndex).getItemByteArray();
                return ByteBuffer.allocate(itemBytes.length + Integer.BYTES)
                    .put(itemBytes)
                    .putInt(item.getHashIndex())
                    .array();
            })
            .toArray(byte[][]::new);
        byte[][] targetArray = bopprfReceiver.opprf(l, inputArray, pointNum);
        stopWatch.stop();
        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, opprfTime);

        stopWatch.start();
        // The parties invoke a private equality test with l = σ + log_2(β) + log_2(point_num).
        // P1 inputs y_1^*, ..., y_β^* and outputs z0.
        SquareShareZ2Vector z0 = peqtSender.peqt(l, targetArray);
        // create the table
        ByteBuffer[] table = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                HashBinEntry<ByteBuffer> item = cuckooHashBin.getHashBinEntry(batchIndex);
                if (item.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                    return ByteBuffer.wrap(new byte[0]);
                } else {
                    return item.getItem();
                }
            })
            .toArray(ByteBuffer[]::new);
        ScpsiServerOutput serverOutput = new ScpsiServerOutput(table, z0);
        cuckooHashBin = null;
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, peqtTime);

        logPhaseInfo(PtoState.PTO_END);
        return serverOutput;
    }

    private List<byte[]> generateCuckooHashKeyPayload() {
        cuckooHashBin = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, serverElementSize, serverElementArrayList, secureRandom
        );
        // pad random elements into the cuckoo hash
        cuckooHashBin.insertPaddingItems(secureRandom);
        return Arrays.stream(cuckooHashBin.getHashKeys()).collect(Collectors.toList());
    }
}
