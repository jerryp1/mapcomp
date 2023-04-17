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
 * direct Boolean triple generation sender.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class DirectZ2MtgSender extends AbstractZ2MtgParty {
    /**
     * core boolean triple generation sender
     */
    private final Z2CoreMtgParty z2CoreMtgSender;

    public DirectZ2MtgSender(Rpc senderRpc, Party receiverParty, DirectZ2MtgConfig config) {
        super(DirectZ2MtgPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2CoreMtgSender = Z2CoreMtgFactory.createSender(senderRpc, receiverParty, config.getZ2CoreMtgConfig());
        addSubPtos(z2CoreMtgSender);
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        z2CoreMtgSender.init(maxRoundNum);
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
        Z2Triple senderOutput = z2CoreMtgSender.generate(num);
        stopWatch.stop();
        long splitTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, splitTripleTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
