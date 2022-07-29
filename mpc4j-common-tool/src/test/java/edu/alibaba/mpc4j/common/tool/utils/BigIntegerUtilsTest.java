package edu.alibaba.mpc4j.common.tool.utils;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 大整数工具类测试。
 *
 * @author Weiran Liu
 * @date 2021/11/29
 */
public class BigIntegerUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BigIntegerUtils.class);
    /**
     * 随机测试轮数
     */
    private static final int RANDOM_ROUND = 100;
    /**
     * 上界
     */
    private static final long UPPER_BOUND = 100;
    /**
     * 默认随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testBigIntegerByteArray() {
        // 正数转换
        testBigIntegerByteArray(BigInteger.ONE, new byte[] {(byte)0x01,});
        testBigIntegerByteArray(BigInteger.valueOf(67), new byte[] {(byte)0x43,});
        testBigIntegerByteArray(BigInteger.valueOf(14915), new byte[] {(byte)0x3A, (byte)0x43,});
        testBigIntegerByteArray(
            BigIntegerUtils.INT_MAX_VALUE,
            new byte[] {(byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF,}
        );
        testBigIntegerByteArray(
            BigIntegerUtils.LONG_MAX_VALUE,
            new byte[] {(byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,}
        );
        // 0转换
        testBigIntegerByteArray(BigInteger.ZERO, new byte[] {(byte)0x00,});
        // 负数转换
        testBigIntegerByteArray(BigInteger.ONE.negate(), new byte[] {(byte)0xFF,});
        testBigIntegerByteArray(
            BigIntegerUtils.INT_MIN_VALUE,
            new byte[] {(byte)0x80, (byte)0x00, (byte)0x00, (byte)0x00,}
        );
        testBigIntegerByteArray(
            BigIntegerUtils.LONG_MIN_VALUE,
            new byte[] {(byte)0x80, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,}
        );
    }

    private void testBigIntegerByteArray(BigInteger bigInteger, byte[] byteArray) {
        byte[] convertByteArray = BigIntegerUtils.bigIntegerToByteArray(bigInteger);
        Assert.assertArrayEquals(byteArray, convertByteArray);
        BigInteger convertBigInteger = BigIntegerUtils.byteArrayToBigInteger(byteArray);
        Assert.assertEquals(bigInteger, convertBigInteger);
    }

    @Test
    public void testNonNegBigIntegerByteArray() {
        // 0转换
        testNonNegBigIntegerByteArray(BigInteger.ZERO, new byte[] {(byte)0x00,});
        testNonNegBigIntegerByteArray(BigInteger.ZERO, new byte[] {(byte)0x00, (byte)0x00,});
        // 正数转换
        testNonNegBigIntegerByteArray(BigInteger.ONE, new byte[] {(byte)0x01,});
        testNonNegBigIntegerByteArray(BigInteger.ONE, new byte[] {(byte)0x00, (byte)0x01,});
        // 正好需要符号位的正数转换
        testNonNegBigIntegerByteArray(BigInteger.valueOf(255), new byte[] {(byte)0xFF,});
        testNonNegBigIntegerByteArray(BigInteger.valueOf(255), new byte[] {(byte)0x00, (byte)0xFF,});
    }

    private void testNonNegBigIntegerByteArray(BigInteger nonNegBigInteger, byte[] byteArray) {
        byte[] convertByteArray = BigIntegerUtils.nonNegBigIntegerToByteArray(nonNegBigInteger, byteArray.length);
        Assert.assertArrayEquals(byteArray, convertByteArray);
        BigInteger convertNonNegBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(byteArray);
        Assert.assertEquals(nonNegBigInteger, convertNonNegBigInteger);
    }

    @Test
    public void testInvalidCombinatorial() {
        try {
            BigIntegerUtils.combinatorial(-1, 0);
            throw new IllegalStateException("ERROR: successfully compute C(-1, 0)");
        } catch (AssertionError ignored) {

        }

        try {
            BigIntegerUtils.combinatorial(10, -1);
            throw new IllegalStateException("ERROR: successfully compute C(10, -1)");
        } catch (AssertionError ignored) {

        }

        try {
            BigIntegerUtils.combinatorial(10, 11);
            throw new IllegalStateException("ERROR: successfully compute C(10, 11)");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testCombinatorial() {
        // C(1, 0) = 1
        testCombinatorial(1, 0, BigInteger.ONE);
        // C(1, 1) = 1
        testCombinatorial(1, 1, BigInteger.ONE);
        // C(10, 0) = 1
        testCombinatorial(10, 0, BigInteger.ONE);
        // C(10, 1) = 10
        testCombinatorial(10, 1, BigInteger.valueOf(10));
        // C(10, 9) = 10
        testCombinatorial(10, 9, BigInteger.valueOf(10));
        // C(10, 10) = 1
        testCombinatorial(10, 10, BigInteger.ONE);
        // C(10, 5) = 252
        testCombinatorial(10, 5, BigInteger.valueOf(252));
        // C(10, 3) = 120
        testCombinatorial(10, 3, BigInteger.valueOf(120));
        // C(10, 6) = 210
        testCombinatorial(10, 6, BigInteger.valueOf(210));
    }

    private void testCombinatorial(int n, int m, BigInteger truth) {
        BigInteger combinatorial = BigIntegerUtils.combinatorial(n, m);
        Assert.assertEquals(truth, combinatorial);
    }

    @Test
    public void testBigIntegerSignum() {
        // 1的验证结果
        Assert.assertTrue(BigIntegerUtils.positive(BigInteger.ONE));
        Assert.assertTrue(BigIntegerUtils.nonNegative(BigInteger.ONE));
        Assert.assertFalse(BigIntegerUtils.negative(BigInteger.ONE));
        Assert.assertFalse(BigIntegerUtils.nonPositive(BigInteger.ONE));
        // 0的验证结果
        Assert.assertTrue(BigIntegerUtils.nonNegative(BigInteger.ZERO));
        Assert.assertTrue(BigIntegerUtils.nonPositive(BigInteger.ZERO));
        // -1的验证结果
        Assert.assertTrue(BigIntegerUtils.negative(BigInteger.ONE.negate()));
        Assert.assertTrue(BigIntegerUtils.nonPositive(BigInteger.ONE.negate()));
        Assert.assertFalse(BigIntegerUtils.positive(BigInteger.ONE.negate()));
        Assert.assertFalse(BigIntegerUtils.nonNegative(BigInteger.ONE.negate()));
    }

    @Test
    public void testComparison() {
        // 1和-1、1的比较结果
        Assert.assertTrue(BigIntegerUtils.greater(BigInteger.ONE, BigInteger.ONE.negate()));
        Assert.assertTrue(BigIntegerUtils.greaterOrEqual(BigInteger.ONE, BigInteger.ONE.negate()));
        Assert.assertTrue(BigIntegerUtils.greaterOrEqual(BigInteger.ONE, BigInteger.ONE));
        // -1和1、-1的比较结果
        Assert.assertTrue(BigIntegerUtils.less(BigInteger.ONE.negate(), BigInteger.ONE));
        Assert.assertTrue(BigIntegerUtils.lessOrEqual(BigInteger.ONE.negate(), BigInteger.ONE));
        Assert.assertTrue(BigIntegerUtils.lessOrEqual(BigInteger.ONE.negate(), BigInteger.ONE.negate()));
        // -1和1的比较结果
        Assert.assertFalse(BigIntegerUtils.greater(BigInteger.ONE.negate(), BigInteger.ONE));
        Assert.assertFalse(BigIntegerUtils.greaterOrEqual(BigInteger.ONE.negate(), BigInteger.ONE));
        // 1和-1的比较结果
        Assert.assertFalse(BigIntegerUtils.less(BigInteger.ONE, BigInteger.ONE.negate()));
        Assert.assertFalse(BigIntegerUtils.lessOrEqual(BigInteger.ONE, BigInteger.ONE.negate()));
    }

    @Test
    public void testSqrt() {
        // 测试1的开根号
        Assert.assertEquals(BigInteger.ONE, BigIntegerUtils.sqrt(BigInteger.ONE));
        for (int round = 0; round < RANDOM_ROUND; round++) {
            // 随机取[1, 2^512)中的一个元素，求平方，并开根号
            BigInteger n = BigIntegerUtils.randomPositive(BigInteger.ONE.shiftLeft(512), SECURE_RANDOM);
            BigInteger nSquared = n.multiply(n);
            Assert.assertEquals(n, BigIntegerUtils.sqrt(nSquared));
        }
    }

    @Test
    public void testModPow() {
        LOGGER.info("-----测试modPow性能差距，模数比特长度 = {}比特-----", 1 << 10);
        StopWatch stopWatch = new StopWatch();
        // 随机取一个1024比特长的质数
        BigInteger modulus = BigInteger.probablePrime(1 << 10, SECURE_RANDOM);
        // 随机选择MAX_ITERATION个底数
        BigInteger[] bases = IntStream.range(0, RANDOM_ROUND)
            .mapToObj(index -> BigIntegerUtils.randomPositive(modulus, SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        // 随机选择MAX_ITERATION个幂，有正有负
        BigInteger[] exponents = IntStream.range(0, RANDOM_ROUND)
            .mapToObj(index -> {
                BigInteger exponent = new BigInteger(10, SECURE_RANDOM);
                return SECURE_RANDOM.nextBoolean() ? exponent.negate() : exponent;
            })
            .toArray(BigInteger[]::new);

        // 用BigInteger.modPow计算
        stopWatch.start();
        BigInteger[] javaModPows = IntStream.range(0, RANDOM_ROUND)
            .mapToObj(index -> bases[index].modPow(exponents[index], modulus))
            .toArray(BigInteger[]::new);
        stopWatch.stop();
        long javaModPowTime = stopWatch.getTime(TimeUnit.MICROSECONDS);
        LOGGER.info("BigInteger.modPow平均计算时间 = {}us", javaModPowTime / RANDOM_ROUND);
        stopWatch.reset();

        // 用BigIntegerUtils.secureModPow计算
        stopWatch.start();
        BigInteger[] utilSecureModPows = IntStream.range(0, RANDOM_ROUND)
            .mapToObj(index -> BigIntegerUtils.secureModPow(bases[index], exponents[index], modulus))
            .toArray(BigInteger[]::new);
        stopWatch.stop();
        Assert.assertArrayEquals(javaModPows, utilSecureModPows);
        long utilSecureModPowTime = stopWatch.getTime(TimeUnit.MICROSECONDS);
        LOGGER.info("BigIntegerUtils.secureModPow平均计算时间 = {}us", utilSecureModPowTime / RANDOM_ROUND);
        stopWatch.reset();

        // 用BigIntegerUtils.modPow计算
        stopWatch.start();
        BigInteger[] utilModPows = IntStream.range(0, RANDOM_ROUND)
            .mapToObj(index -> BigIntegerUtils.modPow(bases[index], exponents[index], modulus))
            .toArray(BigInteger[]::new);
        stopWatch.stop();
        Assert.assertArrayEquals(javaModPows, utilModPows);
        long utilModPowTime = stopWatch.getTime(TimeUnit.MICROSECONDS);
        LOGGER.info("BigIntegerUtils.modPow平均计算时间 = {}us", utilModPowTime / RANDOM_ROUND);
        stopWatch.reset();
    }

    @Test
    public void testModInverse() {
        LOGGER.info("-----测试modInverse性能差距，模数比特长度 = {}比特-----", 1 << 10);
        StopWatch stopWatch = new StopWatch();
        BigInteger modulus = BigInteger.probablePrime(1 << 10, SECURE_RANDOM);
        // 随机选择MAX_ITERATION个底数
        BigInteger[] bases = IntStream.range(0, RANDOM_ROUND)
            .mapToObj(index -> BigIntegerUtils.randomPositive(modulus, SECURE_RANDOM))
            .toArray(BigInteger[]::new);

        // 用Java的modInverse计算
        stopWatch.start();
        BigInteger[] javaModInverses = IntStream.range(0, RANDOM_ROUND)
            .mapToObj(index -> bases[index].modInverse(modulus))
            .toArray(BigInteger[]::new);
        stopWatch.stop();
        long javaModInverseTime = stopWatch.getTime(TimeUnit.MICROSECONDS);
        LOGGER.info("BigInteger.modInverse平均计算时间 = {}us", javaModInverseTime / RANDOM_ROUND);
        stopWatch.reset();

        // 用Util的modInverse计算
        stopWatch.start();
        BigInteger[] utilModInverses = IntStream.range(0, RANDOM_ROUND)
            .mapToObj(index -> BigIntegerUtils.modInverse(bases[index], modulus))
            .toArray(BigInteger[]::new);
        stopWatch.stop();
        Assert.assertArrayEquals(javaModInverses, utilModInverses);
        long utilModInverseTime = stopWatch.getTime(TimeUnit.MICROSECONDS);
        LOGGER.info("BigIntegerUtils.modInverse平均计算时间 = {}us", utilModInverseTime / RANDOM_ROUND);
        stopWatch.reset();
    }

    @Test
    public void testLog2() {
        for (int t = 0; t < Long.SIZE - 1; t++) {
            // x = 2^t
            BigInteger x = BigInteger.valueOf(1L << t);
            Assert.assertEquals(t, BigIntegerUtils.log2(x), DoubleUtils.PRECISION);
        }
    }

    @Test
    public void testRandomNonNegative() {
        try {
            BigIntegerUtils.randomNonNegative(BigInteger.valueOf(-1), SECURE_RANDOM);
            throw new IllegalStateException("ERROR: successfully generate random non-negative with negative n"
            );
        } catch (AssertionError ignored) {

        }
        try {
            BigIntegerUtils.randomNonNegative(BigInteger.ZERO, SECURE_RANDOM);
            throw new IllegalStateException("ERROR: successfully generate random non-negative with n = " + BigInteger.ZERO);
        } catch (AssertionError ignored) {

        }
        // 测试输出结果范围
        for (int bound = 1; bound < UPPER_BOUND; bound++) {
            for (int round = 0; round < RANDOM_ROUND; round++) {
                int random = BigIntegerUtils.randomNonNegative(BigInteger.valueOf(bound), SECURE_RANDOM).intValue();
                Assert.assertTrue(random >= 0 && random < bound);
            }
        }
    }

    @Test
    public void testRandomPositive() {
        try {
            BigIntegerUtils.randomPositive(BigInteger.valueOf(-1), SECURE_RANDOM);
            throw new IllegalStateException("ERROR: successfully generate random positive with negative n"
            );
        } catch (AssertionError ignored) {

        }
        try {
            BigIntegerUtils.randomPositive(BigInteger.ZERO, SECURE_RANDOM);
            throw new IllegalStateException("ERROR: successfully generate random positive with n = " + BigInteger.ZERO);
        } catch (AssertionError ignored) {

        }
        try {
            BigIntegerUtils.randomPositive(BigInteger.ONE, SECURE_RANDOM);
            throw new IllegalStateException("ERROR: successfully generate random positive with n =  " + BigInteger.ONE);
        } catch (AssertionError ignored) {

        }
        // 测试输出结果范围
        for (int bound = 2; bound < UPPER_BOUND; bound++) {
            for (int round = 0; round < RANDOM_ROUND; round++) {
                int random = BigIntegerUtils.randomPositive(BigInteger.valueOf(bound), SECURE_RANDOM).intValue();
                Assert.assertTrue(random > 0 && random < bound);
            }
        }
    }
}
