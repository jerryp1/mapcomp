package edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Plain mux thread.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
class PlainBitMuxSenderThread extends Thread {
    /**
     * the sender
     */
    private final PlainBitMuxParty sender;
    /**
     * x0
     */
    private final BitVector x0;
    /**
     * y
     */
    private final SquareZlVector y0;
    /**
     * the num
     */
    private final int num;
    /**
     * z0
     */
    private SquareZlVector z0;

    PlainBitMuxSenderThread(PlainBitMuxParty sender, BitVector x0, SquareZlVector y0) {
        this.sender = sender;
        this.x0 = x0;
        num = y0.getNum();
        this.y0 = y0;
    }

    SquareZlVector getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            sender.init(num);
            z0 = sender.mux(null, y0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
