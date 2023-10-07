package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr16;

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
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.filter.SparseRandomBloomFilter;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.RandomGbfGf2eDokvs;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * RR16 malicious PSI server.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/10/06
 */
public class Rr16PsiServer <T> extends AbstractPsiServer<T> {
    /**
     * 核COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * CT协议发送方
     */
    private final CoinTossParty ctSender;
    /**
     * COT发送方输出
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * OKVS keys
     */
    private byte[][] hashKeys;
    /**
     * 关联密钥
     */
    private byte[][] q1;
    /**
     * GBF
     */
    private RandomGbfGf2eDokvs<byte[]> gbf;
    /**
     * OT number
     */
    private int nOt;
    /**
     * Bloom Filter position num
     */
    private int nBf;
    /**
     * GBF hash num
     */
    private int hashNum;
    /**
     * PEQT byte length
     */
    private int peqtByteLength;
    /**
     * filter type
     */
    private final FilterType filterType;
    /**
     * PEQT hash
     */
    private Hash peqtHash;

    public Rr16PsiServer(Rpc serverRpc, Party clientParty, Rr16PsiConfig config) {
        super(Rr16PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        coreCotSender = CoreCotFactory.createSender(serverRpc, clientParty, config.getCoreCotConfig());
        ctSender = CoinTossFactory.createSender(serverRpc, clientParty, config.getCoinTossConfig());
        addSubPtos(coreCotSender);
        addSubPtos(ctSender);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        ctSender.init();
        this.hashKeys = ctSender.coinToss(1, CommonConstants.BLOCK_BIT_LENGTH);
        peqtByteLength = PsiUtils.getMaliciousPeqtByteLength(maxServerElementSize, maxClientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);

        this.nBf = SparseRandomBloomFilter.bitSize(maxClientElementSize);
        this.hashNum = SparseRandomBloomFilter.getHashNum(maxClientElementSize);
        // 初始化COT协议
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        this.nOt = Rr16PsiUtils.getOtBatchSize(maxClientElementSize);
        coreCotSender.init(delta, nOt);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 4, initCotTime, "Server generates key");

        // 执行COT协议
        stopWatch.start();
        this.cotSenderOutput = coreCotSender.send(nOt);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 4, cotTime, "Server OT");

        stopWatch.start();
        List<byte[]> challengePayload = genChallengePayload(maxClientElementSize);
        DataPacketHeader challengeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr16PsiPtoDesc.PtoStep.SERVER_SEND_CHANLLEGE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(challengeHeader, challengePayload));
        stopWatch.stop();
        long challengeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, challengeTime, "Server sends challenge");

        stopWatch.start();
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr16PsiPtoDesc.PtoStep.CLIENT_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();
        assert responsePayload.size() - 1 >= challengePayload.size() - Rr16PsiUtils.getCncThreshold(maxClientElementSize);
        checkClientResponse(responsePayload);
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, checkTime, "Server checks response");
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader piHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr16PsiPtoDesc.PtoStep.CLIENT_SEND_PERMUTATION.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> piPayload = rpc.receive(piHeader).getPayload();
        this.q1 = handlePiPayload(piPayload);
        this.gbf = new RandomGbfGf2eDokvs<>(envType, clientElementSize, nBf, CommonConstants.BLOCK_BIT_LENGTH, hashNum, hashKeys[0]);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cotTime, "Server receives msg");

        stopWatch.start();
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> serverPrfs = serverElementStream
            .map(element -> {
                byte[] elementByteArray = ObjectUtils.objectToByteArray(element);
                byte[] prf = gbf.decode(q1, elementByteArray);
                return peqtHash.digestToBytes(prf);
            })
            .collect(Collectors.toList());
        Collections.shuffle(serverPrfs, secureRandom);
        stopWatch.stop();
        long prfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, prfTime, "Server generates PRF");

        // construct the filter
        stopWatch.start();
        Filter<byte[]> serverPrfFilter = FilterFactory.createFilter(envType, filterType, serverElementSize, secureRandom);
        serverPrfs.forEach(serverPrfFilter::put);
        List<byte[]> serverPrfFilterPayload = serverPrfFilter.toByteArrayList();
        DataPacketHeader serverPrfFilterHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Rr16PsiPtoDesc.PtoStep.SERVER_SEND_PRFS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrfFilterHeader, serverPrfFilterPayload));
        stopWatch.stop();
        long serverFilterTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverFilterTime, "Server sends PRF filter");

        logPhaseInfo(PtoState.PTO_END);
    }

    List<byte[]> genChallengePayload(int maxBatchSize){
        double cncProb = Rr16PsiUtils.getCncProb(maxBatchSize);
        return IntStream.range(0, nOt).mapToObj(i ->
            secureRandom.nextDouble() < cncProb ? IntUtils.intToByteArray(i) : null)
            .filter(Objects::nonNull).collect(Collectors.toList());
    }

    void checkClientResponse(List<byte[]> responsePayload){
        int[] index = responsePayload.subList(0, responsePayload.size() - 1).stream().mapToInt(IntUtils::byteArrayToInt).toArray();
        byte[] zero = new byte[this.cotSenderOutput.getR0(0).length];
        IntStream.range(0, index.length).forEach(i -> BytesUtils.xori(zero, this.cotSenderOutput.getR0(index[i])));
        assert BytesUtils.equals(zero, responsePayload.get(responsePayload.size() - 1));
    }

    byte[][] handlePiPayload(List<byte[]> piPayload) {
        return IntStream.range(0, nBf).mapToObj(index ->
            cotSenderOutput.getR1(IntUtils.byteArrayToInt(piPayload.get(index)))).toArray(byte[][]::new);
    }
}