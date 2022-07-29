package edu.alibaba.mpc4j.common.tool.polynomial.gf2x;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * GF2X多项式性能测试。
 *
 * @author Weiran Liu
 * @date 2022/7/27
 */
@Ignore
public class Gf2xPolyEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gf2xPolyEfficiencyTest.class);
    /**
     * log(n)
     */
    private static final int logN = 4;
    /**
     * 点数量输出格式
     */
    private static final DecimalFormat POINT_NUM_DECIMAL_FORMAT = new DecimalFormat("00");
    /**
     * 时间输出格式
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.00");
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 秒表
     */
    private static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * 测试类型
     */
    private static final Gf2xPolyFactory.Gf2xPolyType[] TYPES = new Gf2xPolyFactory.Gf2xPolyType[] {
        Gf2xPolyFactory.Gf2xPolyType.NTL,
        Gf2xPolyFactory.Gf2xPolyType.RINGS_NEWTON,
        Gf2xPolyFactory.Gf2xPolyType.RINGS_LAGRANGE,
    };

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
            "                type", "  # points", "    log(n)",
            "  Full(ms)", " rFull(ms)", "  Half(ms)", "  rHalf(ms)",
            " Eval.(ms)", "bEval.(ms)"
        );
        testEfficiency(10);
        testEfficiency(20);
        testEfficiency(30);
        testEfficiency(40);
        testEfficiency(50);
    }

    private void testEfficiency(int pointNum) {
        int n = 1 << logN;
        for (Gf2xPolyFactory.Gf2xPolyType type : TYPES) {
            Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
            // 创建全量插值点
            byte[][] xFullArray = IntStream.range(0, pointNum)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL()];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            byte[][] yFullArray = IntStream.range(0, pointNum)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL()];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            byte[] yFullBytes = new byte[gf2xPoly.getByteL()];
            SECURE_RANDOM.nextBytes(yFullBytes);
            // 创建半数插值点
            byte[][] xHalfArray = IntStream.range(0, pointNum / 2)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL()];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            byte[][] yHalfArray = IntStream.range(0, pointNum / 2)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL()];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            byte[] yHalfBytes = new byte[gf2xPoly.getByteL()];
            SECURE_RANDOM.nextBytes(yHalfBytes);
            // 全量插值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2xPoly.interpolate(pointNum, xFullArray, yFullArray));
            STOP_WATCH.stop();
            double fullInterpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 全量根差值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2xPoly.rootInterpolate(pointNum, xFullArray, yFullBytes));
            STOP_WATCH.stop();
            double fullRootInterpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 半量插值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2xPoly.interpolate(pointNum, xHalfArray, yHalfArray));
            STOP_WATCH.stop();
            double halfInterpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 半量根差值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2xPoly.rootInterpolate(pointNum, xHalfArray, yHalfBytes));
            STOP_WATCH.stop();
            double halfRootInterpolateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 单一求值时间
            byte[][] coefficients = gf2xPoly.rootInterpolate(pointNum, xFullArray, yFullBytes);
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index ->
                Arrays.stream(xFullArray).forEach(x -> gf2xPoly.evaluate(coefficients, x))
            );
            STOP_WATCH.stop();
            double singleEvaluateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();
            // 批量求值时间
            STOP_WATCH.start();
            IntStream.range(0, n).forEach(index -> gf2xPoly.evaluate(coefficients, xFullArray));
            double multiEvaluateTime = (double) STOP_WATCH.getTime(TimeUnit.MICROSECONDS) / n / 1000;
            STOP_WATCH.reset();

            LOGGER.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
                StringUtils.leftPad(type.name(), 20),
                StringUtils.leftPad(POINT_NUM_DECIMAL_FORMAT.format(pointNum), 10),
                StringUtils.leftPad(String.valueOf(logN), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(fullInterpolateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(fullRootInterpolateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(halfInterpolateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(halfRootInterpolateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(singleEvaluateTime), 10),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(multiEvaluateTime), 10)
            );
        }
        LOGGER.info(StringUtils.rightPad("", 60, '-'));
    }
}
