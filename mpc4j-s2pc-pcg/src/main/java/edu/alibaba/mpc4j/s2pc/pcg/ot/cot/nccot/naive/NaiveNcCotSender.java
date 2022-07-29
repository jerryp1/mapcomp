package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.naive;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.AbstractNcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotSender;

import java.util.concurrent.TimeUnit;

/**
 * 朴素NCCOT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/7/14
 */
public class NaiveNcCotSender extends AbstractNcCotSender {
    /**
     * RCOT协议发送方
     */
    private final RcotSender rcotSender;

    public NaiveNcCotSender(Rpc senderRpc, Party receiverParty, NaiveNcCotConfig config) {
        super(NaiveNcCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
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
    public void init(byte[] delta, int num) throws MpcAbortException {
        setInitInput(delta, num);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        rcotSender.init(delta, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public CotSenderOutput send() throws MpcAbortException {
        setPtoInput();
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
