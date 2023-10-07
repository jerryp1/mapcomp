package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
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
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RR17 Dual Execution malicious PSI server.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/05
 */
public class Rr17DePsiServer <T> extends AbstractPsiServer<T> {
    /**
     * OPRF发送方
     */
    private final LcotSender lcotSender;
    /**
     * InverseOPRF接收方
     */
    private final LcotReceiver lcotInvReceiver;
    /**
     * CoinToss发送方
     */
    private final CoinTossParty coinTossSender;
    /**
     * 过滤器类型
     */
    private final FilterFactory.FilterType filterType;
    /**
     * PEQT哈希函数
     */
    private Hash peqtHash;
    /**
     * Input哈希函数
     */
    private Hash h1;
    /**
     * LOT input Length
     */
    private int encodeInputByteLength;
    /**
     * 布谷鸟哈希桶所用的哈希函数
     */
    private PhaseHashBin phaseHashBin;
    /**
     * phase哈希桶个数
     */
    private int binNum;
    /**
     * 哈希桶maxsize
     */
    private int binSize;
    /**
     * OPRF发送方输出
     */
    private LcotSenderOutput lcotSenderOutput;
    /**
     *  InverseOPRF接收方输出
     */
    private LcotReceiverOutput lcotInvReceiverOutput;
    /**
     * 决定PhaseHash number的系数，真实结果有max element size / divParam4PhaseHash 决定
     */
    private final int divParam4PhaseHash;
    /**
     * hash表中的数据以及是否为真实值的flag
     */
    private byte[][] serverByteArrays;
    private boolean[] ind4ValidElement;

    public Rr17DePsiServer(Rpc serverRpc, Party clientParty, Rr17DePsiConfig config) {
        super(Rr17DePsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        lcotSender = LcotFactory.createSender(serverRpc, clientParty, config.getLcotConfig());
        lcotInvReceiver= LcotFactory.createReceiver(serverRpc, clientParty, config.getLcotConfig());
        coinTossSender = CoinTossFactory.createSender(serverRpc, clientParty, config.getCoinTossConfig());
        divParam4PhaseHash = config.getDivParam4PhaseHash();
        addSubPtos(lcotSender);
        addSubPtos(lcotInvReceiver);
        addSubPtos(coinTossSender);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 初始化hash bin的number和size
        coinTossSender.init();
        byte[][] hashKeys = coinTossSender.coinToss(1, CommonConstants.BLOCK_BIT_LENGTH);
        int maxItemSize = Math.max(maxClientElementSize, maxServerElementSize);
        binNum = Math.max(maxItemSize / divParam4PhaseHash, 1);
        binSize = MaxBinSizeUtils.expectMaxBinSize(maxItemSize, binNum);
        phaseHashBin = new PhaseHashBin(envType, binNum, maxItemSize, hashKeys[0]);

        // 初始化hash bin中元素的byte长度
        int l = PsiUtils.getMaliciousPeqtByteLength(maxServerElementSize, maxClientElementSize);
        h1 = HashFactory.createInstance(envType, l);
        encodeInputByteLength = CommonUtils.getByteLength(l * Byte.SIZE - (int) Math.round(Math.floor(DoubleUtils.log2(binNum))));
        //初始化最终peqt时发送的byte长度和hash函数
        int peqtByteLength = CommonConstants.STATS_BYTE_LENGTH +
            CommonUtils.getByteLength(2 * LongUtils.ceilLog2(Math.max(2, binSize * clientElementSize)));
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // 初始化OT
        lcotSender.init(encodeInputByteLength * Byte.SIZE, binNum * binSize);
        lcotInvReceiver.init(encodeInputByteLength * Byte.SIZE, binNum * binSize);

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime, "Key exchange and OT init");
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // 将消息插入到Hash中
        phaseHashBin.insertItems(serverElementArrayList.stream().map(arr ->
            BigIntegerUtils.byteArrayToNonNegBigInteger(h1.digestToBytes(ObjectUtils.objectToByteArray(arr))))
            .collect(Collectors.toList()));
        phaseHashBin.insertPaddingItems(BigInteger.ZERO);
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, hashTime, "Server Hash Insertion");

        stopWatch.start();
        lcotSenderOutput = lcotSender.send(binSize * binNum);
        serverByteArrays = generateElementByteArrays();
        lcotInvReceiverOutput= lcotInvReceiver.receive(serverByteArrays);
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lcotTime, "Server LOT");

        stopWatch.start();
        // 发送服务端哈希桶PRF过滤器
        List<byte[]> serverPrfPayload = generatePrfPayload();
        DataPacketHeader serverPrfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr17DePsiPtoDesc.PtoStep.SERVER_SEND_PRFS.ordinal(), extraInfo++,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrfHeader, serverPrfPayload));
        lcotSenderOutput = null;
        lcotInvReceiverOutput=null;
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime, "Server computes PRFs");
        logPhaseInfo(PtoState.PTO_END);
    }

    private byte[][] generateElementByteArrays() {
        // 桶中的元素，后面的是贮存区中的元素
        ind4ValidElement = new boolean[binNum * binSize];
        return IntStream.range(0, ind4ValidElement.length).mapToObj(elementIndex -> {
            HashBinEntry<BigInteger> hashBinEntry = phaseHashBin.getBin(elementIndex / binSize).get(elementIndex % binSize);
            ind4ValidElement[elementIndex] = hashBinEntry.getHashIndex() == 0;
            return BigIntegerUtils.nonNegBigIntegerToByteArray(hashBinEntry.getItem(), encodeInputByteLength);
        }).toArray(byte[][]::new);
    }

    private List<byte[]> generatePrfPayload() {
        int peqtHashInputLength = lcotSenderOutput.getOutputByteLength() + encodeInputByteLength;
        IntStream serverElementStream = IntStream.range(0, binNum * binSize);
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> prfList = Collections.synchronizedList(new LinkedList<>());
        serverElementStream.forEach(index -> {
            int binIndex = index / binSize;
            if (ind4ValidElement[index]) {
                byte[] halfElementPrf = lcotInvReceiverOutput.getRb(index);
                byte[] elementByteArray = serverByteArrays[index];
                for(int i = 0; i < binSize; i++){
                    byte[] elementPrf = BytesUtils.xor(halfElementPrf, lcotSenderOutput.getRb(
                        binIndex * binSize + i, BytesUtils.paddingByteArray(elementByteArray, encodeInputByteLength)));
                    prfList.add(peqtHash.digestToBytes(ByteBuffer.allocate(peqtHashInputLength)
                        .put(elementByteArray).put(elementPrf).array()));
                }
            }
        });
        Collections.shuffle(prfList, secureRandom);
        // 构建过滤器
        Filter<byte[]> prfFilter = FilterFactory.createFilter(envType, filterType, serverElementSize * binSize, secureRandom);
        prfList.forEach(prfFilter::put);
        return prfFilter.toByteArrayList();
    }

}
