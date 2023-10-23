package edu.alibaba.mpc4j.s2pc.aby.basics.b2a;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * B2a sender thread.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
class B2aSenderThread extends Thread {
    /**
     * the sender
     */
    private final B2aParty sender;
    /**
     * x0
     */
    private final SquareZ2Vector[] x0;
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

    B2aSenderThread(B2aParty sender, SquareZ2Vector[] x0, int l) {
        this.sender = sender;
        this.x0 = x0;
        num = x0[0].getNum();
        this.l = l;
    }

    SquareZlVector getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            sender.init(l, num);
            z0 = sender.b2a(x0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
