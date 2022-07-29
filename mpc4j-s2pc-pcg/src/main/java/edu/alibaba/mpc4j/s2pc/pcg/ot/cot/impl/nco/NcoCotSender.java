package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.nco;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.AbstractCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.PcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.PcotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotSender;

/**
 * NCO-COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/07/13
 */
public class NcoCotSender extends AbstractCotSender {
    /**
     * NCCOT协议发送方
     */
    private final NcCotSender ncCotSender;
    /**
     * PCOT协议发送方
     */
    private final PcotSender pcotSender;

    public NcoCotSender(Rpc senderRpc, Party receiverParty, NcoCotConfig config) {
        super(NcoCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        ncCotSender = NcCotFactory.createSender(senderRpc, receiverParty, config.getNcCotConfig());
        ncCotSender.addLogLevel();
        pcotSender = PcotFactory.createSender(senderRpc, receiverParty, config.getPcotConfig());
        pcotSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        // NCCOT协议和PCOT协议需要使用不同的taskID
        byte[] taskIdBytes = ByteBuffer.allocate(Long.BYTES).putLong(taskId).array();
        ncCotSender.setTaskId(taskIdPrf.getLong(0, taskIdBytes, Long.MAX_VALUE));
        pcotSender.setTaskId(taskIdPrf.getLong(1, taskIdBytes, Long.MAX_VALUE));
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        ncCotSender.setParallel(parallel);
        pcotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        ncCotSender.addLogLevel();
        pcotSender.addLogLevel();
    }

    @Override
    public void init(byte[] delta, int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(delta, maxRoundNum, updateNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        ncCotSender.init(delta, maxRoundNum);
        pcotSender.init();
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
        // 应用NCCOT协议生成指定数量的COT，并裁剪
        CotSenderOutput senderOutput = ncCotSender.send();
        senderOutput.reduce(num);
        stopWatch.stop();
        long nccotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), nccotTime);

        stopWatch.start();
        // 应用PCOT协议纠正选择比特
        senderOutput = pcotSender.send(senderOutput);
        stopWatch.stop();
        long pcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/2 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pcotTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }
}
