package edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.BitOtSenderOutput;

/**
 * NC-BitOt接收方线程。
 *
 * @author Hanwen Feng
 * @date 2022/08/12
 */
class NcBitOtSenderThread extends Thread{
    /**
     * 发送方
     */
    private final NcBitOtSender sender;
    /**
     * 数量
     */
    private final int num;
    /**
     * 输出
     */
    private final BitOtSenderOutput[] senderOutputs;

    NcBitOtSenderThread(NcBitOtSender sender, int num, int round) {
        this.sender = sender;
        this.num = num;
        senderOutputs = new BitOtSenderOutput[round];
    }

    BitOtSenderOutput[] getSenderOutputs() {
        return senderOutputs;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(num);
            for (int round = 0; round < senderOutputs.length; round++) {
                senderOutputs[round] = sender.send();
            }
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
