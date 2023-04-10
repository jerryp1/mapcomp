package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfSenderOutput;

/**
 * MPOPRF发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/4/9
 */
class MpOprfSenderThread extends Thread {
    /**
     * 发送方
     */
    private final MpOprfSender sender;
    /**
     * 批处理数量
     */
    private final int batchSize;
    /**
     * 输出
     */
    private MpOprfSenderOutput senderOutput;

    MpOprfSenderThread(MpOprfSender sender, int batchSize) {
        this.sender = sender;
        this.batchSize = batchSize;
    }

    MpOprfSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(batchSize);
            senderOutput = sender.oprf(batchSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
