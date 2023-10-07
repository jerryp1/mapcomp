package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import org.bouncycastle.crypto.Commitment;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * RR17 Encode-Commit malicious PSI client.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/05
 */
public class Rr17EcPsiClient <T> extends AbstractPsiClient<T> {
    /**
     * OPRF接收方
     */
    private final LcotReceiver lcotReceiver;
    private LcotReceiverOutput lcotReceiverOutput;
    /**
     * CoinToss Receiver
     */
    private final CoinTossParty coinTossReceiver;
    /**
     * Rr17计算方均需要初始化和计算的参数与函数
     */
    private Rr17EcPsiComTools tools;
    /**
     * 哈希桶数量
     */
    private int binNum;
    /**
     * 哈希桶maxsize
     */
    private int binSize;
    /**
     * phase哈希max item size
     */
    private int maxItemSize;
    /**
     * Element HashMap
     */
    Map<BigInteger, T> elementMap;
    /**
     * Tuple HashMap
     */
    Map<BigInteger, byte[][]> tuplesMap;
    /**
     * 决定PhaseHash number的系数，真实结果有max element size / divParam4PhaseHash 决定
     */
    private final int divParam4PhaseHash;

    public Rr17EcPsiClient(Rpc clientRpc, Party serverParty, Rr17EcPsiConfig config) {
        super(Rr17EcPsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        lcotReceiver = LcotFactory.createReceiver(clientRpc, serverParty, config.getLcotConfig());
        coinTossReceiver = CoinTossFactory.createReceiver(clientRpc, serverParty, config.getCoinTossConfig());
        divParam4PhaseHash = config.getDivParam4PhaseHash();
        addSubPtos(lcotReceiver);
        addSubPtos(coinTossReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coinTossReceiver.init();
        byte[][] hashKeys = coinTossReceiver.coinToss(3, CommonConstants.BLOCK_BIT_LENGTH);
        this.maxItemSize = Math.max(maxClientElementSize, maxServerElementSize);
        this.binNum = Math.max(maxItemSize / divParam4PhaseHash, 1);
        this.binSize = MaxBinSizeUtils.expectMaxBinSize(maxItemSize, binNum);

        tools = new Rr17EcPsiComTools(envType, maxServerElementSize, maxClientElementSize, hashKeys, binNum, binSize, clientElementSize, parallel);
        lcotReceiver.init(tools.encodeInputByteLength * Byte.SIZE, binNum * binSize);

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime, "Key exchange and OT init");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverSetSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();

        elementMap = new HashMap<>();
        // todo 因为不知道怎么由bigInteger转化为原本的泛型，所以只能再建一个hashMap
        tools.phaseHashBin.insertItems(clientElementArrayList.stream().map(arr -> {
            BigInteger intArr = BigIntegerUtils.byteArrayToNonNegBigInteger(tools.h1.digestToBytes(ObjectUtils.objectToByteArray(arr)));
            elementMap.put(intArr, arr);
            return intArr;
        }).collect(Collectors.toList()));
        tools.phaseHashBin.insertPaddingItems(BigInteger.ZERO);

        // 桶中的元素，后面的是贮存区中的元素
        byte[][] clientByteArrays = IntStream.range(0, binNum).mapToObj(binIndex ->
                IntStream.range(0, binSize).mapToObj(entryIndex -> {
                    HashBinEntry<BigInteger> hashBinEntry = tools.phaseHashBin.getBin(binIndex).get(entryIndex);
                    return BigIntegerUtils.nonNegBigIntegerToByteArray(hashBinEntry.getItem(), tools.encodeInputByteLength);
                }).collect(Collectors.toList()))
            .flatMap(Collection::stream).toArray(byte[][]::new);
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, hashTime, "Client Hash Insertion");

        stopWatch.start();
        this.lcotReceiverOutput = lcotReceiver.receive(clientByteArrays);
        tuplesMap = generateTupleHashMap();
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lcotTime, "Client LOT");

        stopWatch.start();
        // 接收服务端Tuples
        DataPacketHeader serverTuplesHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr17EcPsiPtoDesc.PtoStep.SERVER_SEND_TUPLES.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverTuplesPayload = rpc.receive(serverTuplesHeader).getPayload();
        extraInfo++;
        // 求交集
        Set<T> intersection = handleServerTuples(serverTuplesPayload);
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime, "Client handles Tuples");

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }

    private Map<BigInteger, byte[][]> generateTupleHashMap(){
        int encodeInputLength = lcotReceiverOutput.getOutputByteLength() + tools.encodeInputByteLength;
        Map<BigInteger, byte[][]> map = parallel ? new ConcurrentHashMap<>() : new HashMap<>();
        IntStream intStream = parallel ? IntStream.range(0, binNum).parallel() : IntStream.range(0, binNum);
        intStream.forEach(binIndex ->
            IntStream.range(0, binSize).forEach(entryIndex -> {
                HashBinEntry<BigInteger> hashBinEntry = tools.phaseHashBin.getBin(binIndex).get(entryIndex);
                if (hashBinEntry.getHashIndex() == 0) {
                    byte[] elementByteArray = BigIntegerUtils.nonNegBigIntegerToByteArray(tools.phaseHashBin.dephaseItem(binIndex, hashBinEntry.getItem()), tools.h1.getOutputByteLength());
                    byte[] phasedElementByteArray = BigIntegerUtils.nonNegBigIntegerToByteArray(hashBinEntry.getItem(), tools.encodeInputByteLength);
                    byte[] encode = tools.peqtHash.digestToBytes(ByteBuffer.allocate(encodeInputLength)
                        .put(phasedElementByteArray).put(lcotReceiverOutput.getRb(binIndex * binSize + entryIndex)).array());
                    BigInteger tag = BigIntegerUtils.byteArrayToBigInteger(tools.prfTag.getBytes(encode));
                    byte[] enc = tools.prfEnc.getBytes(encode);
                    byte[][] value = {enc, elementByteArray};
                    map.put(tag, value);
                }
        }));
        return map;
    }

    private Set<T> handleServerTuples(List<byte[]> serverTuplesPayload) {
        int commitLength = tools.commits[0].commit(new byte[0]).getCommitment().length;
        int eachMesLength = tools.tagPrfByteLength + CommonConstants.BLOCK_BYTE_LENGTH;
        Set<T> intersection;
        if(parallel){
            int parallelNum = ForkJoinPool.getCommonPoolParallelism();
            int eachThreadNum = Math.max(serverTuplesPayload.size() / parallelNum, 1);
            intersection = IntStream.range(0, Math.min(parallelNum, serverTuplesPayload.size())).parallel().mapToObj(tIndex -> {
                int startIndex = eachThreadNum * tIndex, endIndex = (tIndex == parallelNum - 1) ? serverTuplesPayload.size() : eachThreadNum * (tIndex + 1);
                return IntStream.range(startIndex, endIndex).mapToObj(index -> judgeOneElement(serverTuplesPayload.get(index), commitLength, eachMesLength, tIndex))
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            }).flatMap(Collection::stream).collect(Collectors.toSet());
        }else{
            Stream<byte[]> tupleStream = serverTuplesPayload.stream();
            intersection = tupleStream.map(tuple -> judgeOneElement(tuple, commitLength, eachMesLength, 0))
                .filter(Objects::nonNull).collect(Collectors.toSet());
        }
        tools.phaseHashBin = null;
        return intersection;
    }

    public T judgeOneElement(byte[] tuple, int commitLength, int eachMesLength, int tIndex){
        int secretLength = tuple.length - binSize * eachMesLength - commitLength;
        byte[] serverSecret = Arrays.copyOf(tuple, secretLength);
        byte[] serverCommitment = Arrays.copyOfRange(tuple, secretLength, secretLength + commitLength);
        for (int i = 0; i < binSize; i++) {
            int copyStartIndex = secretLength + commitLength + i * eachMesLength;
            byte[] tagArray = Arrays.copyOfRange(tuple, copyStartIndex, copyStartIndex + tools.tagPrfByteLength);
            BigInteger tag = BigIntegerUtils.byteArrayToBigInteger(tagArray);
            if(tuplesMap.containsKey(tag)) {
                byte[][] clientTuple = tuplesMap.get(tag);
                byte[] ry = Arrays.copyOfRange(tuple, copyStartIndex + tools.tagPrfByteLength, copyStartIndex + eachMesLength);
                BytesUtils.xori(ry, clientTuple[0]);
                byte[] clientMessage = ByteBuffer.allocate(tools.h1.getOutputByteLength() + ry.length).put(clientTuple[1]).put(ry).array();
                if(tools.commits[tIndex].isRevealed(clientMessage, new Commitment(serverSecret, serverCommitment))){
                    return elementMap.get(BigIntegerUtils.byteArrayToNonNegBigInteger(clientTuple[1]));
                }
            }
        }
        return null;
    }
}

