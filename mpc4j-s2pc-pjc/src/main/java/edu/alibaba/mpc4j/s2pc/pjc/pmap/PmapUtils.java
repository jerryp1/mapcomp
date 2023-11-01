package edu.alibaba.mpc4j.s2pc.pjc.pmap;

import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

public class PmapUtils {
    public static long resetAndGetTime(StopWatch stopWatch){
        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        return time;
    }

    public static SquareZ2Vector[] switchData(SquareZ2Vector[] data, int[] pai){
        // todo
        return null;
    }
}
