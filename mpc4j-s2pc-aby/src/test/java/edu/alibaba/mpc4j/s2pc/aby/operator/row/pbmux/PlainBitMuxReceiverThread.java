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
class PlainBitMuxReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final PlainBitMuxParty receiver;
    /**
     * x1
     */
    private final BitVector x1;
    /**
     * y1
     */
    private final SquareZlVector y1;
    /**
     * the num
     */
    private final int num;
    /**
     * z1
     */
    private SquareZlVector z1;
    /**
     * y
     */
    private long[] y;

    PlainBitMuxReceiverThread(PlainBitMuxParty receiver, BitVector x1, SquareZlVector y1) {
        this.receiver = receiver;
        this.x1 = x1;
        num = x1.bitNum();
        this.y1 = y1;
    }

    SquareZlVector getZ1() {
        return z1;
    }

    @Override
    public void run() {
        try {
            receiver.init(num);
            z1 = receiver.mux(x1, y1);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
