package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.nco;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.AbstractCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.PcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.PcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotReceiver;

/**
 * NCO-COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/04
 */
public class NcoCotReceiver extends AbstractCotReceiver {
    /**
     * NC-COT协议接收方
     */
    private final NcCotReceiver ncCotReceiver;
    /**
     * PCOT协议接收方
     */
    private final PcotReceiver pcotReceiver;

    public NcoCotReceiver(Rpc receiverRpc, Party senderParty, NcoCotConfig config) {
        super(NcoCotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        ncCotReceiver = NcCotFactory.createReceiver(receiverRpc, senderParty, config.getNcCotConfig());
        ncCotReceiver.addLogLevel();
        pcotReceiver = PcotFactory.createReceiver(receiverRpc, senderParty, config.getPcotConfig());
        pcotReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        // NCCOT协议和PCOT协议需要使用不同的taskID
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        ncCotReceiver.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        pcotReceiver.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        ncCotReceiver.setParallel(parallel);
        pcotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        ncCotReceiver.addLogLevel();
        pcotReceiver.addLogLevel();
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        ncCotReceiver.init(maxRoundNum);
        pcotReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public CotReceiverOutput receive(boolean[] choices) throws MpcAbortException {
        setPtoInput(choices);
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 应用NCCOT协议生成指定数量的COT，并裁剪
        CotReceiverOutput receiverOutput = ncCotReceiver.receive();
        receiverOutput.reduce(num);
        stopWatch.stop();
        long nccotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), nccotTime);

        stopWatch.start();
        // 应用PCOT协议纠正选择比特
        receiverOutput = pcotReceiver.receive(receiverOutput, choices);
        stopWatch.stop();
        long pcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pcotTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }
}
