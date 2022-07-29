package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.rto;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.AbstractCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotSender;

import java.util.concurrent.TimeUnit;

/**
 * RTO-COT发送方。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class RtoCotSender extends AbstractCotSender {
    /**
     * RCOT协议发送方
     */
    private final RcotSender rcotSender;

    public RtoCotSender(Rpc senderRpc, Party receiverParty, RtoCotConfig config) {
        super(RtoCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        rcotSender = RcotFactory.createSender(senderRpc, receiverParty, config.getRcotConfig());
        rcotSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        rcotSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        rcotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        rcotSender.addLogLevel();
    }

    @Override
    public void init(byte[] delta, int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(delta, maxRoundNum, updateNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        rcotSender.init(delta, maxRoundNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public CotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 应用RCOT协议生成指定数量的COT
        CotSenderOutput senderOutput = rcotSender.send(num);
        stopWatch.stop();
        long rcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), rcotTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }
}
