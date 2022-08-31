package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.LotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.AbstractNcLotSender;

import java.util.concurrent.TimeUnit;

/**
 * 直接NC-LOT协议发送方。
 *
 * @author Hanwen Feng
 * @date 2022/8/18
 */
public class DirectNcLotSender extends AbstractNcLotSender {
    /**
     * LHOT协议发送方
     */
    private final LhotSender lhotSender;

    public DirectNcLotSender(Rpc senderRpc, Party receiverParty, DirectNcLotConfig config) {
        super(DirectNcLotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        lhotSender = LhotFactory.createSender(senderRpc,receiverParty, config.getLhotConfig());
        lhotSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        lhotSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        lhotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        lhotSender.addLogLevel();
    }

    @Override
    public void init(int inputBitLength, int num) throws MpcAbortException {
        setInitInput(inputBitLength, num);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        lhotSender.init(inputBitLength, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public LotSenderOutput send() throws MpcAbortException {
        setPtoInput();
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        LotSenderOutput senderOutput = lhotSender.send(num);
        stopWatch.stop();
        long lhotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        senderOutput.reduce(num);
        stopWatch.reset();
        info("{}{} Send. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), lhotTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }
}
