package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfReceiver;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.BopprfPlpsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * abstract batched OPPRF-based payload-circuit PSI client.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public abstract class AbstractBopprfPlpsiClient<T> extends AbstractPlpsiClient<T> {
    /**
     * batched OPPRF receiver
     */
    private final BopprfReceiver bopprfReceiver;
    /**
     * private equality test receiver
     */
    private final PeqtParty peqtReceiver;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * cuckoo hash num
     */
    private final int hashNum;
    /**
     * cuckoo hash bin
     */
    private CuckooHashBin<T> cuckooHashBin;

    private byte[][] inputArray;

    private PlpsiClientOutput<T> clientOutput;

    public AbstractBopprfPlpsiClient(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, BopprfPlpsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
        bopprfReceiver = BopprfFactory.createReceiver(serverRpc, clientParty, config.getBopprfConfig());
        addSubPtos(bopprfReceiver);
        peqtReceiver = PeqtFactory.createSender(serverRpc, clientParty, config.getPeqtConfig());
        addSubPtos(peqtReceiver);
        cuckooHashBinType = config.getCuckooHashBinType();
        hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init batched OPPRF, where β_max = (1 + ε) * n_c, max_point_num = hash_num * n_s
        int maxBeta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize);
        int maxPointNum = hashNum * maxServerElementSize;
        bopprfReceiver.init(maxBeta, maxPointNum);
        // init private equality test, where max(l_peqt) = σ + log_2(β_max)
        int maxPeqtL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(maxBeta);
        peqtReceiver.init(maxPeqtL, maxBeta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PlpsiClientOutput<T> psi(List<T> clientElementList, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementList, serverElementSize);
        return psiCommonPart(false);
//        logPhaseInfo(PtoState.PTO_BEGIN);
//
//        stopWatch.start();
//        // β = (1 + ε) * n_c
//        int beta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
//        // point_num = hash_num * n_s
//        int pointNum = hashNum * serverElementSize;
//        // l_peqt = σ + log_2(β)
//        int peqtL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(beta);
//        int peqtByteL = CommonUtils.getByteLength(peqtL);
//        // l_opprf = σ + log_2(point_num)
//        int opprfL = Math.max(CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(pointNum), peqtL);
//        int opprfByteL = CommonUtils.getByteLength(opprfL);
//        // P2 inserts items into no-stash cuckoo hash bin Table_1 with β bins.
//        List<byte[]> cuckooHashKeyPayload = generateCuckooHashKeyPayload();
//        // P2 sends the cuckoo hash bin keys
//        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
//            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
//            ownParty().getPartyId(), otherParty().getPartyId()
//        );
//        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
//        stopWatch.stop();
//        long binTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        logStepInfo(PtoState.PTO_STEP, 1, 3, binTime, "Server inserts cuckoo hash");
//
//        stopWatch.start();
//        // The parties invoke a batched OPPRF.
//        // P2 inputs Table_1[1], . . . , Table_1[β] and receives y_1^*, ..., y_β^*
//        inputArray = IntStream.range(0, beta)
//            .mapToObj(batchIndex -> {
//                HashBinEntry<T> item = cuckooHashBin.getHashBinEntry(batchIndex);
//                byte[] itemBytes = cuckooHashBin.getHashBinEntry(batchIndex).getItemByteArray();
//                return ByteBuffer.allocate(itemBytes.length + Integer.BYTES)
//                    .put(itemBytes)
//                    .putInt(item.getHashIndex())
//                    .array();
//            })
//            .toArray(byte[][]::new);
//        byte[][] targetArray = bopprfReceiver.opprf(opprfL, inputArray, pointNum);
//        stopWatch.stop();
//        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        logStepInfo(PtoState.PTO_STEP, 2, 3, opprfTime);
//
//        stopWatch.start();
//        // The parties invoke a private equality test
//        targetArray = Arrays.stream(targetArray)
//            .map(target -> {
//                byte[] truncatedTarget = new byte[peqtByteL];
//                System.arraycopy(target, opprfByteL - peqtByteL, truncatedTarget, 0, peqtByteL);
//                BytesUtils.reduceByteArray(truncatedTarget, peqtL);
//                return truncatedTarget;
//            })
//            .toArray(byte[][]::new);
//        // P2 inputs y_1^*, ..., y_β^* and outputs z1.
//        SquareZ2Vector z1 = peqtReceiver.peqt(peqtL, targetArray);
//        // create the table
//        ArrayList<T> table = IntStream.range(0, beta)
//            .mapToObj(batchIndex -> {
//                HashBinEntry<T> item = cuckooHashBin.getHashBinEntry(batchIndex);
//                if (item.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
//                    return null;
//                } else {
//                    return item.getItem();
//                }
//            })
//            .collect(Collectors.toCollection(ArrayList::new));
//        clientOutput = new PlpsiClientOutput<>(table, z1);
//        stopWatch.stop();
//        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        logStepInfo(PtoState.PTO_STEP, 3, 3, peqtTime);
//
//        logPhaseInfo(PtoState.PTO_END);
//        return clientOutput;
    }

    @Override
    public void intersectPayload(int payloadBitL, boolean isBinaryShare) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[][] payloadTargetArray = bopprfReceiver.opprf(payloadBitL, inputArray, hashNum * serverElementSize);
        Payload payload = new Payload(envType, parallel, payloadTargetArray, payloadBitL, isBinaryShare);
        if (clientOutput != null) {
            clientOutput.addPayload(payload);
        }
        logStepInfo(PtoState.PTO_STEP, 1, 1, resetAndGetTime());
        logPhaseInfo(PtoState.PTO_END);
    }

    @Override
    public PlpsiClientOutput<T> psiWithPayload(List<T> clientElementList, int serverElementSize,
                                               int[] payloadBitLs, boolean[] isBinaryShare) throws MpcAbortException {
        setPtoInput(clientElementList, serverElementSize);
        if (payloadBitLs != null && payloadBitLs.length > 0) {
            setPayload(payloadBitLs, isBinaryShare);
            return psiCommonPart(true);
        }else{
            return psiCommonPart(false);
        }
    }

    private PlpsiClientOutput<T> psiCommonPart(boolean withPayload) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // β = (1 + ε) * n_c
        int beta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        // point_num = hash_num * n_s
        int pointNum = hashNum * serverElementSize;
        // l_peqt = σ + log_2(β)
        int peqtL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(beta);
        int peqtByteL = CommonUtils.getByteLength(peqtL);
        // l_opprf = σ + log_2(point_num)
        int opprfL = Math.max(CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(pointNum), peqtL);
        int opprfByteL = CommonUtils.getByteLength(opprfL);
        // P2 inserts items into no-stash cuckoo hash bin Table_1 with β bins.
        List<byte[]> cuckooHashKeyPayload = generateCuckooHashKeyPayload();
        // P2 sends the cuckoo hash bin keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        logStepInfo(PtoState.PTO_STEP, 1, 3, resetAndGetTime(), "Server inserts cuckoo hash");

        stopWatch.start();
        // The parties invoke a batched OPPRF.
        // P2 inputs Table_1[1], . . . , Table_1[β] and receives y_1^*, ..., y_β^*
        inputArray = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                HashBinEntry<T> item = cuckooHashBin.getHashBinEntry(batchIndex);
                byte[] itemBytes = cuckooHashBin.getHashBinEntry(batchIndex).getItemByteArray();
                return ByteBuffer.allocate(itemBytes.length + Integer.BYTES)
                    .put(itemBytes)
                    .putInt(item.getHashIndex())
                    .array();
            })
            .toArray(byte[][]::new);
        Payload[] payloadRes = null;
        byte[][] targetArray;
        if (withPayload) {
            int payloadTotalByteL = Arrays.stream(payloadByteLs).sum();
            byte[][] opprfRes = bopprfReceiver.opprf(payloadTotalByteL, inputArray, pointNum);
            byte[][][] maskPayload = new byte[payloadBitLs.length][beta][];
            int[] copyIndex = new int[payloadBitLs.length];
            copyIndex[0] = opprfL;
            for (int i = 1; i < payloadByteLs.length; i++) {
                copyIndex[i] = copyIndex[i - 1] + payloadByteLs[i - 1];
            }
            targetArray = IntStream.range(0, beta).mapToObj(i -> {
                for (int j = 0; j < payloadByteLs.length; j++) {
                    maskPayload[j][i] = Arrays.copyOfRange(opprfRes[i], copyIndex[j], copyIndex[j] + payloadByteLs[j]);
                }
                return Arrays.copyOf(opprfRes[i], opprfL);
            }).toArray(byte[][]::new);
            payloadRes = IntStream.range(0, payloadBitLs.length).mapToObj(i ->
                new Payload(envType, parallel, maskPayload[i], payloadBitLs[i], isBinaryShare[i])).toArray(Payload[]::new);
        } else {
            targetArray = bopprfReceiver.opprf(opprfL, inputArray, pointNum);
        }
        logStepInfo(PtoState.PTO_STEP, 2, 3, resetAndGetTime());

        stopWatch.start();
        // The parties invoke a private equality test
        targetArray = Arrays.stream(targetArray)
            .map(target -> {
                byte[] truncatedTarget = new byte[peqtByteL];
                System.arraycopy(target, opprfByteL - peqtByteL, truncatedTarget, 0, peqtByteL);
                BytesUtils.reduceByteArray(truncatedTarget, peqtL);
                return truncatedTarget;
            })
            .toArray(byte[][]::new);
        // P2 inputs y_1^*, ..., y_β^* and outputs z1.
        SquareZ2Vector z1 = peqtReceiver.peqt(peqtL, targetArray);
        // create the table
        ArrayList<T> table = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                HashBinEntry<T> item = cuckooHashBin.getHashBinEntry(batchIndex);
                if (item.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                    return null;
                } else {
                    return item.getItem();
                }
            })
            .collect(Collectors.toCollection(ArrayList::new));
        clientOutput = new PlpsiClientOutput<>(table, z1);
        if (withPayload) {
            for (Payload payload : payloadRes) {
                clientOutput.addPayload(payload);
            }
        }
        logStepInfo(PtoState.PTO_STEP, 3, 3, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END);
        return clientOutput;
    }

    private List<byte[]> generateCuckooHashKeyPayload() {
        cuckooHashBin = CuckooHashBinFactory.createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, clientElementSize, clientElementArrayList, secureRandom
        );
        // pad random elements into the cuckoo hash
        cuckooHashBin.insertPaddingItems(secureRandom);
        return Arrays.stream(cuckooHashBin.getHashKeys()).collect(Collectors.toList());
    }


}
