package edu.alibaba.mpc4j.crypto.fhe.rq;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/9/21
 */
public class PolyArithmeticSmallModEfficiencyTest {


    private static final Logger LOGGER = LoggerFactory.getLogger(PolyArithmeticSmallModEfficiencyTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * max loop num
     */
    private static final int MAX_LOOP_NUM = 2048;

    /**
     * Single modulus
     */
    private static final Modulus mod = new Modulus(0x3fffffff000001L);

    /**
     * time format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * the stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();


    @Test
    public void testPolyArithmeticSmallModEfficiency() {
        LOGGER.info("{}\t{}\t{}",
                "                name", "      coeffCount", "         time(us)"
        );

        int[] polyCoeffCounts = new int[]{1024, 2048, 4096, 8192, 16384, 32768};
        for (int coeffCount : polyCoeffCounts) {
            testEfficiency(coeffCount);

            LOGGER.info(StringUtils.rightPad("", 60, '-'));
        }
    }


    private void testEfficiency(int coeffCount) {

        long[] poly1 = new long[coeffCount];
        long[] poly2 = new long[coeffCount];
        long[] result = new long[coeffCount];
        // 生成随机数组
        IntStream.range(0, coeffCount).forEach(
                i -> {
                    poly1[i] = Math.abs(SECURE_RANDOM.nextLong()) % mod.getValue();
                    poly2[i] = Math.abs(SECURE_RANDOM.nextLong()) % mod.getValue();
                }
        );
        // warm up
//        IntStream.range(0, MAX_LOOP_NUM * 2).forEach(i -> PolyArithmeticSmallMod.dyadicProductCoeffMod(poly1, poly2, coeffCount, mod, result));


        // dyadicProductCoeffMod
        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> PolyArithmeticSmallMod.dyadicProductCoeffMod(poly1, poly2, coeffCount, mod, result));
        STOP_WATCH.stop();
        double dyadicProductCoeffModTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}\t{}",
                StringUtils.leftPad("dyadicProductCoeffMod", 25),
                StringUtils.leftPad(String.valueOf(coeffCount), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(dyadicProductCoeffModTime), 20)
        );

        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> PolyArithmeticSmallMod.addPolyCoeffMod(poly1, poly2, coeffCount, mod, result));
        STOP_WATCH.stop();
        double addPolyCoeffModTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}\t{}",
                StringUtils.leftPad("addPolyCoeffMod", 25),
                StringUtils.leftPad(String.valueOf(coeffCount), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(addPolyCoeffModTime), 20)
        );

        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> PolyArithmeticSmallMod.negatePolyCoeffMod(poly1, coeffCount, mod, result));
        STOP_WATCH.stop();
        double negatePolyCoeffModTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}\t{}",
                StringUtils.leftPad("negatePolyCoeffMod", 25),
                StringUtils.leftPad(String.valueOf(coeffCount), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(negatePolyCoeffModTime), 20)
        );
    }

}
