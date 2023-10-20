package edu.alibaba.mpc4j.s2pc.aby.basics.b2a;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * B2a receiver thread.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
class B2aReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final B2aParty receiver;
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
    private final int l;
    /**
     * z1
     */
    private SquareZlVector z1;

    B2aReceiverThread(B2aParty receiver, SquareZ2Vector[] x1, int l) {
        this.receiver = receiver;
        this.x1 = x1;
        num = x1[0].getNum();
        this.l = l;
    }

    SquareZlVector getZ1() {
        return z1;
    }

    @Override
    public void run() {
        try {
            receiver.init(l, num);
            z1 = receiver.b2a(x1);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
