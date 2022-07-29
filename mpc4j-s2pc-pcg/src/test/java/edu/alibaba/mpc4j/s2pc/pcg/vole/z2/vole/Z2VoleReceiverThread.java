package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.vole;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Z2-VOLE协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
class Z2VoleReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final Z2VoleReceiver receiver;
    /**
     * 关联值Δ
     */
    private final boolean delta;
    /**
     * 数量
     */
    private final int num;
    /**
     * 输出
     */
    private Z2VoleReceiverOutput receiverOutput;

    Z2VoleReceiverThread(Z2VoleReceiver receiver, boolean delta, int num) {
        this.receiver = receiver;
        this.delta = delta;
        this.num = num;
    }

    Z2VoleReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(delta, num);
            receiverOutput = receiver.receive(num);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
