package edu.alibaba.mpc4j.common.tool.polynomial.gf2x;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2x.Gf2xPolyFactory.Gf2xPolyType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * GF(2^l)多项式插值测试。
 *
 * @author Weiran Liu
 * @date 2021/12/11
 */
@RunWith(Parameterized.class)
public class Gf2xPolyTest {
    /**
     * 最大随机轮数
     */
    private static final int MAX_RANDOM_ROUND = 5;
    /**
     * 插值点数量
     */
    private static final int DEFAULT_NUM = 20;
    /**
     * 并发数量
     */
    private static final int MAX_PARALLEL = 10;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // NTL
        configurationParams.add(new Object[] {Gf2xPolyType.NTL.name(), Gf2xPolyType.NTL,});
        // RINGS_NEWTON
        configurationParams.add(new Object[] {Gf2xPolyType.RINGS_NEWTON.name(), Gf2xPolyType.RINGS_NEWTON,});
        // RINGS_LAGRANGE
        configurationParams.add(new Object[] {Gf2xPolyType.RINGS_LAGRANGE.name(), Gf2xPolyType.RINGS_LAGRANGE,});

        return configurationParams;
    }

    private final Gf2xPolyType type;

    public Gf2xPolyTest(String name, Gf2xPolyType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        Assert.assertEquals(type, gf2xPoly.getGf2xPolyType());
    }

    @Test
    public void testIllegalInputs() {
        // 尝试设置l = 0
        try {
            Gf2xPolyFactory.createInstance(type, 0);
            throw new IllegalStateException("ERROR: successfully create Gf2xPoly with l = 0");
        } catch (AssertionError ignored) {

        }
        // 尝试设置l不能被Byte.SIZE整除
        try {
            Gf2xPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH - 1);
            throw new IllegalStateException("ERROR: successfully create Gf2xPoly with l % Byte.SIZE != 0");
        } catch (AssertionError ignored) {

        }
        Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        // 尝试插值1组元素
        try {
            byte[][] xArray = IntStream.range(0, 1)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL()];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            byte[][] yArray = IntStream.range(0, 1)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL()];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            gf2xPoly.interpolate(1, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate 1 pair");
        } catch (AssertionError ignored) {

        }
        // 尝试对给定的元素数量少于实际元素数量插值
        try {
            byte[][] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL()];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            byte[][] yArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL()];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            gf2xPoly.interpolate(DEFAULT_NUM / 2, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate with DEFAULT_NUM < actual pairs");
        } catch (AssertionError ignored) {

        }
        // 尝试对大于l比特长度的插值对插值
        try {
            byte[][] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL() + 1];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            byte[][] yArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL() + 1];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            gf2xPoly.interpolate(DEFAULT_NUM, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate large values");
        } catch (AssertionError ignored) {

        }
        // 尝试对不相等的数据插值
        try {
            byte[][] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL()];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            byte[][] yArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL()];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            gf2xPoly.interpolate(DEFAULT_NUM, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate points with unequal size");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testEmptyInterpolation() {
        Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        // 不存在真实插值点，也应该可以构建多项式
        byte[][] xArray = new byte[0][];
        byte[][] yArray = new byte[0][];
        byte[][] coefficients = gf2xPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        Assert.assertEquals(DEFAULT_NUM, coefficients.length);
    }

    @Test
    public void testOneInterpolation() {
        Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        // 只存在一组插值点，也应该可以构建多项式
        byte[][] xArray = IntStream.range(0, 1)
            .mapToObj(index -> {
                byte[] xBytes = new byte[gf2xPoly.getByteL()];
                SECURE_RANDOM.nextBytes(xBytes);
                return xBytes;
            })
            .toArray(byte[][]::new);
        byte[][] yArray = IntStream.range(0, 1)
            .mapToObj(index -> {
                byte[] xBytes = new byte[gf2xPoly.getByteL()];
                SECURE_RANDOM.nextBytes(xBytes);
                return xBytes;
            })
            .toArray(byte[][]::new);
        byte[][] coefficients = gf2xPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        Assert.assertEquals(DEFAULT_NUM, coefficients.length);
        // 验证结果
        testEvaluate(gf2xPoly, coefficients, xArray, yArray);
    }

    @Test
    public void testInterpolation() {
        // 40比特
        testInterpolation(CommonConstants.STATS_BIT_LENGTH);
        // 40 + 8比特
        testInterpolation(CommonConstants.STATS_BIT_LENGTH + Byte.SIZE);
        // 128比特
        testInterpolation(CommonConstants.BLOCK_BIT_LENGTH);
        // 128 - 8比特
        testInterpolation(CommonConstants.BLOCK_BIT_LENGTH - Byte.SIZE);
        // 128 + 8比特
        testInterpolation(CommonConstants.BLOCK_BIT_LENGTH + Byte.SIZE);
    }

    private void testInterpolation(int l) {
        Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, l);
        byte[][] xArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(BigInteger::valueOf)
            .map(x -> BigIntegerUtils.nonNegBigIntegerToByteArray(x, gf2xPoly.getByteL()))
            .toArray(byte[][]::new);
        byte[][] yArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(BigInteger::valueOf)
            .map(x -> BigIntegerUtils.nonNegBigIntegerToByteArray(x, gf2xPoly.getByteL()))
            .toArray(byte[][]::new);
        byte[][] coefficients = gf2xPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        // 验证多项式结果长度
        Assert.assertEquals(DEFAULT_NUM, coefficients.length);
        byte[] zero = new byte[gf2xPoly.getByteL()];
        // 多项式仍然过(0,0)点，因此常数项仍然为0，但其他位应该均不为0
        Assert.assertArrayEquals(zero, coefficients[0]);
        IntStream.range(1, coefficients.length).forEach(i ->
            Assert.assertNotEquals(ByteBuffer.wrap(zero), ByteBuffer.wrap(coefficients[i]))
        );
        // 验证求值
        testEvaluate(gf2xPoly, coefficients, xArray, yArray);
    }

    @Test
    public void testRandomFullInterpolation() {
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            // 40比特
            testRandomFullInterpolation(CommonConstants.STATS_BIT_LENGTH);
            // 40 + 8比特
            testRandomFullInterpolation(CommonConstants.STATS_BIT_LENGTH + Byte.SIZE);
            // 128比特
            testRandomFullInterpolation(CommonConstants.BLOCK_BIT_LENGTH);
            // 128 - 8比特
            testRandomFullInterpolation(CommonConstants.BLOCK_BIT_LENGTH - Byte.SIZE);
            // 128 + 8比特
            testRandomFullInterpolation(CommonConstants.BLOCK_BIT_LENGTH + Byte.SIZE);
        }
    }

    private void testRandomFullInterpolation(int l) {
        Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, l);
        byte[][] xArray = IntStream.range(0, DEFAULT_NUM)
            .mapToObj(index -> {
                byte[] xBytes = new byte[gf2xPoly.getByteL()];
                SECURE_RANDOM.nextBytes(xBytes);
                return xBytes;
            })
            .toArray(byte[][]::new);
        byte[][] yArray = IntStream.range(0, DEFAULT_NUM)
            .mapToObj(index -> {
                byte[] xBytes = new byte[gf2xPoly.getByteL()];
                SECURE_RANDOM.nextBytes(xBytes);
                return xBytes;
            })
            .toArray(byte[][]::new);
        byte[][] coefficients = gf2xPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        // 验证多项式结果长度
        Assert.assertEquals(DEFAULT_NUM, coefficients.length);
        // 验证求值
        testEvaluate(gf2xPoly, coefficients, xArray, yArray);
    }

    @Test
    public void testRandomHalfInterpolation() {
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            // 40比特
            testRandomHalfInterpolation(CommonConstants.STATS_BIT_LENGTH);
            // 40 + 8比特
            testRandomHalfInterpolation(CommonConstants.STATS_BIT_LENGTH + Byte.SIZE);
            // 128比特
            testRandomHalfInterpolation(CommonConstants.BLOCK_BIT_LENGTH);
            // 128 - 8比特
            testRandomHalfInterpolation(CommonConstants.BLOCK_BIT_LENGTH - Byte.SIZE);
            // 128 + 8比特
            testRandomHalfInterpolation(CommonConstants.BLOCK_BIT_LENGTH + Byte.SIZE);
        }
    }

    private void testRandomHalfInterpolation(int l) {
        Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, l);
        byte[][] xArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(index -> {
                byte[] xBytes = new byte[gf2xPoly.getByteL()];
                SECURE_RANDOM.nextBytes(xBytes);
                return xBytes;
            })
            .toArray(byte[][]::new);
        byte[][] yArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(index -> {
                byte[] xBytes = new byte[gf2xPoly.getByteL()];
                SECURE_RANDOM.nextBytes(xBytes);
                return xBytes;
            })
            .toArray(byte[][]::new);
        byte[][] coefficients = gf2xPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        // 验证多项式结果长度
        Assert.assertEquals(DEFAULT_NUM, coefficients.length);
        // 验证求值
        testEvaluate(gf2xPoly, coefficients, xArray, yArray);
    }

    @Test
    public void testParallel() {
        Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        ArrayList<byte[][]> xArrayList = new ArrayList<>();
        ArrayList<byte[][]> yArrayList = new ArrayList<>();
        IntStream.range(0, MAX_PARALLEL).forEach(parallel -> {
            byte[][] xArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL()];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            byte[][] yArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL()];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            xArrayList.add(xArray);
            yArrayList.add(yArray);
        });
        IntStream.range(0, MAX_PARALLEL).parallel().forEach(parallel -> {
            byte[][] xArray = xArrayList.get(parallel);
            byte[][] yArray = yArrayList.get(parallel);
            byte[][] coefficients = gf2xPoly.interpolate(DEFAULT_NUM, xArray, yArray);
            testEvaluate(gf2xPoly, coefficients, xArray, yArray);
        });
    }

    private void testEvaluate(Gf2xPoly gf2xPoly, byte[][] coefficients, byte[][] xArray, byte[][] yArray) {
        // 逐一求值
        IntStream.range(0, xArray.length).forEach(index -> {
            byte[] evaluation = gf2xPoly.evaluate(coefficients, xArray[index]);
            Assert.assertArrayEquals(yArray[index], evaluation);
        });
        // 批量求值
        byte[][] evaluations = gf2xPoly.evaluate(coefficients, xArray);
        Assert.assertArrayEquals(yArray, evaluations);
    }

    @Test
    public void testEmptyRootInterpolation() {
        Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        // 不存在真实插值点，也应该可以构建多项式
        byte[][] xArray = new byte[0][];
        byte[][] coefficients = gf2xPoly.rootInterpolate(DEFAULT_NUM, xArray, null);
        Assert.assertEquals(DEFAULT_NUM + 1, coefficients.length);
    }

    @Test
    public void testOneRootInterpolation() {
        Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        // 只存在一组插值点，也应该可以构建多项式
        byte[][] xArray = IntStream.range(0, 1)
            .mapToObj(index -> {
                byte[] xBytes = new byte[gf2xPoly.getByteL()];
                SECURE_RANDOM.nextBytes(xBytes);
                return xBytes;
            })
            .toArray(byte[][]::new);
        byte[] yBytes = new byte[gf2xPoly.getByteL()];
        SECURE_RANDOM.nextBytes(yBytes);
        byte[][] coefficients = gf2xPoly.rootInterpolate(DEFAULT_NUM, xArray, yBytes);
        Assert.assertEquals(DEFAULT_NUM + 1, coefficients.length);
        // 验证求值
        testRootEvaluate(gf2xPoly, coefficients, xArray, yBytes);
    }

    @Test
    public void testRandomFullRootInterpolation() {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            // 40比特
            testRandomFullRootInterpolation(CommonConstants.STATS_BIT_LENGTH);
            // 40 + 8比特
            testRandomFullRootInterpolation(CommonConstants.STATS_BIT_LENGTH + Byte.SIZE);
            // 128比特
            testRandomFullRootInterpolation(CommonConstants.BLOCK_BIT_LENGTH);
            // 128 - 8比特
            testRandomFullRootInterpolation(CommonConstants.BLOCK_BIT_LENGTH - Byte.SIZE);
            // 128 + 8比特
            testRandomFullRootInterpolation(CommonConstants.BLOCK_BIT_LENGTH + Byte.SIZE);
        }
    }

    private void testRandomFullRootInterpolation(int l) {
        Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, l);
        byte[][] xArray = IntStream.range(0, DEFAULT_NUM)
            .mapToObj(index -> {
                byte[] xBytes = new byte[gf2xPoly.getByteL()];
                SECURE_RANDOM.nextBytes(xBytes);
                return xBytes;
            })
            .toArray(byte[][]::new);
        byte[] yBytes = new byte[gf2xPoly.getByteL()];
        SECURE_RANDOM.nextBytes(yBytes);
        byte[][] coefficients = gf2xPoly.rootInterpolate(DEFAULT_NUM, xArray, yBytes);
        // 验证多项式结果长度
        Assert.assertEquals(DEFAULT_NUM + 1, coefficients.length);
        // 验证求值
        testRootEvaluate(gf2xPoly, coefficients, xArray, yBytes);
    }

    @Test
    public void testRandomHalfRootInterpolation() {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            // 40比特
            testRandomHalfRootInterpolation(CommonConstants.STATS_BIT_LENGTH);
            // 40 + 8比特
            testRandomHalfRootInterpolation(CommonConstants.STATS_BIT_LENGTH + Byte.SIZE);
            // 128比特
            testRandomHalfRootInterpolation(CommonConstants.BLOCK_BIT_LENGTH);
            // 128 - 8比特
            testRandomHalfRootInterpolation(CommonConstants.BLOCK_BIT_LENGTH - Byte.SIZE);
            // 128 + 8比特
            testRandomHalfRootInterpolation(CommonConstants.BLOCK_BIT_LENGTH + Byte.SIZE);
        }
    }

    private void testRandomHalfRootInterpolation(int l) {
        Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, l);
        byte[][] xArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(index -> {
                byte[] xBytes = new byte[gf2xPoly.getByteL()];
                SECURE_RANDOM.nextBytes(xBytes);
                return xBytes;
            })
            .toArray(byte[][]::new);
        byte[] yBytes = new byte[gf2xPoly.getByteL()];
        SECURE_RANDOM.nextBytes(yBytes);
        byte[][] coefficients = gf2xPoly.rootInterpolate(DEFAULT_NUM, xArray, yBytes);
        // 验证多项式结果长度
        Assert.assertEquals(DEFAULT_NUM + 1, coefficients.length);
        // 验证求值
        testRootEvaluate(gf2xPoly, coefficients, xArray, yBytes);
    }

    @Test
    public void testRootParallel() {
        Gf2xPoly gf2xPoly = Gf2xPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        ArrayList<byte[][]> xArrayList = new ArrayList<>();
        ArrayList<byte[]> yList = new ArrayList<>();
        IntStream.range(0, MAX_PARALLEL).forEach(parallel -> {
            byte[][] xArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> {
                    byte[] xBytes = new byte[gf2xPoly.getByteL()];
                    SECURE_RANDOM.nextBytes(xBytes);
                    return xBytes;
                })
                .toArray(byte[][]::new);
            byte[] yBytes = new byte[gf2xPoly.getByteL()];
            SECURE_RANDOM.nextBytes(yBytes);
            xArrayList.add(xArray);
            yList.add(yBytes);
        });
        IntStream.range(0, MAX_PARALLEL).forEach(parallel -> {
            byte[][] xArray = xArrayList.get(parallel);
            byte[] yBytes = yList.get(parallel);
            byte[][] coefficients = gf2xPoly.rootInterpolate(DEFAULT_NUM, xArray, yBytes);
            testRootEvaluate(gf2xPoly, coefficients, xArray, yBytes);
        });
    }

    private void testRootEvaluate(Gf2xPoly gf2xPoly, byte[][] coefficients, byte[][] xArray, byte[] yBytes) {
        // 逐一求值
        Arrays.stream(xArray)
            .map(x -> gf2xPoly.evaluate(coefficients, x))
            .forEach(evaluation -> Assert.assertArrayEquals(yBytes, evaluation));
        // 批量求值
        Arrays.stream(gf2xPoly.evaluate(coefficients, xArray))
            .forEach(evaluation -> Assert.assertArrayEquals(yBytes, evaluation));
    }
}
