package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class SqOprfReceiverThread  extends Thread {
    /**
     * 接收方
     */
    private final SqOprfReceiver receiver;
    /**
     * 输入
     */
    private final byte[][] inputs;
    /**
     * 输出
     */
    private SqOprfReceiverOutput receiverOutput;

    SqOprfReceiverThread(SqOprfReceiver receiver, byte[][] inputs) {
        this.receiver = receiver;
        this.inputs = inputs;
    }

    SqOprfReceiverOutput getReceiverOutput() {
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
