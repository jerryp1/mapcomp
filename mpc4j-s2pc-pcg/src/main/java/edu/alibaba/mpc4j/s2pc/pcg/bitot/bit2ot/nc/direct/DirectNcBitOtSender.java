package edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.BitOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.nc.AbstractNcBitOtSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotSender;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 直接NC-BitOT发送方。
 *
 * @author Hanwen Feng
 * @date 2022/08/12
 */
public class DirectNcBitOtSender extends AbstractNcBitOtSender {
    /**
     * NC-COT协议发送方。
     */
    private final NcCotSender ncCotSender;

    public DirectNcBitOtSender(Rpc senderRpc, Party receiverParty, DirectNcBitOtConfig config) {
        super(DirectNcBitOtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        ncCotSender = NcCotFactory.createSender(senderRpc, receiverParty, config.getNcCotConfig());
        ncCotSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        ncCotSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        ncCotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        ncCotSender.addLogLevel();
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        ncCotSender.init(delta, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BitOtSenderOutput send() throws MpcAbortException {
        setPtoInput();
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        CotSenderOutput cotSenderOutput = ncCotSender.send();
        BitOtSenderOutput senderOutput = generateBitOutput(cotSenderOutput);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        info("{}{} Send. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());

        return senderOutput;
    }

    /**
     * 截取COT接收方输出的第一个比特作为Bit-OT接收方输出。
     *
     * @param cotSenderOutput COT接收方输出。
     * @return Bit-OT接收方输出。
     */
    private BitOtSenderOutput generateBitOutput(CotSenderOutput cotSenderOutput) {
        boolean[] r0Array = new boolean[num];
        boolean[] r1Array = new boolean[num];
        Crhf crhf = CrhfFactory.createInstance(getEnvType(), CrhfFactory.CrhfType.MMO);
        IntStream stream = IntStream.range(0, num);
        stream = parallel? stream.parallel(): stream;
        stream.forEach(index -> {
            r0Array[index] = BinaryUtils.getBoolean(
                    crhf.hash(cotSenderOutput.getR0(index)),0
            );
            r1Array[index] = BinaryUtils.getBoolean(
                    crhf.hash(cotSenderOutput.getR1(index)),0
            );
        });
        return BitOtSenderOutput.create(r0Array, r1Array);
    }
}
