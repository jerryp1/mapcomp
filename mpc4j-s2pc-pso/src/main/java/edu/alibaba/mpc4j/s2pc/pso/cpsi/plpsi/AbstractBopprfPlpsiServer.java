package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
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
public class AbstractBopprfPlpsiServer<T> extends AbstractPlpsiServer<T> {
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
     * target payload
     */
    private Payload payload;
    /**
     * input arrays
     */
    private byte[][][] inputArrays;
    /**
     * target arrays
     */
    private byte[][][] targetArrays;
    /**
     * payload
     */
    private byte[][][] payloadTargetArrays;

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
    public void init(int maxServerElementSize, int maxClientElementSize, int payloadBitL) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize, payloadBitL);
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
    public PlpsiServerOutput psi(List<T> serverElementList, List<T> serverPayloadList, int clientElementSize) throws MpcAbortException {
        assert (serverPayloadBitL == 0 && serverPayloadList == null) || (serverPayloadBitL > 0 && serverPayloadList != null);
        setPtoInput(serverElementList, serverPayloadList, clientElementSize);
        int ptoStepNum = serverPayloadBitL == 0 ? 3 : 4;
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
        logStepInfo(PtoState.PTO_STEP, 1, ptoStepNum, binTime, "Client inserts simple hash");

        stopWatch.start();
        // The parties invoke a batched OPPRF.
        // P1 inputs Table_2[1], . . . , Table_2[β] and receives T[1], ..., T[β]
        generateBopprfInputs(opprfL);
        bopprfSender.opprf(opprfL, inputArrays, targetArrays);
        targetArrays = null;
        stopWatch.stop();
        long opprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, ptoStepNum, opprfTime);

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
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, ptoStepNum, peqtTime);

        if (serverPayloadBitL > 0) {
            stopWatch.start();
            // The parties invoke a batched OPPRF.
            // P1 inputs Table_2[1], . . . , Table_2[β] and receives T[1], ..., T[β]
            generateBopprfPayloadInputs();
            bopprfSender.opprf(serverPayloadBitL, inputArrays, payloadTargetArrays);
            inputArrays = null;
            payloadTargetArrays = null;
            stopWatch.stop();
            long secondOpprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 4, ptoStepNum, secondOpprfTime, "second opprf for payload");
        }

        logPhaseInfo(PtoState.PTO_END);
        return new PlpsiServerOutput(z0, payload);
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

    private void generateBopprfPayloadInputs() {
        int payloadByteL = CommonUtils.getByteLength(serverPayloadBitL);
        byte[][] payloadTargetArray = IntStream.range(0, beta)
            .mapToObj(batchIndex -> BytesUtils.randomByteArray(payloadByteL, serverPayloadBitL, secureRandom)).toArray(byte[][]::new);
        payload = new Payload(envType, parallel, payloadTargetArray, serverPayloadBitL, isBinaryShare);
        payloadTargetArrays = IntStream.range(0, beta)
            .mapToObj(batchIndex -> {
                ArrayList<HashBinEntry<T>> bin = new ArrayList<>(simpleHashBin.getBin(batchIndex));
                return bin.stream()
                    .map(entry -> {
                        T item = entry.getItem();
                        if (hashMap.containsKey(item)) {
                            byte[] value = hashMap.get(item);
                            if (isBinaryShare) {
                                return BytesUtils.xor(value, payloadTargetArray[batchIndex]);
                            } else {
                                BigInteger res = payload.getZlPayload().getZl().sub(
                                    BigIntegerUtils.byteArrayToNonNegBigInteger(value),
                                    payload.getZlPayload().getZlVector().getElement(batchIndex));
                                return BigIntegerUtils.nonNegBigIntegerToByteArray(res, payloadByteL);
                            }
                        } else {
                            return BytesUtils.randomByteArray(payloadByteL, serverPayloadBitL, secureRandom);
                        }
                    })
                    .toArray(byte[][]::new);
            })
            .toArray(byte[][][]::new);
    }
}
