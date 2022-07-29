package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * RCOT协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2021/01/26
 */
class RcotReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final RcotReceiver receiver;
    /**
     * 选择比特
     */
    private final boolean[] choices;
    /**
     * 输出
     */
    private CotReceiverOutput receiverOutput;

    RcotReceiverThread(RcotReceiver receiver, boolean[] choices) {
        this.receiver = receiver;
        this.choices = choices;
    }

    CotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(choices.length);
            receiverOutput = receiver.receive(choices);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}