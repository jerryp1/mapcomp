package edu.alibaba.mpc4j.s2pc.aby.basics.a2b;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * A2b receiver thread.
 *
 * @author Li Peng
 * @date 2023/10/20
 */
class A2bReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final A2bParty receiver;
    /**
     * x1
     */
    private final SquareZlVector x1;
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
    private SquareZ2Vector[] z1;

    A2bReceiverThread(A2bParty receiver, SquareZlVector x1, int l) {
        this.receiver = receiver;
        this.x1 = x1;
        num = x1.getNum();
        this.l = l;
    }

    SquareZ2Vector[] getZ1() {
        return z1;
    }

    @Override
    public void run() {
        try {
            receiver.init(l, num);
            z1 = receiver.a2b(x1);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
