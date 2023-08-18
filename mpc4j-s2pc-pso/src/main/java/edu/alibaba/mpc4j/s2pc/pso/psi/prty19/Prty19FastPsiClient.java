package edu.alibaba.mpc4j.s2pc.pso.psi.prty19;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.filter.Filter;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Prty19FastPsiClient<T> extends AbstractPsiClient<T> {
    /**
     * OPRF接收方
     */
    private final MpOprfReceiver oprfReceiver;
    /**
     * 客户端元素的PRF结果
     */
    private ArrayList<byte[]> clientOprfArrayList;
    /**
     * 服务端元素的PRF结果
     */
    List<Filter<byte[]>> serverPrfFilterList;
    /**
     * PEQT哈希函数
     */
    private Hash peqtHash;


    public Prty19FastPsiClient(Rpc clientRpc, Party serverParty, Prty19FastPsiConfig config) {
        super(Prty19FastPsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        oprfReceiver = OprfFactory.createMpOprfReceiver(clientRpc, serverParty, config.getMpOprfConfig());
        addSubPtos(oprfReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        oprfReceiver.init(maxClientElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverSetSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        peqtHash = HashFactory.createInstance(envType, peqtByteLength);

        MpOprfReceiverOutput oprfReceiverOutput = oprfReceiver.oprf(
                clientElementArrayList.stream()
                        .map(ObjectUtils::objectToByteArray)
                        .toArray(byte[][]::new));
        IntStream oprfIndexIntStream = IntStream.range(0, clientElementSize);
        oprfIndexIntStream = parallel ? oprfIndexIntStream.parallel() : oprfIndexIntStream;
        clientOprfArrayList = oprfIndexIntStream
                .mapToObj(index -> peqtHash.digestToBytes(oprfReceiverOutput.getPrf(index)))
                .collect(Collectors.toCollection(ArrayList::new));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime);

        stopWatch.start();
        // 接收服务端哈希桶PRF过滤器
        DataPacketHeader serverPrf0Header = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Prty19FastPsiPtoDesc.PtoStep.SERVER_SEND_PRFS_0.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrf0Payload = rpc.receive(serverPrf0Header).getPayload();

        DataPacketHeader serverPrf1Header = new DataPacketHeader(
                this.encodeTaskId, getPtoDesc().getPtoId(), Prty19FastPsiPtoDesc.PtoStep.SERVER_SEND_PRFS_1.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrf1Payload = rpc.receive(serverPrf1Header).getPayload();
        extraInfo++;

        Set<T> intersection = handleServerPrfPayload(serverPrf0Payload, serverPrf1Payload);
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, serverPrfTime);

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }


    private Set<T> handleServerPrfPayload(List<byte[]> serverPrf0Payload, List<byte[]> serverPrf1Payload) throws MpcAbortException {
        serverPrfFilterList = new ArrayList<>();
        try {
            serverPrfFilterList.add(FilterFactory.createFilter(envType, serverPrf0Payload));
            serverPrfFilterList.add(FilterFactory.createFilter(envType, serverPrf1Payload));
        } catch (IllegalArgumentException e) {
            throw new MpcAbortException();
        }
        Set<T> intersection = IntStream.range(0, clientElementSize)
                .mapToObj(elementIndex -> {
                    T element = clientElementArrayList.get(elementIndex);
                    byte[] elementPrf = clientOprfArrayList.get(elementIndex);
                    return (serverPrfFilterList.get(0).mightContain(elementPrf) | serverPrfFilterList.get(1).mightContain(elementPrf)) ? element : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        serverPrfFilterList = null;
        clientOprfArrayList = null;
        return intersection;
    }
}