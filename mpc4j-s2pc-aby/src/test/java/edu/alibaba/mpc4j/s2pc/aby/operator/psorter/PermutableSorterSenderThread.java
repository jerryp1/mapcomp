package edu.alibaba.mpc4j.s2pc.aby.operator.psorter;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

/**
 * Permutable sorter sender thread.
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
    private final SquareZ2Vector[] x0;
    /**
     * the num
     */
    private final int num;
    /**
     * l
     */
    private final int l;
    /**
     * z0
     */
    private SquareZlVector z0;

    PermutableSorterSenderThread(PermutableSorterParty sender, SquareZ2Vector[] x0, int l) {
        this.sender = sender;
        this.x0 = x0;
        num = x0[0].getNum();
        this.l = l;
    }

    SquareZlVector getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            sender.init(l, num);
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
