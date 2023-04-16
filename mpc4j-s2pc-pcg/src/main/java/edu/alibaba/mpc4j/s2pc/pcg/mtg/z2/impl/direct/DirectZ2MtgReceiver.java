package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.AbstractZ2MtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgParty;

import java.util.concurrent.TimeUnit;

/**
 * direct Boolean triple generation receiver.
 *
 * @author Weiran Liu
 * @date 2022/7/14
 */
public class DirectZ2MtgReceiver extends AbstractZ2MtgParty {
    /**
     * core Boolean triple generation receiver
     */
    private final Z2CoreMtgParty z2CoreMtgReceiver;

    public DirectZ2MtgReceiver(Rpc receiverRpc, Party senderParty, DirectZ2MtgConfig config) {
        super(DirectZ2MtgPtoDesc.getInstance(), receiverRpc, senderParty, config);
        z2CoreMtgReceiver = Z2CoreMtgFactory.createReceiver(receiverRpc, senderParty, config.getZ2CoreMtgConfig());
        addSubPtos(z2CoreMtgReceiver);
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2CoreMtgReceiver.init(maxRoundNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Z2Triple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        Z2Triple receiverOutput = z2CoreMtgReceiver.generate(num);
        stopWatch.stop();
        long splitTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, splitTripleTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
