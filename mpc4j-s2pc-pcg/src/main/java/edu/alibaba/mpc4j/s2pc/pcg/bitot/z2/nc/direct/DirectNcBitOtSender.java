package edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.BitOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.AbstractNcBitOtSender;
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
    /**
     * 按byte存储时byte数量
     */
    protected int byteNum;
    /**
     * 按byte存储的偏置量
     */
    protected int offset;

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
        this.byteNum = CommonUtils.getByteLength(num);
        this.offset = byteNum * Byte.SIZE - num;
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

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
        stopWatch.reset();
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
        byte[] r0Array = new byte[byteNum];
        byte[] r1Array = new byte[byteNum];
        Crhf crhf = CrhfFactory.createInstance(getEnvType(), CrhfFactory.CrhfType.MMO);
        IntStream.range(0, num)
                .forEach(index -> {
                    BinaryUtils.setBoolean(r0Array, offset + index,
                            crhf.hash(cotSenderOutput.getR0(index))[0] / 2 == 1);
                    BinaryUtils.setBoolean(r1Array, offset + index,
                            crhf.hash(cotSenderOutput.getR1(index))[0] / 2 == 1);
        });
        return BitOtSenderOutput.create(num, r0Array, r1Array);
    }
}
