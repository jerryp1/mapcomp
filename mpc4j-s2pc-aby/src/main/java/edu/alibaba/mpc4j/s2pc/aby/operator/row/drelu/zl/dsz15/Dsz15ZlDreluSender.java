package edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.dsz15;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.AbstractZlDreluParty;

import java.util.concurrent.TimeUnit;

/**
 * DSZ15 Zl DReLU Sender.
 *
 * @author Li Peng
 * @date 2023/11/18
 */
public class Dsz15ZlDreluSender extends AbstractZlDreluParty {
    /**
     * A2b sender.
     */
    private final A2bParty a2bSender;
    /**
     * Z2 circuit party.
     */
    private final Z2cParty z2cSender;

    public Dsz15ZlDreluSender(Rpc senderRpc, Party receiverParty, Dsz15ZlDreluConfig config) {
        super(Dsz15ZlDreluPtoDesc.getInstance(), senderRpc, receiverParty, config);
        a2bSender = A2bFactory.createSender(senderRpc, receiverParty, config.getA2bConfig());
        addSubPtos(a2bSender);
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        addSubPtos(z2cSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        a2bSender.init(maxL, maxNum);
        z2cSender.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector drelu(SquareZlVector xi) throws MpcAbortException {
        setPtoInput(xi);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // a2b
        stopWatch.start();
        SquareZ2Vector[] a2bResult = a2bSender.a2b(xi);

        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, prepareTime);

        logPhaseInfo(PtoState.PTO_END);
        return z2cSender.not(a2bResult[0]);
    }
}
