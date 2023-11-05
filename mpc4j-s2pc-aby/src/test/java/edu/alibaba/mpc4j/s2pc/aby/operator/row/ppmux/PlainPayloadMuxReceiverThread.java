package edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Plain mux thread.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
class PlainPayloadMuxReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final PlainPayloadMuxParty receiver;
    /**
     * x1
     */
    private final SquareZ2Vector x1;
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

    PlainPayloadMuxReceiverThread(PlainPayloadMuxParty receiver, SquareZ2Vector x1) {
        this.receiver = receiver;
        this.x1 = x1;
        num = x1.getNum();
    }

    SquareZlVector getZ1() {
        return z1;
    }

    @Override
    public void run() {
        try {
            receiver.init(num);
            z1 = receiver.mux(x1, null);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
