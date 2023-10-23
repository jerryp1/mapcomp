package edu.alibaba.mpc4j.s2pc.opf.permutation;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Permutation sender thread.
 *
 * @author Li Peng
 * @date 2023/10/12
 */
class PermutationSenderThread extends Thread {
    /**
     * the sender
     */
    private final PermutationSender sender;
    /**
     * x0
     */
    private final SquareZlVector perm0;
    /**
     * x
     */
    private final ZlVector x;
    /**
     * the num
     */
    private final int num;
    /**
     * l
     */
    private final int l;
    /**
     * z0
     */
    private SquareZlVector z0;

    PermutationSenderThread(PermutationSender sender, SquareZlVector perm0, ZlVector x) {
        this.sender = sender;
        this.perm0 = perm0;
        this.num = perm0.getNum();
        this.l = x.getZl().getL();
        this.x = x;
    }

    SquareZlVector getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            sender.init(l, num);
            z0 = sender.permute(perm0, x);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
