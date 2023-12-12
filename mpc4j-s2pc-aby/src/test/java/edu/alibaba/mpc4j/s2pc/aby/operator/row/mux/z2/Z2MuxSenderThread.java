package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Z2 mux sender thread.
 *
 * @author Feng Han
 * @date 2023/11/28
 */
public class Z2MuxSenderThread extends Thread {
    /**
     * the sender
     */
    private final Z2MuxParty sender;
    /**
     * x0
     */
    private final SquareZ2Vector x0;
    /**
     * y0
     */
    private final SquareZ2Vector[] y0;
    /**
     * the num
     */
    private final int num;
    /**
     * z0
     */
    private SquareZ2Vector[] shareZ0;

    Z2MuxSenderThread(Z2MuxParty sender, SquareZ2Vector shareX0, SquareZ2Vector[] shareY0) {
        this.sender = sender;
        this.x0 = shareX0;
        this.y0 = shareY0;
        num = shareX0.getNum();
    }

    SquareZ2Vector[] getShareZ0() {
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
