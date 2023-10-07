package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr17;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import org.bouncycastle.crypto.Commitment;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * RR17 Encode-Commit malicious PSI server.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/05
 */
public class Rr17EcPsiServer <T> extends AbstractPsiServer<T> {
    /**
     * OPRF发送方
     */
    private final LcotSender lcotSender;
    /**
     * CoinToss发送方
     */
    private final CoinTossParty coinTossSender;
    /**
     * Rr17计算方均需要初始化和计算的参数与函数
     */
    private Rr17EcPsiComTools tools;
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
     * 决定PhaseHash number的系数，真实结果有max element size / divParam4PhaseHash 决定
     */
    private final int divParam4PhaseHash;

    public Rr17EcPsiServer(Rpc serverRpc, Party clientParty, Rr17EcPsiConfig config) {
        super(Rr17EcPsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        lcotSender = LcotFactory.createSender(serverRpc, clientParty, config.getLcotConfig());
        coinTossSender = CoinTossFactory.createSender(serverRpc, clientParty, config.getCoinTossConfig());
        divParam4PhaseHash = config.getDivParam4PhaseHash();
        addSubPtos(lcotSender);
        addSubPtos(coinTossSender);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();

        int maxItemSize = Math.max(maxClientElementSize, maxServerElementSize);
        binNum = Math.max(maxItemSize / divParam4PhaseHash, 1);
        binSize = MaxBinSizeUtils.expectMaxBinSize(maxItemSize, binNum);

        coinTossSender.init();
        byte[][] hashKeys = coinTossSender.coinToss(3, CommonConstants.BLOCK_BIT_LENGTH);
        tools = new Rr17EcPsiComTools(envType, maxServerElementSize, maxClientElementSize, hashKeys, binNum, binSize, clientElementSize, parallel);
        lcotSender.init(tools.encodeInputByteLength * Byte.SIZE, binNum * binSize);

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
        tools.phaseHashBin.insertItems(serverElementArrayList.stream().map(arr ->
            BigIntegerUtils.byteArrayToNonNegBigInteger(tools.h1.digestToBytes(ObjectUtils.objectToByteArray(arr)))).collect(Collectors.toList()));
        tools.phaseHashBin.insertPaddingItems(BigInteger.ZERO);
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, hashTime, "Server Hash Insertion");

        stopWatch.start();
        lcotSenderOutput = lcotSender.send(binSize * binNum);
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lcotTime, "Server LOT");

        stopWatch.start();
        // 发送服务端哈希桶PRF过滤器
        List<byte[]> serverPrfPayload = generatePrfPayload();
        DataPacketHeader serverPrfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr17EcPsiPtoDesc.PtoStep.SERVER_SEND_TUPLES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrfHeader, serverPrfPayload));
        extraInfo++;

        lcotSenderOutput = null;
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime, "Server computes Tuples");

        logPhaseInfo(PtoState.PTO_END);
    }


    private List<byte[]> generatePrfPayload() {
        List<byte[]> prfList;
        // todo 为了并行化，代码有些难看，主要原因是Commit要初始化多个
        if(parallel){
            int parallelNum = ForkJoinPool.getCommonPoolParallelism();
            int eachThreadNum = Math.max(binNum * binSize / parallelNum, 1);
            prfList = IntStream.range(0, Math.min(parallelNum, binNum * binSize)).parallel().mapToObj(tIndex -> {
                int startIndex = eachThreadNum * tIndex, endIndex = (tIndex == parallelNum - 1) ? binNum * binSize : eachThreadNum * (tIndex + 1);
                return IntStream.range(startIndex, endIndex).mapToObj(index -> generateCommit(index, tIndex))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            }).flatMap(Collection::stream).collect(Collectors.toList());
        }else{
            IntStream serverElementStream = IntStream.range(0, binNum * binSize);
            prfList = serverElementStream.mapToObj(index -> generateCommit(index, 0))
                .filter(Objects::nonNull).collect(Collectors.toList());
        }
        Collections.shuffle(prfList, secureRandom);
        return prfList;
    }

    private byte[] generateCommit(int index, int tIndex){
        int binIndex = index / binSize;
        int entryIndex = index % binSize;
        HashBinEntry<BigInteger> hashBinEntry = tools.phaseHashBin.getBin(binIndex).get(entryIndex);
        if (hashBinEntry.getHashIndex() == 0) {
            byte[] rx = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
            byte[] elementByteArray = BigIntegerUtils.nonNegBigIntegerToByteArray(tools.phaseHashBin.dephaseItem(binIndex, hashBinEntry.getItem()), tools.h1.getOutputByteLength());
            byte[] phasedElementByteArray = BigIntegerUtils.nonNegBigIntegerToByteArray(hashBinEntry.getItem(), tools.encodeInputByteLength);
            Commitment commitment = tools.commits[tIndex].commit(ByteBuffer.allocate(
                rx.length + elementByteArray.length).put(elementByteArray).put(rx).array());
            ByteBuffer tupleArray = ByteBuffer.allocate(commitment.getSecret().length + commitment.getCommitment().length + binSize * (tools.tagPrfByteLength +  CommonConstants.BLOCK_BYTE_LENGTH))
                .put(commitment.getSecret()).put(commitment.getCommitment());
            for(int i = 0; i < binSize; i++){
                byte[] encode = tools.peqtHash.digestToBytes(ByteBuffer.allocate(
                        lcotSenderOutput.getOutputByteLength() + phasedElementByteArray.length)
                    .put(phasedElementByteArray).put(lcotSenderOutput.getRb(binIndex * binSize + i, phasedElementByteArray)).array());
                byte[] tag = tools.prfTag.getBytes(encode);
                byte[] enc = tools.prfEnc.getBytes(encode);
                tupleArray.put(tag).put(BytesUtils.xor(rx, enc));
            }
            return tupleArray.array();
        }
        return null;
    }
}
