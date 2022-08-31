package edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.BitOtReceiverOutput;

/**
 * NC-BitOt协议接收方线程。
 *
 * @author Hanwen Feng
 * @date 2022/08/12
 */
class NcBitOtReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final NcBitOtReceiver receiver;
    /**
     * 数量
     */
    private final int num;
    /**
     * 输出
     */
    private final BitOtReceiverOutput[] receiverOutputs;

    NcBitOtReceiverThread(NcBitOtReceiver receiver, int num, int round) {
        this.receiver = receiver;
        this.num = num;
        receiverOutputs = new BitOtReceiverOutput[round];
    }

    BitOtReceiverOutput[] getReceiverOutputs() {
        return receiverOutputs;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(num);
            for (int round = 0; round < receiverOutputs.length; round++) {
                receiverOutputs[round] = receiver.receive();
            }
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
