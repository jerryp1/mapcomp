package edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.SquareShareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

/**
 * Zl mux sender thread.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
class ZlMuxSenderThread extends Thread {
    /**
     * the sender
     */
    private final ZlMuxParty sender;
    /**
     * x0
     */
    private final SquareShareZ2Vector x0;
    /**
     * y0
     */
    private final SquareShareZlVector y0;
    /**
     * the num
     */
    private final int num;
    /**
     * z0
     */
    private SquareShareZlVector shareZ0;

    ZlMuxSenderThread(ZlMuxParty sender, SquareShareZ2Vector shareX0, SquareShareZlVector shareY0) {
        this.sender = sender;
        this.x0 = shareX0;
        this.y0 = shareY0;
        num = shareX0.getNum();
    }

    SquareShareZlVector getShareZ0() {
        return shareZ0;
    }

    @Override
    public void run() {
        try {
            sender.init(num);
            shareZ0 = sender.mux(x0, y0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
