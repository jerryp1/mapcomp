package edu.alibaba.mpc4j.s2pc.pso.psi.prty20;

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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Prty20PsiServer <T> extends AbstractPsiServer<T> {
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

    public Prty20PsiServer(Rpc serverRpc, Party clientParty, Prty20PsiConfig config) {
        super(Prty20PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        oprfSender = OprfFactory.createMpOprfSender(serverRpc, clientParty, config.getMpOprfConfig());
        addSubPtos(oprfSender);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

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
        // 发送服务端PRF过滤器
        List<byte[]> serverPrfPayload = generatePrfPayload();
        DataPacketHeader serverPrfHeader = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Prty20PsiPtoDesc.PtoStep.SERVER_SEND_PRFS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrfHeader, serverPrfPayload));
        extraInfo++;
        oprfSenderOutput = null;
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, serverPrfTime);

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<byte[]> generatePrfPayload() {
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> prfList = serverElementStream
                .map(element -> {
                    byte[] elementByteArray = ObjectUtils.objectToByteArray(element);
                    byte[] prf = oprfSenderOutput.getPrf(elementByteArray);
                    return peqtHash.digestToBytes(ByteBuffer.allocate(prf.length + elementByteArray.length).put(elementByteArray).put(prf).array());
                })
                .collect(Collectors.toList());
        Collections.shuffle(prfList, secureRandom);
        // 构建过滤器
        Filter<byte[]> prfFilter = FilterFactory.createFilter(envType, filterType, serverElementSize, secureRandom);
        prfList.forEach(prfFilter::put);
        return prfFilter.toByteArrayList();
    }
}