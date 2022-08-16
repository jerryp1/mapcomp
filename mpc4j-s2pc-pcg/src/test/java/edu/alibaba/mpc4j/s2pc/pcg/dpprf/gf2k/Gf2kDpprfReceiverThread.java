package edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * GF2K-DPPRF接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
class Gf2kDpprfReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final Gf2kDpprfReceiver receiver;
    /**
     * α数组
     */
    private final int[] alphaArray;
    /**
     * α上界
     */
    private final int alphaBound;
    /**
     * 预计算接收方输出
     */
    private final CotReceiverOutput preReceiverOutput;
    /**
     * 输出
     */
    private Gf2kDpprfReceiverOutput receiverOutput;

    Gf2kDpprfReceiverThread(Gf2kDpprfReceiver receiver, int[] alphaArray, int alphaBound) {
        this(receiver, alphaArray, alphaBound, null);
    }

    Gf2kDpprfReceiverThread(Gf2kDpprfReceiver receiver, int[] alphaArray, int alphaBound, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.alphaArray = alphaArray;
        this.alphaBound = alphaBound;
        this.preReceiverOutput = preReceiverOutput;
    }

    Gf2kDpprfReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(alphaArray.length, alphaBound);
            receiverOutput = preReceiverOutput == null ? receiver.puncture(alphaArray, alphaBound)
                : receiver.puncture(alphaArray, alphaBound, preReceiverOutput);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
