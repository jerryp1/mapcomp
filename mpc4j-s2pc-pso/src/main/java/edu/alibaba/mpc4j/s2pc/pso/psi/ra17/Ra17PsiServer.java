package edu.alibaba.mpc4j.s2pc.pso.psi.ra17;

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
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSender;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Ra17PsiServer<T> extends AbstractPsiServer<T> {
    /**
     * OPRF发送方
     */
    private final SqOprfSender oprfSender;
    /**
     * PEQT哈希函数
     */
    private Hash peqtHash;
    /**
     * 过滤器类型
     */
    private final FilterFactory.FilterType filterType;
    /**
     * the key
     */
    private SqOprfKey key;

    public Ra17PsiServer(Rpc serverRpc, Party clientParty, Ra17PsiConfig config) {
        super(Ra17PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        oprfSender = SqOprfFactory.createSender(serverRpc, clientParty, config.getSqOprfConfig());
        addSubPtos(oprfSender);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        key = oprfSender.keyGen();
        oprfSender.init(maxClientElementSize, key);
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

        oprfSender.oprf(clientElementSize);
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime);

        stopWatch.start();
        // 发送服务端Ra17PRF过滤器
        List<byte[]> serverPrfPayload = generatePrfPayload();
        DataPacketHeader serverPrfHeader = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Ra17PsiPtoDesc.PtoStep.SERVER_SEND_BLIND_PRFS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrfHeader, serverPrfPayload));
        extraInfo++;
        key = null;
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
                    byte[] prf = key.getPrf(elementByteArray);
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

