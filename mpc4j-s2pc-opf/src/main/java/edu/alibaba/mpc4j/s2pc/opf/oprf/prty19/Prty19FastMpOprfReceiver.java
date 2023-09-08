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
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.TwoChoiceHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.AbstractMpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Prty19FastMpOprfReceiver extends AbstractMpOprfReceiver {
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
     * extended input
     */
    List<byte[]> extendedInputs;
    /**
     * l-ByteLength
     */
    private int lByteLength;
    /**
     * F: {0,1}^λ × {0,1}^* → [0,1]
     */
    private List<List<Prf>> fList;
    /**
     * ROT发送方输出
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * 2choice hashbin
     */
    private TwoChoiceHashBin<byte[]> twoChoiceHashBin;
    /**
     * 2choicehash binNum
     */
    private int binNum;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * R-OKVS
     */
    private List<Gf2eDokvs<ByteBuffer>> okvsList;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;
    /**
     *  Input-Prf Map
     */
    private Map<byte[], byte[]> inputPrfMap;

    public Prty19FastMpOprfReceiver(Rpc receiverRpc, Party senderParty, Prty19FastMpOprfConfig config) {
        super(Prty19FastMpOprfPtoDesc.getInstance(), receiverRpc, senderParty, config);
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
        byte[][] hashBinKeys = CommonUtils.generateRandomKeys(2, secureRandom);
        this.twoChoiceHashBin = new TwoChoiceHashBin<>(envType, maxBatchSize, hashBinKeys[0], hashBinKeys[1]);
        this.binNum = twoChoiceHashBin.binNum();
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, maxL);
        this.fList = new ArrayList<>();
        int hashNum = Gf2eDokvsFactory.getHashKeyNum(okvsType);
        this.okvsKeys = CommonUtils.generateRandomKeys(hashNum * binNum, secureRandom);
//        IntStream binStream = IntStream.range(0, binNum);
//        binStream = parallel ? binStream.parallel() : binStream;
//        okvsList = binStream.mapToObj(index ->
//                        OkvsFactory.createInstance(envType, okvsType, twoChoiceHashBin.maxBinSize(),
//                            CommonUtils.getByteLength(maxL) * Byte.SIZE, Arrays.copyOfRange(okvsKeys, index * hashNum, (index + 1) * hashNum)))
//                .collect(Collectors.toList());
        Gf2eDokvs<ByteBuffer>[] tmpOkvsArray = new Gf2eDokvs[binNum];
        IntStream binStream = IntStream.range(0, binNum);
        binStream = parallel ? binStream.parallel() : binStream;
        binStream.forEach(index -> tmpOkvsArray[index] = Gf2eDokvsFactory.createInstance(envType, okvsType, twoChoiceHashBin.maxBinSize(),
            CommonUtils.getByteLength(maxL) * Byte.SIZE, Arrays.copyOfRange(okvsKeys, index * hashNum, (index + 1) * hashNum)));
        okvsList = Arrays.stream(tmpOkvsArray).collect(Collectors.toList());

        List<byte[]> keyPayload = Arrays.stream(ArrayUtils.addAll(hashBinKeys, okvsKeys)).collect(Collectors.toList());
        DataPacketHeader keyHeader = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Prty19FastMpOprfPtoDesc.PtoStep.RECEIVER_SEND_KEY.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keyHeader, keyPayload));

        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initCotTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
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
                this.encodeTaskId, ptoDesc.getPtoId(), Prty19FastMpOprfPtoDesc.PtoStep.RECEIVER_SEND_STORAGE.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(storageHeader, storagePayload));

        // 生成OPRF
        MpOprfReceiverOutput receiverOutput = generateReceiverOutput();

        okvsList = null;
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
        IntStream initPrf0Stream = IntStream.range(0, l);
        initPrf0Stream = parallel ? initPrf0Stream.parallel() : initPrf0Stream;
        // 初始化伪随机函数
        this.fList.add(initPrf0Stream.mapToObj(index -> PrfFactory.createInstance(envType, 1)).collect(Collectors.toList()));
        IntStream initPrf1Stream = IntStream.range(0, l);
        initPrf1Stream = parallel ? initPrf1Stream.parallel() : initPrf1Stream;
        this.fList.add(initPrf1Stream.mapToObj(index -> PrfFactory.createInstance(envType, 1)).collect(Collectors.toList()));
        IntStream keyPrfStream = IntStream.range(0, l);
        keyPrfStream = parallel ? keyPrfStream.parallel() : keyPrfStream;
        keyPrfStream.forEach(index -> {
            fList.get(0).get(index).setKey(cotSenderOutput.getR0(index));
            fList.get(1).get(index).setKey(cotSenderOutput.getR1(index));
        });
        // 初始化2choiceHashBin
        extendedInputs = Arrays.stream(inputs).map(inputs -> h1.digestToBytes(inputs)).collect(Collectors.toList());
        this.twoChoiceHashBin.insertItems(extendedInputs);
        this.twoChoiceHashBin.insertPaddingItems(secureRandom);
        // collect OPRF output in advance
        this.inputPrfMap = new ConcurrentHashMap<>();
        // compute OPRF values and corresponding OKVS
        IntStream binStream = IntStream.range(0, binNum);
        binStream = parallel ? binStream.parallel() : binStream;
        List<List<byte[]>> storageList = binStream.mapToObj(binIndex -> {
            Map<ByteBuffer, byte[]> keyValueMap = new ConcurrentHashMap<>();
            Stream<HashBinEntry<byte[]>> entryStream = twoChoiceHashBin.getBin(binIndex).stream();
            entryStream = parallel ? entryStream.parallel() : entryStream;
            entryStream.forEach(entry -> {
                byte[] extendInput = h1.digestToBytes(ByteBuffer.allocate(entry.getItemByteArray().length + 1)
                    .put(entry.getItemByteArray())
                    .put(Integer.valueOf(entry.getHashIndex()).byteValue())
                    .array());
                // 计算哈希值
                boolean[] ty = new boolean[lByteLength * Byte.SIZE];
                boolean[] ry = new boolean[lByteLength * Byte.SIZE];
                IntStream bitStream = IntStream.range(0,l);
                bitStream = parallel ? bitStream.parallel() : bitStream;
                bitStream.forEach(bitIndex -> {
                    ty[bitIndex] = fList.get(0).get(bitIndex).getBoolean(extendInput);
                    ry[bitIndex] = ty[bitIndex] ^ fList.get(1).get(bitIndex).getBoolean(extendInput);
                });
                keyValueMap.put(ByteBuffer.wrap(extendInput), BinaryUtils.binaryToByteArray(ry));
                if(entry.getItem() != null)
                    inputPrfMap.put(entry.getItem(), BinaryUtils.binaryToByteArray(ty));
            });
            return Arrays.asList(okvsList.get(binIndex).encode(keyValueMap, false));
        }).collect(Collectors.toList());
        List<byte[]> storage = new ArrayList<>();
        for (List<byte[]> bytes : storageList) {
            storage.addAll(bytes);
        }
        return storage;
    }


    private MpOprfReceiverOutput generateReceiverOutput(){
        Stream<byte[]> inputStream = extendedInputs.stream();
        inputStream = parallel ? inputStream.parallel() : inputStream;
        byte[][] inputPrf = inputStream
                .map(input -> inputPrfMap.get(input)).toArray(byte[][]::new);
        return new MpOprfReceiverOutput(lByteLength, inputs, inputPrf);
    }
}
