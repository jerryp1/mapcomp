package edu.alibaba.mpc4j.s2pc.opf.psm.cgs22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.pesm.PesmFactory;
import edu.alibaba.mpc4j.s2pc.opf.pesm.PesmSender;
import edu.alibaba.mpc4j.s2pc.opf.psm.AbstractPsmSender;

import java.util.concurrent.TimeUnit;

/**
 * CGS22 naive PSM sender.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22NaivePsmSender extends AbstractPsmSender {
    /**
     * PESM sender
     */
    private final PesmSender pesmSender;

    public Cgs22NaivePsmSender(Rpc senderRpc, Party receiverParty, Cgs22NaivePsmConfig config) {
        super(Cgs22NaivePsmPtoDesc.getInstance(), senderRpc, receiverParty, config);
        pesmSender = PesmFactory.createSender(senderRpc, receiverParty, config.getPesmConfig());
        addSubPtos(pesmSender);
    }

    @Override
    public void init(int maxL, int d, int maxNum) throws MpcAbortException {
        setInitInput(maxL, d, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        pesmSender.init(maxL, d, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector psm(int l, byte[][][] inputArrays) throws MpcAbortException {
        setPtoInput(l, inputArrays);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SquareZ2Vector z0 = pesmSender.pesm(l, inputArrays);
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);
        return z0;
    }
}
