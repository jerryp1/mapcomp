package edu.alibaba.mpc4j.s2pc.opf.permutation;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Permutation receiver thread.
 *
 */
class PermutationReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final PermutationReceiver receiver;
    /**
     * x1
     */
    private final SquareZlVector perm1;
    /**
     * the num
     */
    private final int num;
    /**
     * l
     */
    private final int l;
    /**
     * z1
     */
    private SquareZlVector z1;

    PermutationReceiverThread(PermutationReceiver receiver, SquareZlVector perm1) {
        this.receiver = receiver;
        this.perm1 = perm1;
        this.num = perm1.getNum();
        this.l = perm1.getZl().getL();
    }

    SquareZlVector getZ1() {
        return z1;
    }

    @Override
    public void run() {
        try {
            receiver.init(l, num);
            z1 = receiver.permute(perm1);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
