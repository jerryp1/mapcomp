package edu.alibaba.mpc4j.s2pc.pso.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * OPRF协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2019/07/12
 */
class OprfSenderThread extends Thread {
    /**
     * 发送方
     */
    private final OprfSender sender;
    /**
     * 批处理数量
     */
    private final int batchSize;
    /**
     * 输出
     */
    private OprfSenderOutput senderOutput;

    OprfSenderThread(OprfSender sender, int batchSize) {
        this.sender = sender;
        this.batchSize = batchSize;
    }

    OprfSenderOutput getSenderOutput() {
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