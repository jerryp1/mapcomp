package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.filter.Filter;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.PhaseHashBin;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.*;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * RR17 Dual Execution malicious PSI client.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/05
 */
public class Rr17DePsiClient <T> extends AbstractPsiClient<T> {
    /**
     * OPRF接收方
     */
    private final LcotReceiver lcotReceiver;
    private LcotReceiverOutput lcotReceiverOutput;
    /**
     * Inverse OPRF发送方
     */
    private final LcotSender lcotInvSender;
    private LcotSenderOutput lcotInvSenderOutput;
    /**
     * CoinToss Receiver
     */
    private final CoinTossParty coinTossReceiver;
    /**
     * PEQT哈希函数
     */
    private Hash peqtHash;
    /**
     * input hash
     */
    private Hash h1;
    /**
     * LOT input Length
     */
    private int encodeInputByteLength;
    /**
     * 哈希桶数量
     */
    private int binNum;
    /**
     * 哈希桶maxsize
     */
    private int binSize;
    /**
     * 布谷鸟哈希
     */
    private PhaseHashBin phaseHashBin;
    /**
     * 服务端元素的PRF结果
     */
    Filter<byte[]> serverPrfFilter;
    /**
     * 查找原始输入的map
     */
    Map<BigInteger, T> elementMap;
    /**
     * 决定PhaseHash number的系数，真实结果有max element size / divParam4PhaseHash 决定
     */
    private final int divParam4PhaseHash;
    /**
     * hash表中的数据以及是否为真实值的flag
     */
    private byte[][] clientByteArrays;
    private boolean[] ind4ValidElement;

    public Rr17DePsiClient(Rpc clientRpc, Party serverParty, Rr17DePsiConfig config) {
        super(Rr17DePsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        lcotReceiver = LcotFactory.createReceiver(clientRpc, serverParty, config.getLcotConfig());
        lcotInvSender = LcotFactory.createSender(clientRpc, serverParty, config.getLcotConfig());
        coinTossReceiver = CoinTossFactory.createReceiver(clientRpc, serverParty, config.getCoinTossConfig());
        divParam4PhaseHash = config.getDivParam4PhaseHash();
        addSubPtos(lcotReceiver);
        addSubPtos(lcotInvSender);
        addSubPtos(coinTossReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coinTossReceiver.init();
        byte[][] hashKey = coinTossReceiver.coinToss(1, CommonConstants.BLOCK_BIT_LENGTH);
        int maxItemSize = Math.max(maxClientElementSize, maxServerElementSize);
        this.binNum = Math.max(maxItemSize / divParam4PhaseHash, 1);
        this.binSize = MaxBinSizeUtils.expectMaxBinSize(maxItemSize, binNum);
        phaseHashBin = new PhaseHashBin(envType, binNum, maxItemSize, hashKey[0]);

        int l = PsiUtils.getMaliciousPeqtByteLength(maxServerElementSize, maxClientElementSize);
        h1 = HashFactory.createInstance(envType, l);
        encodeInputByteLength = CommonUtils.getByteLength(l * Byte.SIZE - (int) Math.round(Math.floor(DoubleUtils.log2(binNum))));
        int peqtByteLength = CommonConstants.STATS_BYTE_LENGTH +
            CommonUtils.getByteLength(2 * LongUtils.ceilLog2(Math.max(2, binSize * clientElementSize)));
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        lcotReceiver.init(encodeInputByteLength * Byte.SIZE, binNum * binSize);
        lcotInvSender.init(encodeInputByteLength * Byte.SIZE, binNum * binSize);

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
        elementMap = parallel ? new ConcurrentHashMap<>() : new HashMap<>();
        // 将客户端消息插入到HashBin中
        Stream<T> elementStream = parallel ? clientElementArrayList.stream().parallel() : clientElementArrayList.stream();
        phaseHashBin.insertItems(elementStream.map(arr -> {
            BigInteger intArr = BigIntegerUtils.byteArrayToNonNegBigInteger(h1.digestToBytes(ObjectUtils.objectToByteArray(arr)));
            elementMap.put(intArr, arr);
            return intArr;
        }).collect(Collectors.toList()));
        phaseHashBin.insertPaddingItems(BigInteger.ZERO);
        clientByteArrays = generateElementByteArrays();
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, hashTime, "Client Hash Insertion");

        stopWatch.start();
        this.lcotReceiverOutput = lcotReceiver.receive(clientByteArrays);
        this.lcotInvSenderOutput = lcotInvSender.send(binSize * binNum);
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lcotTime, "Client LOT");

        stopWatch.start();
        // 接收服务端PRF
        DataPacketHeader serverPrfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr17DePsiPtoDesc.PtoStep.SERVER_SEND_PRFS.ordinal(), extraInfo++,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrfPayload = rpc.receive(serverPrfHeader).getPayload();
        // 求交集
        Set<T> intersection = handleServerPrf(serverPrfPayload);
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime, "Client computes PRFs");
        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }

    private byte[][] generateElementByteArrays() {
        ind4ValidElement = new boolean[binNum * binSize];
        IntStream intStream = parallel ? IntStream.range(0, binNum * binSize).parallel() : IntStream.range(0, binNum * binSize);
        return intStream.mapToObj(elementIndex -> {
            HashBinEntry<BigInteger> hashBinEntry = phaseHashBin.getBin(elementIndex / binSize).get(elementIndex % binSize);
            ind4ValidElement[elementIndex] = hashBinEntry.getHashIndex() == 0;
            return BigIntegerUtils.nonNegBigIntegerToByteArray(hashBinEntry.getItem(), encodeInputByteLength);
        }).toArray(byte[][]::new);
    }

    private Set<T> handleServerPrf(List<byte[]> serverPrfPayload) {
        int peqtHashInputLength = lcotReceiverOutput.getOutputByteLength() + encodeInputByteLength;
        serverPrfFilter = FilterFactory.createFilter(envType, serverPrfPayload);
        // 遍历布谷鸟哈希中的哈希桶
        IntStream intStream = parallel ? IntStream.range(0, binNum * binSize).parallel() : IntStream.range(0, binNum * binSize);
        Set<T> intersection = intStream.mapToObj(elementIndex -> {
            int binIndex = elementIndex / binSize;
            if (ind4ValidElement[elementIndex]) {
                byte[] elementByteArray = clientByteArrays[elementIndex];
                byte[] halfElementPrf = lcotReceiverOutput.getRb(elementIndex);
                for(int i = 0; i < binSize; i++){
                    byte[] elementPrf = BytesUtils.xor(halfElementPrf, lcotInvSenderOutput.getRb(binIndex * binSize + i, elementByteArray));
                    byte[] clientPrf = peqtHash.digestToBytes(ByteBuffer.allocate(peqtHashInputLength).put(elementByteArray).put(elementPrf).array());
                    if(serverPrfFilter.mightContain(clientPrf))
                        return elementMap.get(phaseHashBin.dephaseItem(binIndex, BigIntegerUtils.byteArrayToNonNegBigInteger(elementByteArray)));
                }
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
        serverPrfFilter = null;
        phaseHashBin = null;
        return intersection;
    }
}

