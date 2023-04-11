package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSender;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class SqOprfSenderThread extends Thread{


    /**
     * 发送方
     */
    private final SqOprfSender sender;
    /**
     * 批处理数量
     */
    private final int batchSize;
    /**
     * 输出
     */
    private SqOprfSenderOutput senderOutput;

    SqOprfSenderThread(SqOprfSender sender, int batchSize) {
        this.sender = sender;
        this.batchSize = batchSize;
    }

    SqOprfSenderOutput getSenderOutput() {
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
