package edu.alibaba.mpc4j.s2pc.opf.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;

/**
 * OPRF协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2019/07/12
 */
class OprfReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final OprfReceiver receiver;
    /**
     * 输入
     */
    private final byte[][] inputs;
    /**
     * 输出
     */
    private OprfReceiverOutput receiverOutput;

    OprfReceiverThread(OprfReceiver receiver, byte[][] inputs) {
        this.receiver = receiver;
        this.inputs = inputs;
    }

    OprfReceiverOutput getReceiverOutput() {
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