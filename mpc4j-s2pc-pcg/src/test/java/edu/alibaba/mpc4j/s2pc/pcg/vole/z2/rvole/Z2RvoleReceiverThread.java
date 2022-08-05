package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.rvole;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleReceiverOutput;

/**
 * Z2-RVOLE协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
class Z2RvoleReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final Z2RvoleReceiver receiver;
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

    Z2RvoleReceiverThread(Z2RvoleReceiver receiver, boolean delta, int num) {
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
