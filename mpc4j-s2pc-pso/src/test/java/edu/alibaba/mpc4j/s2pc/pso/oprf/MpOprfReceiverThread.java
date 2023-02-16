package edu.alibaba.mpc4j.s2pc.pso.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * MPOPRF协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/4/9
 */
class MpOprfReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final MpOprfReceiver receiver;
    /**
     * 输入
     */
    private final byte[][] inputs;
    /**
     * 输出
     */
    private MpOprfReceiverOutput receiverOutput;

    MpOprfReceiverThread(MpOprfReceiver receiver, byte[][] inputs) {
        this.receiver = receiver;
        this.inputs = inputs;
    }

    MpOprfReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(inputs.length);
            receiverOutput = receiver.oprf(inputs);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
