package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.LotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.AbstractNcLotReceiver;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class DirectNcLotReceiver extends AbstractNcLotReceiver {
    /**
     * LHOT协议接收方
     */
    private final LhotReceiver lhotReceiver;

    public DirectNcLotReceiver(Rpc receiverRpc, Party senderParty, DirectNcLotConfig config) {
        super(DirectNcLotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        lhotReceiver = LhotFactory.createReceiver(receiverRpc, senderParty, config.getLhotConfig());
        lhotReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        lhotReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        lhotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        lhotReceiver.addLogLevel();
    }

    @Override
    public void init(int inputBitLength, int num) throws MpcAbortException {
        setInitInput(inputBitLength, num);
        info("{}{} Rec. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        lhotReceiver.init(inputBitLength, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Rec. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Rec. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());

    }

    @Override
    public LotReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        info("{}{} Rec. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int inputByteLength = CommonUtils.getByteLength(inputBitLength);
        byte[][] choices = IntStream.range(0, num)
                .mapToObj(index -> {
                    byte[] choice = new byte[inputByteLength];
                    secureRandom.nextBytes(choice);
                    BytesUtils.reduceByteArray(choice, inputBitLength);
                    return choice;
                })
                .toArray(byte[][]::new);
        LotReceiverOutput receiverOutput = lhotReceiver.receive(choices);
        stopWatch.stop();
        long lhTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        receiverOutput.reduce(num);
        stopWatch.reset();
        info("{}{} Rec. Rec 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), lhTime);

        info("{}{} Rec. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }
}
