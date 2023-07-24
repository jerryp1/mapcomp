package edu.alibaba.mpc4j.s2pc.opf.psm.cgs22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.pesm.PesmFactory;
import edu.alibaba.mpc4j.s2pc.opf.pesm.PesmReceiver;
import edu.alibaba.mpc4j.s2pc.opf.psm.AbstractPsmReceiver;

import java.util.concurrent.TimeUnit;

/**
 * CGS22 naive PSM receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22NaivePsmReceiver extends AbstractPsmReceiver {
    /**
     * PESM receiver
     */
    private final PesmReceiver pesmReceiver;

    public Cgs22NaivePsmReceiver(Rpc senderRpc, Party receiverParty, Cgs22NaivePsmConfig config) {
        super(Cgs22NaivePsmPtoDesc.getInstance(), senderRpc, receiverParty, config);
        pesmReceiver = PesmFactory.createReceiver(senderRpc, receiverParty, config.getPesmConfig());
        addSubPtos(pesmReceiver);
    }

    @Override
    public void init(int maxL, int d, int maxNum) throws MpcAbortException {
        setInitInput(maxL, d, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        pesmReceiver.init(maxL, d, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector psm(int l, byte[][] inputArray) throws MpcAbortException {
        setPtoInput(l, inputArray);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SquareZ2Vector z1 = pesmReceiver.pesm(l, d, inputArray);
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);
        return z1;
    }
}
