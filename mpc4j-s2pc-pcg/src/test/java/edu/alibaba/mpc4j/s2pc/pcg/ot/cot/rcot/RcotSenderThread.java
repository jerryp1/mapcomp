package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * RCOT协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2021/01/26
 */
class RcotSenderThread extends Thread {
    /**
     * 发送方
     */
    private final RcotSender sender;
    /**
     * 关联值Δ
     */
    private final byte[] delta;
    /**
     * 数量
     */
    private final int num;
    /**
     * 输出
     */
    private CotSenderOutput senderOutput;

    RcotSenderThread(RcotSender sender, byte[] delta, int num) {
        this.sender = sender;
        this.delta = delta;
        this.num = num;
    }

    CotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(delta, num);
            senderOutput = sender.send(num);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}