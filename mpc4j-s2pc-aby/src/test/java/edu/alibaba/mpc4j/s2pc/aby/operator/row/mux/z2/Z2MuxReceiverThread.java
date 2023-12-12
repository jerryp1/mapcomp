package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Z2 mux receiver thread.
 *
 * @author Feng Han
 * @date 2023/11/28
 */
public class Z2MuxReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final Z2MuxParty receiver;
    /**
     * x1
     */
    private final SquareZ2Vector shareX1;
    /**
     * y1
     */
    private final SquareZ2Vector[] shareY1;
    /**
     * the num
     */
    private final int num;
    /**
     * z1
     */
    private SquareZ2Vector[] shareZ1;

    Z2MuxReceiverThread(Z2MuxParty receiver, SquareZ2Vector shareX1, SquareZ2Vector[] shareY1) {
        this.receiver = receiver;
        this.shareX1 = shareX1;
        this.shareY1 = shareY1;
        num = shareX1.getNum();
    }

    SquareZ2Vector[] getShareZ1() {
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
