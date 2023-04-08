package edu.alibaba.mpc4j.s2pc.opf.opprf.rbopprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rbopprf.RbopprfReceiver;

/**
 * Related-Batch OPPRF receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
class RbopprfReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final RbopprfReceiver receiver;
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
    private byte[][][] receiverTargetArray;

    RbopprfReceiverThread(RbopprfReceiver receiver, int l, byte[][] receiverInputArray, int pointNum) {
        this.receiver = receiver;
        this.l = l;
        this.receiverInputArray = receiverInputArray;
        this.pointNum = pointNum;
    }

    byte[][][] getReceiverTargetArray() {
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