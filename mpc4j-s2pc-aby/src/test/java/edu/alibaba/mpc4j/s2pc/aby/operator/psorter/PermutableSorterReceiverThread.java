package edu.alibaba.mpc4j.s2pc.aby.operator.psorter;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Bit2a receiver thread.
 *
 * @author Li Peng
 * @date 2023/10/12
 */
class PermutableSorterReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final PermutableSorterParty receiver;
    /**
     * x1
     */
    private final SquareZ2Vector[][] x1;
    /**
     * the num
     */
    private final int num;
    /**
     * the num of sorted elements
     */
    private final int numSorted;
    /**
     * l
     */
    private final int l;
    /**
     * z1
     */
    private SquareZlVector[] z1;

    PermutableSorterReceiverThread(PermutableSorterParty receiver, SquareZ2Vector[][] x1, int l) {
        this.receiver = receiver;
        this.x1 = x1;
        num = x1[0][0].getNum();
        this.l = l;
        this.numSorted = x1.length;
    }

    SquareZlVector[] getZ1() {
        return z1;
    }

    @Override
    public void run() {
        try {
            receiver.init(l,  num * numSorted);
            z1 = receiver.sort(x1);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
