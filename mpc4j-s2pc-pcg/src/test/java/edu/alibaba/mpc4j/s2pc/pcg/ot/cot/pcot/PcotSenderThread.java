package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * PCOT协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
class PcotSenderThread extends Thread {
    /**
     * 发送方
     */
    private final PcotSender sender;
    /**
     * 预计算输出
     */
    private final CotSenderOutput preSenderOutput;
    /**
     * 输出
     */
    private CotSenderOutput senderOutput;

    PcotSenderThread(PcotSender sender, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.preSenderOutput = preSenderOutput;
    }

    CotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init();
            senderOutput = sender.send(preSenderOutput);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
