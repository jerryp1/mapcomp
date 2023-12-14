package edu.alibaba.mpc4j.s2pc.opf.pgenerator;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Permutable sorter sender thread.
 *
 * @author Li Peng
 * @date 2023/10/12
 */
class PermGenSenderThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermGenSenderThread.class);
    /**
     * the sender
     */
    private final PermGenParty sender;
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
    private final int k;
    /**
     * z0
     */
    private SquareZlVector z0;

    PermGenSenderThread(PermGenParty sender, SquareZ2Vector[] x0, int k) {
        this.sender = sender;
        this.x0 = x0;
        num = x0[0].getNum();
        this.k = k;
    }

    SquareZlVector getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            sender.init(num, k);
            stopWatch.stop();
            long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            LOGGER.info("### init: " + initTime + " ms.");
            z0 = sender.sort(x0);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
