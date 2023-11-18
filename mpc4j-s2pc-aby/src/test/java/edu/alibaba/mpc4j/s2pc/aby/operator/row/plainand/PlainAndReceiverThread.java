package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Plain and thread.
 *
 * @author Li Peng
 * @date 2023/11/8
 */
class PlainAndReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final PlainAndParty receiver;
    /**
     * x1
     */
    private final BitVector x1;
    /**
     * the num
     */
    private final int num;
    /**
     * z1
     */
    private SquareZ2Vector z1;

    PlainAndReceiverThread(PlainAndParty receiver, BitVector x1) {
        this.receiver = receiver;
        this.x1 = x1;
        num = x1.bitNum();
    }

    SquareZ2Vector getZ1() {
        return z1;
    }

    @Override
    public void run() {
        try {
            receiver.init(num);
            z1 = receiver.and(x1);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
