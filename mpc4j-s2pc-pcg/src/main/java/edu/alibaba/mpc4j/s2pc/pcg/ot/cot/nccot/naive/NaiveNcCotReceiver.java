package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.naive;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.AbstractNcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotReceiver;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 朴素NCCOT接收方。
 *
 * @author Weiran Liu
 * @date 2022/7/14
 */
public class NaiveNcCotReceiver extends AbstractNcCotReceiver {
    /**
     * RCOT协议接收方
     */
    private final RcotReceiver rcotReceiver;

    public NaiveNcCotReceiver(Rpc receiverRpc, Party senderParty, NaiveNcCotConfig config) {
        super(NaiveNcCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        rcotReceiver = RcotFactory.createReceiver(receiverRpc, senderParty, config.getRcotConfig());
        rcotReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        rcotReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        rcotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        rcotReceiver.addLogLevel();
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        rcotReceiver.init(num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public CotReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 随机生成选择比特，应用RCOT协议生成指定数量的COT
        boolean[] choices = new boolean[num];
        IntStream.range(0, num).forEach(index -> choices[index] = secureRandom.nextBoolean());
        CotReceiverOutput receiverOutput = rcotReceiver.receive(choices);
        receiverOutput.reduce(num);
        stopWatch.stop();
        long rcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), rcotTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }
}
