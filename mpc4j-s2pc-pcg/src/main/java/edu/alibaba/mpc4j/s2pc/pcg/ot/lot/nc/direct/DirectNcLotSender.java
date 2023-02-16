package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.LotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.CoreLotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core.CoreLotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.AbstractNcLotSender;

import java.util.concurrent.TimeUnit;

/**
 * 直接NC-2^l选1-OT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/8/18
 */
public class DirectNcLotSender extends AbstractNcLotSender {
    /**
     * 核2^l选1-OT协议发送方
     */
    private final CoreLotSender coreLotSender;

    public DirectNcLotSender(Rpc senderRpc, Party receiverParty, DirectNcLotConfig config) {
        super(DirectNcLotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreLotSender = CoreLotFactory.createSender(senderRpc, receiverParty, config.getCoreLotConfig());
        addSubPtos(coreLotSender);
    }

    @Override
    public void init(int inputBitLength, int num) throws MpcAbortException {
        setInitInput(inputBitLength, num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreLotSender.init(inputBitLength, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LotSenderOutput send() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        LotSenderOutput senderOutput = coreLotSender.send(num);
        stopWatch.stop();
        long coreLotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        senderOutput.reduce(num);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, coreLotTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
