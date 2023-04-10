package edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.SquareShareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

/**
 * Zl mux receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
class ZlMuxReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final ZlMuxParty receiver;
    /**
     * x1
     */
    private final SquareShareZ2Vector shareX1;
    /**
     * y1
     */
    private final SquareShareZlVector shareY1;
    /**
     * the num
     */
    private final int num;
    /**
     * z1
     */
    private SquareShareZlVector shareZ1;

    ZlMuxReceiverThread(ZlMuxParty receiver, SquareShareZ2Vector shareX1, SquareShareZlVector shareY1) {
        this.receiver = receiver;
        this.shareX1 = shareX1;
        this.shareY1 = shareY1;
        num = shareX1.getNum();
    }

    SquareShareZlVector getShareZ1() {
        return shareZ1;
    }

    @Override
    public void run() {
        try {
            receiver.init(num);
            shareZ1 = receiver.mux(shareX1, shareY1);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
