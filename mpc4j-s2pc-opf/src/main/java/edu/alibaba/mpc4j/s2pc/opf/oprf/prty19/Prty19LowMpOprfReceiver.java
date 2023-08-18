package edu.alibaba.mpc4j.s2pc.opf.oprf.prty19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Prty19LowMpOprfReceiver extends AbstractMpOprfReceiver {
    /**
     * 核COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * l
     */
    private int l;
    /**
     * input hash
     */
    private Hash h1;
    /**
     * l-ByteLength
     */
    private int lByteLength;
    /**
     * F: {0,1}^λ × {0,1}^* → [0,1]
     */
    private List<Prf> fList;
    /**
     * ROT发送方输出
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * T(y)
     */
    private byte[][] ty;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * R-OKVS
     */
    private Gf2eDokvs<ByteBuffer> rArrayOkvs;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;

    public Prty19LowMpOprfReceiver(Rpc receiverRpc, Party senderParty, Prty19LowMpOprfConfig config) {
        super(Prty19LowMpOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
        coreCotSender = CoreCotFactory.createSender(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPtos(coreCotSender);
        okvsType = config.getOkvsType();
    }

    @Override
    public void init(int maxBatchSize, int maxPrfNum) throws MpcAbortException {
        setInitInput(maxBatchSize, maxPrfNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 计算maxL，初始化COT协议
        int maxL = Prty19MpOprfUtils.getL(maxBatchSize);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, maxL);
        this.okvsKeys = CommonUtils.generateRandomKeys(Gf2eDokvsFactory.getHashKeyNum(okvsType), secureRandom);
        int n = (Gf2eDokvsFactory.isBinary(okvsType) || (maxBatchSize > 1)) ? maxBatchSize: 2;
        rArrayOkvs = Gf2eDokvsFactory.createInstance(envType,okvsType, n, CommonUtils.getByteLength(maxL) * Byte.SIZE, okvsKeys);

        List<byte[]> okvsKeyPayload = Arrays.stream(okvsKeys).collect(Collectors.toList());
        DataPacketHeader okvsKeyHeader = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Prty19LowMpOprfPtoDesc.PtoStep.RECEIVER_SEND_OKVS_KEY.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
        rpc.send(DataPacket.fromByteArrayList(okvsKeyHeader, okvsKeyPayload));

        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initCotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    public MpOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException {
        setPtoInput(inputs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        l = Prty19MpOprfUtils.getL(inputs.length);
        lByteLength = CommonUtils.getByteLength(l);
        h1 = HashFactory.createInstance(HashFactory.HashType.BC_SHA3_512, lByteLength);
        // 执行COT协议
        cotSenderOutput = coreCotSender.send(l);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, cotTime);

        stopWatch.start();
        // 生成关联矩阵R = T ⊕ U
        List<byte[]> storagePayload = generateStoragePayload();
        cotSenderOutput = null;
        DataPacketHeader storageHeader = new DataPacketHeader(
                this.encodeTaskId, ptoDesc.getPtoId(), Prty19LowMpOprfPtoDesc.PtoStep.RECEIVER_SEND_STORAGE.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(storageHeader, storagePayload));

        // 生成OPRF
        MpOprfReceiverOutput receiverOutput = new MpOprfReceiverOutput(lByteLength, inputs, ty);
        ty = null;
        rArrayOkvs = null;
        okvsKeys = null;
        fList = null;
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, oprfTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }

    private List<byte[]> generateStoragePayload() {
        IntStream initPrfStream = IntStream.range(0,l);
        initPrfStream = parallel ? initPrfStream.parallel() : initPrfStream;
        // 初始化伪随机函数
        this.fList = initPrfStream.mapToObj(index -> PrfFactory.createInstance(envType, 1)).collect(Collectors.toList());
        IntStream keyPrfStream = IntStream.range(0,l);
        keyPrfStream = parallel ? keyPrfStream.parallel() : keyPrfStream;
        keyPrfStream.forEach(index -> {
            fList.get(index).setKey(cotSenderOutput.getR0(index));
        });
        // For each y ∈ Y, compute Ty  = F(t1, y) || ... || F(tl, y)
        List<byte[]> extendedInputs = Arrays.stream(inputs).map(input -> h1.digestToBytes(input)).collect(Collectors.toList());
        Stream<byte[]> inputStream = extendedInputs.stream();
        inputStream = parallel ? inputStream.parallel() : inputStream;
        ty = inputStream
                .map(input -> {
                    // 计算哈希值
                    boolean[] tExtendPrf = new boolean[lByteLength * Byte.SIZE];
                    IntStream tBinaryStream = IntStream.range(0, l);
                    tBinaryStream = parallel ? tBinaryStream.parallel() : tBinaryStream;
                    tBinaryStream.forEach(index -> {
                        tExtendPrf[index] = fList.get(index).getBoolean(input);
                    });
                    return BinaryUtils.binaryToByteArray(tExtendPrf);
                }).toArray(byte[][]::new);
        // Compute uy
        IntStream rPrfStream = IntStream.range(0,l);
        rPrfStream = parallel ? rPrfStream.parallel() : rPrfStream;
        rPrfStream.forEach(index -> fList.get(index).setKey(cotSenderOutput.getR1(index)));
        Stream<byte[]> inputStream2 = extendedInputs.stream();
        inputStream2 = parallel ? inputStream2.parallel() : inputStream2;
        byte[][] uy = inputStream2
                .map(input -> {
                    // 计算哈希值
                    boolean[] uExtendPrf = new boolean[lByteLength * Byte.SIZE];
                    IntStream uBinaryStream = IntStream.range(0, l);
                    uBinaryStream = parallel ? uBinaryStream.parallel() : uBinaryStream;
                    uBinaryStream.forEach(index -> {
                        uExtendPrf[index] = fList.get(index).getBoolean(input);
                    });
                    return BinaryUtils.binaryToByteArray(uExtendPrf);
                }).toArray(byte[][]::new);
        Map<ByteBuffer, byte[]> keyValueMap = new HashMap<>();
        IntStream.range(0, inputs.length).forEach(index -> {
            byte[] valueBytes = BytesUtils.xor(uy[index], ty[index]);
            keyValueMap.put(ByteBuffer.wrap(extendedInputs.get(index)), valueBytes);
        });
        cotSenderOutput = null;
        return Arrays.asList(rArrayOkvs.encode(keyValueMap, false));
    }

}
