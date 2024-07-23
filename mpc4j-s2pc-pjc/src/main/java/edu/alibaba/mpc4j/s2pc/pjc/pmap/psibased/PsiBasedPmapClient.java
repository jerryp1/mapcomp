package edu.alibaba.mpc4j.s2pc.pjc.pmap.psibased;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.AbstractPmapClient;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput.MapType;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.psibased.PsiBasedPmapPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Feng Han
 * @date 2024/7/22
 */
public class PsiBasedPmapClient<T> extends AbstractPmapClient<T> {
    /**
     * pid server
     */
    private final PsiClient<T> psiClient;

    public PsiBasedPmapClient(Rpc clientRpc, Party serverParty, PsiBasedPmapConfig config) {
        super(PsiBasedPmapPtoDesc.getInstance(), clientRpc, serverParty, config);
        psiClient = PsiFactory.createClient(clientRpc, serverParty, config.getPsiConfig());
        addMultipleSubPtos(psiClient);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        psiClient.init(maxClientElementSize, maxServerElementSize);
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PmapPartyOutput<T> map(List<T> clientElementList, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementList, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // 1. psi
        stopWatch.start();
        Set<T> psiRes = psiClient.psi(new HashSet<>(clientElementList), serverElementSize);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime());

        // 2. get psi result
        stopWatch.start();
        List<T> psiList = new ArrayList<>(psiRes);
        List<byte[]> msg = psiList.stream().map(ObjectUtils::objectToByteArray).collect(Collectors.toList());
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_SPI_RES.ordinal(), msg);
        HashMap<Integer, T> resMap = new HashMap<>();
        int index = 0;
        for(T x : psiList){
            resMap.put(index++, x);
        }
        PmapPartyOutput<T> res = new PmapPartyOutput<>(MapType.PSI, clientElementList, resMap,
            SquareZ2Vector.create(BitVectorFactory.createZeros(resMap.size()), false));
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }
}
