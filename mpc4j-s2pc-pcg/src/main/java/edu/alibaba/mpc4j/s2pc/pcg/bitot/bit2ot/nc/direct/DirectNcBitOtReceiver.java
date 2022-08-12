package edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.BitOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.nc.AbstractNcBitOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotReceiver;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 直接NC-BitOt接收方。
 *
 * @author Hanwen Feng
 * @date 2022/08/11
 */
public class DirectNcBitOtReceiver extends AbstractNcBitOtReceiver {
    /**
     * NC-COT协议接收方。
     */
    private final NcCotReceiver ncCotReceiver;

    public DirectNcBitOtReceiver(Rpc receiverRpc, Party senderParty, DirectNcBitOtConfig config) {
        super(DirectNcBitOtPtoDesc.getInstance(), receiverRpc, senderParty, config);
        ncCotReceiver = NcCotFactory.createReceiver(receiverRpc, senderParty, config.getNcCotConfig());
        ncCotReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        ncCotReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        ncCotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        ncCotReceiver.addLogLevel();
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        ncCotReceiver.init(num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BitOtReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = ncCotReceiver.receive();
        BitOtReceiverOutput receiverOutput = generateBitOutput(cotReceiverOutput);
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        info("{}{} Recv. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cotTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());

        return receiverOutput;
    }

    /**
     * 截取COT接收方输出的第一个比特作为Bit-OT接收方输出。
     *
     * @param cotReceiverOutput COT接收方输出。
     * @return Bit-OT接收方输出。
     */
    private BitOtReceiverOutput generateBitOutput(CotReceiverOutput cotReceiverOutput) {
        boolean[] choices = new boolean[num];
        boolean[] rbArray = new boolean[num];
        Crhf crhf = CrhfFactory.createInstance(getEnvType(), CrhfFactory.CrhfType.MMO);
        IntStream stream = IntStream.range(0, num);
        stream = parallel? stream.parallel(): stream;
        stream.forEach(index -> {
            choices[index] = cotReceiverOutput.getChoice(index);
            rbArray[index] = BinaryUtils.getBoolean(crhf.hash(cotReceiverOutput.getRb(index)),0);
        });
        return BitOtReceiverOutput.create(choices, rbArray);
    }

}
