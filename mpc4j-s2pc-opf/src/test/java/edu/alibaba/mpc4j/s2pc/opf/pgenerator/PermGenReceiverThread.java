package edu.alibaba.mpc4j.s2pc.opf.pgenerator;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Permutable sorter receiver thread.
 *
 */
class PermGenReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final PermGenParty receiver;
    /**
     * x1
     */
    private final SquareZ2Vector[] x1;
    /**
     * the num
     */
    private final int num;
    /**
     * l
     */
    private final int k;
    /**
     * z1
     */
    private SquareZlVector z1;

    PermGenReceiverThread(PermGenParty receiver, SquareZ2Vector[] x1, int k) {
        this.receiver = receiver;
        this.x1 = x1;
        num = x1[0].getNum();
        this.k = k;
    }

    SquareZlVector getZ1() {
        return z1;
    }

    @Override
    public void run() {
        try {
            receiver.init(num, k);
            z1 = receiver.sort(x1);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
