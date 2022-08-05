package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.rvole;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleSenderOutput;

/**
 * Z2-RVOLE协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
class Z2RvoleSenderThread extends Thread {
    /**
     * 发送方
     */
    private final Z2RvoleSender sender;
    /**
     * x
     */
    private final byte[] x;
    /**
     * 数量
     */
    private final int num;
    /**
     * 输出
     */
    private Z2VoleSenderOutput senderOutput;

    Z2RvoleSenderThread(Z2RvoleSender sender, byte[] x, int num) {
        this.sender = sender;
        this.x = x;
        this.num = num;
    }

    Z2VoleSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(num);
            senderOutput = sender.send(x, num);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
