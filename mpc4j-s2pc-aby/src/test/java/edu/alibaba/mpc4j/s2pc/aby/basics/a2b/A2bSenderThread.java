package edu.alibaba.mpc4j.s2pc.aby.basics.a2b;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * A2b sender thread.
 *
 * @author Li Peng
 * @date 2023/10/20
 */
class A2bSenderThread extends Thread {
    /**
     * the sender
     */
    private final A2bParty sender;
    /**
     * x0
     */
    private final SquareZlVector x0;
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
    private SquareZ2Vector[] z0;

    A2bSenderThread(A2bParty sender, SquareZlVector x0, int l) {
        this.sender = sender;
        this.x0 = x0;
        num = x0.getNum();
        this.l = l;
    }

    SquareZ2Vector[] getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            sender.init(l, num);
            z0 = sender.a2b(x0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
