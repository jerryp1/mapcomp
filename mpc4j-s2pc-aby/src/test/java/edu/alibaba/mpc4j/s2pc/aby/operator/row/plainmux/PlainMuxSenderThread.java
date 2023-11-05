package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainmux;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Plain mux thread.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
class PlainMuxSenderThread extends Thread {
    /**
     * the sender
     */
    private final PlainMuxParty sender;
    /**
     * x0
     */
    private final SquareZ2Vector x0;
    /**
     * y
     */
    private final long[] y;
    /**
     * the num
     */
    private final int num;
    /**
     * z0
     */
    private SquareZlVector z0;

    PlainMuxSenderThread(PlainMuxParty sender, SquareZ2Vector x0, long[] y) {
        this.sender = sender;
        this.x0 = x0;
        num = x0.getNum();
        this.y = y;
    }

    SquareZlVector getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            sender.init(num);
            z0 = sender.mux(x0, y);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
