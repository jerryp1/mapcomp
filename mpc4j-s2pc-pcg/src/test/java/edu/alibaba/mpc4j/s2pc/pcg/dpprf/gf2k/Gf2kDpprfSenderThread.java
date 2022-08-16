package edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * GF2K-DPPRF发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
class Gf2kDpprfSenderThread extends Thread {
    /**
     * 发送方
     */
    private final Gf2kDpprfSender sender;
    /**
     * 关联值Δ
     */
    private final byte[] delta;
    /**
     * 批处理数量
     */
    private final int batchNum;
    /**
     * α上界
     */
    private final int alphaBound;
    /**
     * 预计算发送方输出
     */
    private final CotSenderOutput preSenderOutput;
    /**
     * 输出
     */
    private Gf2kDpprfSenderOutput senderOutput;

    Gf2kDpprfSenderThread(Gf2kDpprfSender sender, byte[] delta, int batchNum, int alphaBound) {
        this(sender, delta, batchNum, alphaBound, null);
    }

    Gf2kDpprfSenderThread(Gf2kDpprfSender sender, byte[] delta, int batchNum, int alphaBound, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.delta = delta;
        this.batchNum = batchNum;
        this.alphaBound = alphaBound;
        this.preSenderOutput = preSenderOutput;
    }

    Gf2kDpprfSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(delta, batchNum, alphaBound);
            senderOutput = preSenderOutput == null ? sender.puncture(batchNum, alphaBound)
                : sender.puncture(batchNum, alphaBound, preSenderOutput);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
