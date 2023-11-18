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
 * DSZ15 Zl DReLU Receiver.
 *
 * @author Li Peng
 * @date 2023/11/18
 */
public class Dsz15ZlDreluReceiver extends AbstractZlDreluParty {
    /**
     * A2b sender.
     */
    private final A2bParty a2bReceiver;
    /**
     * Z2 circuit party.
     */
    private final Z2cParty z2cReceiver;

    public Dsz15ZlDreluReceiver(Rpc receiverRpc, Party senderParty, Dsz15ZlDreluConfig config) {
        super(Dsz15ZlDreluPtoDesc.getInstance(), receiverRpc, senderParty, config);
        a2bReceiver = A2bFactory.createReceiver(receiverRpc, senderParty, config.getA2bConfig());
        addSubPtos(a2bReceiver);
        z2cReceiver = Z2cFactory.createReceiver(receiverRpc, senderParty, config.getZ2cConfig());
        addSubPtos(z2cReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        a2bReceiver.init(maxL, maxNum);
        z2cReceiver.init(maxNum);
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
        SquareZ2Vector[] a2bResult = a2bReceiver.a2b(xi);
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, prepareTime);

        logPhaseInfo(PtoState.PTO_END);

        return z2cReceiver.not(a2bResult[0]);
    }
}
