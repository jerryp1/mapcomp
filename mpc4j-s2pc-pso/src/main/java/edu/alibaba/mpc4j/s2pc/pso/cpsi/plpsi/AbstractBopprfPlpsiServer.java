package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfSender;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.BopprfPlpsiPtoDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * abstract batched OPPRF-based payload-circuit PSI server.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class AbstractBopprfPlpsiServer<T, X> extends AbstractPlpsiServer<T, X> {
    /**
     * batched OPPRF sender
     */
    private final BopprfSender bopprfSender;
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
    private final int hashNum;
    /**
     * β
     */
    private int beta;
    /**
     * simple hash bin
     */
    private RandomPadHashBin<T> simpleHashBin;
    /**
     * target array
     */
    private byte[][] targetArray;
    /**
     * input arrays
     */
    private byte[][][] inputArrays;
    /**
     * target arrays
     */
    private byte[][][] targetArrays;

    private PlpsiShareOutput plpsiShareOutput;

    protected AbstractBopprfPlpsiServer(PtoDesc ptoDesc, Rpc clientRpc, Party senderParty, BopprfPlpsiConfig config) {
        super(ptoDesc, clientRpc, senderParty, config);
        bopprfSender = BopprfFactory.createSender(clientRpc, senderParty, config.getBopprfConfig());
        addSubPtos(bopprfSender);
        peqtSender = PeqtFactory.createReceiver(clientRpc, senderParty, config.getPeqtConfig());
        addSubPtos(peqtSender);
        cuckooHashBinType = config.getCuckooHashBinType();
        hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init batched OPPRF, where β_max = (1 + ε) * n_c, max_point_num = hash_num * n_s
        int maxBeta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize);
        int maxPointNum = hashNum * maxServerElementSize;
        bopprfSender.init(maxBeta, maxPointNum);
        // init private equality test sender, where max(l_peqt) = σ + log_2(β_max)
        int maxPeqtL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(maxBeta);
        peqtSender.init(maxPeqtL, maxBeta);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PlpsiShareOutput psi(List<T> serverElementList, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementList, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // P1 receives the cuckoo hash bin keys
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        // β = (1 + ε) * n_s
        beta = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        // point_num = hash_num * n_s
        int pointNum = hashNum * serverElementSize;
        // l_peqt = σ + log_2(β)
        int peqtL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(beta);
        int peqtByteL = CommonUtils.getByteLength(peqtL);
        // l_opprf = σ + log_2(point_num)
        int opprfL = Math.max(CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(pointNum), peqtL);
        int opprfByteL = CommonUtils.getByteLength(opprfL);
        // P1 inserts items into simple hash bin Table_2 with β bins
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long binTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, binTime, "Client inserts simple hash");

        stopWatch.start();
        // The parties invoke a batched OPPRF.
        // P1 inputs Table_2[1], . . . , Table_2[β] and receives T[1], ..., T[β]
        generateBopprfInputs(opprfL);
        bopprfSender.opprf(opprfL, inputArrays, targetArrays);
        targetArrays = null;
        stopWatch.stop();
        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, opprfTime);

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
        // P1 inputs y_1^*, ..., y_β^* and outputs z0.
        SquareZ2Vector z0 = peqtSender.peqt(peqtL, targetArray);
        targetArray = null;
        plpsiShareOutput = new PlpsiShareOutput(z0);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, peqtTime);
        logPhaseInfo(PtoState.PTO_END);
        return plpsiShareOutput;
    }

    @Override
    public Payload intersectPayload(List<X> serverPayloadList, int payloadBitLs, boolean isBinaryShare) throws MpcAbortException {
        assert plpsiShareOutput != null;
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int payloadByteL = CommonUtils.getByteLength(payloadBitLs);
        byte[][] serverPayloadArray = serverPayloadList.stream().map(x ->
            BytesUtils.fixedByteArrayLength(ObjectUtils.objectToByteArray(x), payloadByteL)).toArray(byte[][]::new);
        byte[][] payloadTargetArray = IntStream.range(0, beta)
            .mapToObj(batchIndex -> BytesUtils.randomByteArray(payloadByteL, payloadBitLs, secureRandom)).toArray(byte[][]::new);
        Payload payload = new Payload(envType, parallel, payloadTargetArray, payloadBitLs, isBinaryShare);
        byte[][][] payloadTargetArrays = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                ArrayList<HashBinEntry<T>> bin = new ArrayList<>(simpleHashBin.getBin(batchIndex));
                return bin.stream()
                    .map(entry -> {
                        T item = entry.getItem();
                        if (hashMap.containsKey(item)) {
                            int index = hashMap.get(item);
                            byte[] value = serverPayloadArray[index];
                            if (isBinaryShare) {
                                return BytesUtils.xor(value, payloadTargetArray[batchIndex]);
                            } else {
                                BigInteger res = payload.getZlPayload().getZl().sub(
                                    BigIntegerUtils.byteArrayToNonNegBigInteger(value),
                                    payload.getZlPayload().getZlVector().getElement(batchIndex));
                                return BigIntegerUtils.nonNegBigIntegerToByteArray(res, payloadByteL);
                            }
                        } else {
                            return BytesUtils.randomByteArray(payloadByteL, payloadBitLs, secureRandom);
                        }
                    })
                    .toArray(byte[][]::new);
            })
            .toArray(byte[][][]::new);

        bopprfSender.opprf(payloadBitLs, inputArrays, payloadTargetArrays);
        plpsiShareOutput.addPayload(payload);
        stopWatch.stop();
        long payloadOpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, payloadOpprfTime, "opprf for payload");

        logPhaseInfo(PtoState.PTO_END);
        return payload;
    }

    private void handleCuckooHashKeyPayload(List<byte[]> cuckooHashKeyPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(cuckooHashKeyPayload.size() == hashNum);
        byte[][] cuckooHashKeys = cuckooHashKeyPayload.toArray(new byte[0][]);
        simpleHashBin = new RandomPadHashBin<>(envType, beta, serverElementSize, cuckooHashKeys);
        simpleHashBin.insertItems(serverElementArrayList);
    }

    private void generateBopprfInputs(int l) {
        int byteL = CommonUtils.getByteLength(l);
        // P2 generates the input arrays
        inputArrays = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                ArrayList<HashBinEntry<T>> bin = new ArrayList<>(simpleHashBin.getBin(batchIndex));
                return bin.stream()
                    .map(entry -> {
                        byte[] itemBytes = entry.getItemByteArray();
                        return ByteBuffer.allocate(itemBytes.length + Integer.BYTES)
                            .put(itemBytes)
                            .putInt(entry.getHashIndex())
                            .array();
                    })
                    .toArray(byte[][]::new);
            })
            .toArray(byte[][][]::new);
        // P1 samples uniformly random and independent target values t_1, ..., t_β ∈ {0,1}^κ
        targetArray = IntStream.range(0, beta)
            .mapToObj(batchIndex -> BytesUtils.randomByteArray(byteL, l, secureRandom)).toArray(byte[][]::new);
        targetArrays = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                int batchPointNum = inputArrays[batchIndex].length;
                byte[][] copyTargetArray = new byte[batchPointNum][byteL];
                for (int i = 0; i < batchPointNum; i++) {
                    copyTargetArray[i] = BytesUtils.clone(targetArray[batchIndex]);
                }
                return copyTargetArray;
            })
            .toArray(byte[][][]::new);
    }


//    /**
//     * 一次性处理多个payload的版本
//     */
//    private void generateBopprfPayloadInputs() {
//        int[] payloadByteLs = Arrays.stream(serverPayloadBitLs).map(CommonUtils::getByteLength).toArray();
//        totalByteLen = Arrays.stream(payloadByteLs).sum();
//
//        byte[][][] mask = IntStream.range(0, payloadByteLs.length)
//            .mapToObj(payloadIndex -> {
//                int byteLen = payloadByteLs[payloadIndex], bitLen = serverPayloadBitLs[payloadIndex];
//                return IntStream.range(0, beta)
//                    .mapToObj(rowIndex -> BytesUtils.randomByteArray(byteLen, bitLen, secureRandom))
//                    .toArray(byte[][]::new);
//            })
//            .toArray(byte[][][]::new);
//        payloads = IntStream.range(0, serverPayloadBitLs.length).mapToObj(i ->
//            new Payload(envType, parallel, mask[i], serverPayloadBitLs[i], isBinaryShare[i])).toArray(Payload[]::new);
//        IntStream intStream = parallel ? IntStream.range(0, beta).parallel() : IntStream.range(0, beta);
//        payloadTargetArrays = intStream
//            .mapToObj(batchIndex -> {
//                ArrayList<HashBinEntry<T>> bin = new ArrayList<>(simpleHashBin.getBin(batchIndex));
//                return bin.stream()
//                    .map(entry -> {
//                        T item = entry.getItem();
//                        ByteBuffer onePayload = ByteBuffer.allocate(totalByteLen);
//                        if (hashMap.containsKey(item)) {
//                            int index = hashMap.get(item);
//                            for (int payloadIndex = 0; payloadIndex < serverPayloadBitLs.length; payloadIndex++){
//                                byte[] value = serverPayloadArrays[payloadIndex][index];
//                                if (isBinaryShare[payloadIndex]) {
//                                    onePayload.put(BytesUtils.xor(value, mask[payloadIndex][batchIndex]));
//                                } else {
//                                    Payload currentPayload = payloads[payloadIndex];
//                                    BigInteger res = currentPayload.getZlPayload().getZl().sub(
//                                        BigIntegerUtils.byteArrayToNonNegBigInteger(value),
//                                        currentPayload.getZlPayload().getZlVector().getElement(batchIndex));
//                                    onePayload.put(BigIntegerUtils.nonNegBigIntegerToByteArray(res, payloadByteLs[payloadIndex]));
//                                }
//                            }
//                        } else {
//                            for (int payloadIndex = 0; payloadIndex < serverPayloadBitLs.length; payloadIndex++){
//                                onePayload.put(BytesUtils.randomByteArray(payloadByteLs[payloadIndex], serverPayloadBitLs[payloadIndex], secureRandom));
//                            }
//                        }
//                        return onePayload.array();
//                    })
//                    .toArray(byte[][]::new);
//            })
//            .toArray(byte[][][]::new);
//    }
}
