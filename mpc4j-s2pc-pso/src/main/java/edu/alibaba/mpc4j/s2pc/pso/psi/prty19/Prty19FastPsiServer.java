package edu.alibaba.mpc4j.s2pc.pso.psi.prty19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.filter.Filter;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Prty19FastPsiServer <T> extends AbstractPsiServer<T> {
    /**
     * OPRF发送方
     */
    private final MpOprfSender oprfSender;
    /**
     * PEQT哈希函数
     */
    private Hash peqtHash;
    /**
     * 过滤器类型
     */
    private final FilterFactory.FilterType filterType;
    /**
     * OPRF发送方输出
     */
    private MpOprfSenderOutput oprfSenderOutput;

    public Prty19FastPsiServer(Rpc serverRpc, Party clientParty, Prty19FastPsiConfig config) {
        super(Prty19FastPsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        oprfSender = OprfFactory.createMpOprfSender(serverRpc, clientParty, config.getMpOprfConfig());
        addSubPtos(oprfSender);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        oprfSender.init(maxClientElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);

        oprfSenderOutput = oprfSender.oprf(clientElementSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime);

        stopWatch.start();
        // 发送服务端PRF0过滤器
        List<byte[]> serverPrf0Payload = generatePrfPayload(0);
        DataPacketHeader serverPrf0Header = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Prty19FastPsiPtoDesc.PtoStep.SERVER_SEND_PRFS_0.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrf0Header, serverPrf0Payload));
        // 发送服务端PRF1过滤器
        List<byte[]> serverPrf1Payload = generatePrfPayload(1);
        DataPacketHeader serverPrf1Header = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Prty19FastPsiPtoDesc.PtoStep.SERVER_SEND_PRFS_1.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrf1Header, serverPrf1Payload));
        extraInfo++;
        oprfSenderOutput = null;
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, serverPrfTime);

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<byte[]> generatePrfPayload(int index) {
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> prfList = serverElementStream
                .map(element -> {
                    byte[] elementByteArray = ObjectUtils.objectToByteArray(element);
                    byte[] prf = oprfSenderOutput.getPrf(index, elementByteArray);
                    return peqtHash.digestToBytes(prf);
                })
                .collect(Collectors.toList());
        Collections.shuffle(prfList, secureRandom);
        // 构建过滤器
        Filter<byte[]> prfFilter = FilterFactory.createFilter(envType, filterType, serverElementSize, secureRandom);
        prfList.forEach(prfFilter::put);
        return prfFilter.toByteArrayList();
    }
}

