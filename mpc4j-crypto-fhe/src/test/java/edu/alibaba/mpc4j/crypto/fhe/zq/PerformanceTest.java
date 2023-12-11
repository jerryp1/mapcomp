package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallMod;
import edu.alibaba.mpc4j.crypto.fhe.rq.PolyArithmeticSmallModEfficiencyTest;
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
 * @date 2023/9/24
 */
public class PerformanceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolyArithmeticSmallModEfficiencyTest.class);

    /**
     * max loop num
     */
    private static final int MAX_LOOP_NUM = 10000 * 1000;

    /**
     * time format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * the stop watch
     */
    private static final StopWatch STOP_WATCH = new StopWatch();


    @Test
    public void testUintArithmetic() {
        LOGGER.info("{}\t{}",
                "                   name", "          time(us)"
        );

        testUintArithmeticEfficiency();
    }


    private void testUintArithmeticEfficiency() {


        long a = 0xF00F00F00F00F00FL;
        long b = 0x0FF0FF0FF0FF0FF0L;
        long[] result = new long[1];
        double time;

        // warm up
        long[] finalResult1 = result;
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.addUint64Generic(a, b, 0, finalResult1));

        long[] finalResult2 = result;
        STOP_WATCH.start();

        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.addUint64Generic(a, b, 0, finalResult2));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("addUint64Generic", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );
        long[] finalResult3 = result;
        STOP_WATCH.start();

        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.addUint64(a, b, 0, finalResult3));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("addUint64", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );
        long[] finalResult4 = result;
        STOP_WATCH.start();

        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.subUint64Generic(a, b, 0, finalResult4));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("subUint64Generic", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );
        long[] finalResult5 = result;
        STOP_WATCH.start();

        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.subUint64(a, b, 0, finalResult5));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("subUint64", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );

        long[] as = new long[]{0xF00F00F00F00F00FL, 0x0FF0FF0FF0FF0FF0L};
        long[] bs = new long[]{0xF00F00F00F00F00FL, 0x0FF0FF0FF0FF0FF0L};
        long[] result128 = new long[2];


        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.addUint128(as, bs, result128));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("addUint128", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );


        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.addUint(as, bs, 2, result128));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("addUint", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );

        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.subUint(as, bs, 2, result128));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("subUint", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );

        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.addUint(as, 2, 0xFFFFFFFFL, result128));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("addUintUint64", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );


        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.subUint(as, 2, 0xFFFFFFFFL, result128));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("subUintUint64", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );


        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.incrementUint(as, 2, result128));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("incrementUint", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );


        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.decrementUint(as, 2, result128));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("decrementUint", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );

        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.negateUint(as, 2, result128));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("negateUint", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );


        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.leftShiftUint(as, 65, 2, result128));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("leftShiftUint", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );

        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.leftShiftUint128(as, 65, result128));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("leftShiftUint128", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );


        {

            long[] ptr = new long[3];
            long[] ptr2 = new long[3];
            ptr[0] = 0;
            ptr[1] = 0;
            ptr[2] = 0;
            ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
            ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
            ptr2[2] = 0xFFFFFFFFFFFFFFFFL;

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.leftShiftUint192(ptr, 65, ptr2));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("leftShiftUint192", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }

        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.rightShiftUint(as, 65, 2, result128));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("rightShiftUint", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );

        STOP_WATCH.start();
        IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.rightShiftUint128(as, 65, result128));
        STOP_WATCH.stop();
        time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
        STOP_WATCH.reset();

        // output
        LOGGER.info(
                "{}\t{}",
                StringUtils.leftPad("rightShiftUint128", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
        );


        {

            long[] ptr = new long[3];
            long[] ptr2 = new long[3];
            ptr[0] = 0;
            ptr[1] = 0;
            ptr[2] = 0;
            ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
            ptr2[1] = 0xFFFFFFFFFFFFFFFFL;
            ptr2[2] = 0xFFFFFFFFFFFFFFFFL;

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.rightShiftUint192(ptr, 65, ptr2));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("rightShiftUint192", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }

        {
            long[] ptr = new long[2];
            long[] ptr2 = new long[2];
            ptr[0] = 0;
            ptr[1] = 0;
            ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
            ptr2[1] = 0xFFFFFFFFFFFFFFFFL;

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.halfRoundUpUint(ptr, 2, ptr2));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("halfRoundUpUint", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );


        }
        {
            long[] ptr = new long[2];
            long[] ptr2 = new long[2];
            ptr[0] = 0;
            ptr[1] = 0;
            ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
            ptr2[1] = 0xFFFFFFFFFFFFFFFFL;

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.notUint(ptr, 2, ptr2));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("notUint", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }

        {
            long[] ptr = new long[2];
            long[] ptr2 = new long[2];
            ptr[0] = 0;
            ptr[1] = 0;
            ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
            ptr2[1] = 0xFFFFFFFFFFFFFFFFL;

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.andUint(ptr, ptr2, 2, ptr2));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("andUint", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }
        {
            long[] ptr = new long[2];
            long[] ptr2 = new long[2];
            ptr[0] = 0;
            ptr[1] = 0;
            ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
            ptr2[1] = 0xFFFFFFFFFFFFFFFFL;

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.orUint(ptr, ptr2, 2, ptr2));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("orUint", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }

        {
            long[] ptr = new long[2];
            long[] ptr2 = new long[2];
            ptr[0] = 0;
            ptr[1] = 0;
            ptr2[0] = 0xFFFFFFFFFFFFFFFFL;
            ptr2[1] = 0xFFFFFFFFFFFFFFFFL;

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.xorUint(ptr, ptr2, 2, ptr2));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("xorUint", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }

        {

            result = new long[2];
            STOP_WATCH.start();
            long[] finalResult6 = result;
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.multiplyUint64Generic(0x100000000L, 0xFAFABABAL, finalResult6));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("multiplyUint64Generic", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }
        {
            result = new long[2];
            STOP_WATCH.start();
            long[] finalResult = result;
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.multiplyUint64(0x100000000L, 0xFAFABABAL, finalResult));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("multiplyUint64", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }
        {

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.multiplyUint64Hw64Generic(0x100000000L, 0xFAFABABAL));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("multiplyUint64Hw64Generic", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }

        {

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.multiplyUint64Hw64(0x100000000L, 0xFAFABABAL));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("multiplyUint64Hw64", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }

        {

            long[] in = new long[]{0xF0F0F0F0F0F0F0L, 0xBABABABABABABAL, 0xCECECECECECECEL};
            long[] out = new long[]{0, 0, 0};

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.multiplyManyUint64(in, 3, out));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("multiplyManyUint64", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }
        {

            long[] in = new long[]{0xF0F0F0F0F0F0F0L, 0xBABABABABABABAL, 0xCECECECECECECEL};
            long[] out = new long[]{0, 0, 0};

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.multiplyManyUint64Except(in, 3, 1, out));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("multiplyManyUint64Except", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }


        {

            long[] in = new long[]{0xF0F0F0F0F0F0F0L, 0xBABABABABABABAL, 0xCECECECECECECEL};
            long[] out = new long[]{0, 0, 0};
            long[] exp = new long[6];
            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.multiplyUint(in, out, 3, exp));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("multiplyUint", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }

        {

            long[] in = new long[]{0xF0F0F0F0F0F0F0L, 0xBABABABABABABAL, 0xCECECECECECECEL};
            long[] out = new long[]{0, 0, 0};
            long[] exp = new long[3];


            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.multiplyUint(in, 3, 0, 3, exp));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("multiplyUintUint64", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }

        {

            long[] ptr = new long[2];
            long[] ptr2 = new long[]{0, 1};
            long[] ptr3 = new long[2];
            long[] ptr4 = new long[2];

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.divideUint(ptr, ptr2, 2, ptr3, ptr4));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("divideUint", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }

        {

            long[] ptr = new long[2];
            long[] ptr2 = new long[]{0, 1};
            long[] ptr3 = new long[2];
            long[] ptr4 = new long[2];

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.divideUint128Inplace(ptr, 1, ptr3));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("divideUint128Inplace", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );

        }

        {

            long[] ptr = new long[3];
            long[] ptr3 = new long[3];
            long[] ptr4 = new long[2];

            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.divideUint192Inplace(ptr, 1, ptr3));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("divideUint192Inplace", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );
        }

        {
            STOP_WATCH.start();
            IntStream.range(0, MAX_LOOP_NUM).forEach(i -> UintArithmetic.exponentUint(123456789L, 3));
            STOP_WATCH.stop();
            time = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / MAX_LOOP_NUM;
            STOP_WATCH.reset();

            // output
            LOGGER.info(
                    "{}\t{}",
                    StringUtils.leftPad("exponentUint", 20),
                    StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(time), 12)
            );
        }
    }

}
