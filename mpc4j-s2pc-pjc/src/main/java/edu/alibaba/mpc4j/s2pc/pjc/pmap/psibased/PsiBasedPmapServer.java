package edu.alibaba.mpc4j.s2pc.pjc.pmap.psibased;

import cc.redberry.rings.bigint.BigInteger;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.AbstractPmapServer;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput.MapType;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.psibased.PsiBasedPmapPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiServer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * @author Feng Han
 * @date 2024/7/22
 */
public class PsiBasedPmapServer<T> extends AbstractPmapServer<T> {
    /**
     * pid server
     */
    private final PsiServer<T> psiServer;

    public PsiBasedPmapServer(Rpc serverRpc, Party clientParty, PsiBasedPmapConfig config) {
        super(PsiBasedPmapPtoDesc.getInstance(), serverRpc, clientParty, config);
        psiServer = PsiFactory.createServer(serverRpc, clientParty, config.getPsiConfig());
        addMultipleSubPtos(psiServer);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        psiServer.init(maxServerElementSize, maxClientElementSize);
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PmapPartyOutput<T> map(List<T> serverElementList, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementList, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // 1. psi
        stopWatch.start();
        psiServer.psi(new HashSet<>(serverElementList), clientElementSize);
        logStepInfo(PtoState.PTO_STEP, 1, 2, resetAndGetTime());

        // 2. get psi result
        stopWatch.start();
        List<byte[]> psiRes = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_SPI_RES.ordinal());
        byte[][] transElements = serverElementList.stream().map(ObjectUtils::objectToByteArray).toArray(byte[][]::new);
        HashMap<BigInteger, Integer> originalMap = new HashMap<>();
        for(int i = 0; i < transElements.length; i++){
            originalMap.put(new BigInteger(transElements[i]), i);
        }
        HashMap<Integer, T> resMap = new HashMap<>();
        int index = 0;
        for(byte[] x : psiRes){
            BigInteger tmp = new BigInteger(x);
            assert originalMap.containsKey(tmp);
            resMap.put(index++, serverElementList.get(originalMap.get(tmp)));
        }
        PmapPartyOutput<T> res = new PmapPartyOutput<>(MapType.PSI, serverElementList, resMap,
            SquareZ2Vector.create(BitVectorFactory.createOnes(resMap.size()), false));
        logStepInfo(PtoState.PTO_STEP, 2, 2, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }
}
