package edu.alibaba.mpc4j.s2pc.aby.operator.psorter;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

/**
 * Bit2a sender thread.
 *
 * @author Li Peng
 * @date 2023/10/12
 */
class PermutableSorterSenderThread extends Thread {
    /**
     * the sender
     */
    private final PermutableSorterParty sender;
    /**
     * x0
     */
    private final SquareZ2Vector[][] x0;
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
     * z0
     */
    private SquareZlVector[] z0;

    PermutableSorterSenderThread(PermutableSorterParty sender, SquareZ2Vector[][] x0, int l) {
        this.sender = sender;
        this.x0 = x0;
        num = x0[0][0].getNum();
        this.l = l;
        this.numSorted = x0.length;
    }

    SquareZlVector[] getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            sender.init(l, l * num * numSorted);
            stopWatch.stop();
            long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            System.out.println("### init: " + initTime + " ms.");
            z0 = sender.sort(x0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
