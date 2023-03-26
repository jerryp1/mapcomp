package edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Batched l-bit-input OPPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
class BlopprfReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final BlopprfReceiver receiver;
    /**
     * the input / output bit length
     */
    private final int l;
    /**
     * the batched receiver input array
     */
    private final byte[][] receiverInputArray;
    /**
     * point num
     */
    private final int pointNum;
    /**
     * the PRF outputs
     */
    private byte[][] receiverTargetArray;

    BlopprfReceiverThread(BlopprfReceiver receiver, int l, byte[][] receiverInputArray, int pointNum) {
        this.receiver = receiver;
        this.l = l;
        this.receiverInputArray = receiverInputArray;
        this.pointNum = pointNum;
    }

    byte[][] getReceiverTargetArray() {
        return receiverTargetArray;
    }

    @Override
    public void run() {
        try {
            receiver.init(receiverInputArray.length, pointNum);
            receiverTargetArray = receiver.opprf(l, receiverInputArray, pointNum);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}