package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.polynomial.zp.ZpPolyFactory.ZpPolyType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * Zp多项式插值测试。
 *
 * @author Weiran Liu
 * @date 2022/01/05
 */
@RunWith(Parameterized.class)
public class ZpPolyTest {
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
        configurationParams.add(new Object[] {ZpPolyType.NTL.name(), ZpPolyType.NTL,});
        // RINGS_NEWTON
        configurationParams.add(new Object[] {ZpPolyType.RINGS_NEWTON.name(), ZpPolyType.RINGS_NEWTON,});
        // RINGS_LAGRANGE
        configurationParams.add(new Object[] {ZpPolyType.RINGS_LAGRANGE.name(), ZpPolyType.RINGS_LAGRANGE,});

        return configurationParams;
    }

    private final ZpPolyType type;

    public ZpPolyTest(String name, ZpPolyType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        Assert.assertEquals(type, zpPoly.getZpPolyType());
    }

    @Test
    public void testIllegalInputs() {
        // 尝试设置l = 0
        try {
            ZpPolyFactory.createInstance(type, 0);
            throw new IllegalStateException("ERROR: successfully create ZpPoly with l = 0");
        } catch (AssertionError ignored) {

        }
        // 尝试设置l不能被Byte.SIZE整除
        try {
            ZpPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH - 1);
            throw new IllegalStateException("ERROR: successfully create ZpPoly with l % Byte.SIZE != 0");
        } catch (AssertionError ignored) {

        }
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        // 尝试插值1组元素
        try {
            BigInteger[] xArray = IntStream.range(0, 1).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new);
            BigInteger[] yArray = IntStream.range(0, 1).mapToObj(BigInteger::valueOf).toArray(BigInteger[]::new);
            zpPoly.interpolate(1, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate 1 pair");
        } catch (AssertionError ignored) {

        }
        // 尝试对给定的元素数量少于实际元素数量插值
        try {
            BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(BigInteger::valueOf)
                .toArray(BigInteger[]::new);
            BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(BigInteger::valueOf)
                .toArray(BigInteger[]::new);
            zpPoly.interpolate(DEFAULT_NUM / 2, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate with DEFAULT_NUM < actual pairs");
        } catch (AssertionError ignored) {

        }
        // 尝试对大于l比特长度的插值对插值
        try {
            BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> new BigInteger(CommonConstants.STATS_BIT_LENGTH + 1, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> new BigInteger(CommonConstants.STATS_BIT_LENGTH + 1, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate large values");
        } catch (AssertionError ignored) {

        }
        // 尝试对不相等的数据虚拟插值
        try {
            BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM)
                .mapToObj(index -> new BigInteger(CommonConstants.STATS_BIT_LENGTH, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> new BigInteger(CommonConstants.STATS_BIT_LENGTH, SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
            throw new IllegalStateException("ERROR: successfully dummy interpolate points with unequal size");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testEmptyInterpolation() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        // 不存在真实插值点，也应该可以构建多项式
        BigInteger[] xArray = new BigInteger[0];
        BigInteger[] yArray = new BigInteger[0];
        BigInteger[] coefficients = zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        Assert.assertEquals(DEFAULT_NUM, coefficients.length);
    }

    @Test
    public void testOneInterpolation() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        // 只存在一组插值点，也应该可以构建多项式
        BigInteger[] xArray = IntStream.range(0, 1)
            .mapToObj(index -> new BigInteger(zpPoly.getL(), SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger[] yArray = IntStream.range(0, 1)
            .mapToObj(index -> new BigInteger(zpPoly.getL(), SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger[] coefficients = zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        Assert.assertEquals(DEFAULT_NUM, coefficients.length);
        // 验证结果
        testEvaluate(zpPoly, coefficients, xArray, yArray);
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
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, l);
        BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(BigInteger::valueOf)
            .toArray(BigInteger[]::new);
        BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(BigInteger::valueOf)
            .toArray(BigInteger[]::new);
        BigInteger[] coefficients = zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        // 验证多项式结果长度
        Assert.assertEquals(DEFAULT_NUM, coefficients.length);
        // 多项式仍然过(0,0)点，因此常数项仍然为0，但其他位应该均不为0
        Assert.assertEquals(BigInteger.ZERO, coefficients[0]);
        IntStream.range(1, coefficients.length).forEach(i -> Assert.assertNotEquals(BigInteger.ZERO, coefficients[i]));
        // 验证求值
        testEvaluate(zpPoly, coefficients, xArray, yArray);
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
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, l);
        BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM)
            .mapToObj(index -> new BigInteger(zpPoly.getL(), SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM)
            .mapToObj(index -> new BigInteger(zpPoly.getL(), SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger[] coefficients = zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        // 验证多项式结果长度
        Assert.assertEquals(DEFAULT_NUM, coefficients.length);
        // 验证求值
        testEvaluate(zpPoly, coefficients, xArray, yArray);
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
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, l);
        BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(index -> new BigInteger(zpPoly.getL(), SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(index -> new BigInteger(zpPoly.getL(), SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger[] coefficients = zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
        // 验证多项式结果长度
        Assert.assertEquals(DEFAULT_NUM, coefficients.length);
        // 验证求值
        testEvaluate(zpPoly, coefficients, xArray, yArray);
    }

    @Test
    public void testParallel() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        ArrayList<BigInteger[]> xArrayList = new ArrayList<>();
        ArrayList<BigInteger[]> yArrayList = new ArrayList<>();
        IntStream.range(0, MAX_PARALLEL).forEach(parallel -> {
            BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> new BigInteger(zpPoly.getL(), SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger[] yArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> new BigInteger(zpPoly.getL(), SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            xArrayList.add(xArray);
            yArrayList.add(yArray);
        });
        IntStream.range(0, MAX_PARALLEL).parallel().forEach(parallel -> {
            BigInteger[] xArray = xArrayList.get(parallel);
            BigInteger[] yArray = yArrayList.get(parallel);
            BigInteger[] coefficients = zpPoly.interpolate(DEFAULT_NUM, xArray, yArray);
            testEvaluate(zpPoly, coefficients, xArray, yArray);
        });
    }

    private void testEvaluate(ZpPoly zpPoly, BigInteger[] coefficients, BigInteger[] xArray, BigInteger[] yArray) {
        // 逐一求值
        IntStream.range(0, xArray.length).forEach(index -> {
            BigInteger evaluation = zpPoly.evaluate(coefficients, xArray[index]);
            Assert.assertEquals(yArray[index], evaluation);
        });
        // 批量求值
        BigInteger[] evaluations = zpPoly.evaluate(coefficients, xArray);
        Assert.assertArrayEquals(yArray, evaluations);
    }

    @Test
    public void testEmptyRootInterpolation() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        // 不存在真实插值点，也应该可以构建多项式
        BigInteger[] xArray = new BigInteger[0];
        BigInteger[] polynomial = zpPoly.rootInterpolate(DEFAULT_NUM, xArray, null);
        Assert.assertEquals(DEFAULT_NUM + 1, polynomial.length);
    }

    @Test
    public void testOneRootInterpolation() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        // 只存在一组插值点，也应该可以构建多项式
        BigInteger[] xArray = IntStream.range(0, 1)
            .mapToObj(index -> new BigInteger(zpPoly.getL(), SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger y = new BigInteger(zpPoly.getL(), SECURE_RANDOM);
        BigInteger[] polynomial = zpPoly.rootInterpolate(DEFAULT_NUM, xArray, y);
        Assert.assertEquals(DEFAULT_NUM + 1, polynomial.length);
        // 验证结果
        testRootEvaluate(zpPoly, polynomial, xArray, y);
    }

    @Test
    public void testRandomFullRootInterpolation() {
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            // 40比特
            testRandomFulRootlInterpolation(CommonConstants.STATS_BIT_LENGTH);
            // 40 + 8比特
            testRandomFulRootlInterpolation(CommonConstants.STATS_BIT_LENGTH + Byte.SIZE);
            // 40 - 8比特
            testRandomFulRootlInterpolation(CommonConstants.STATS_BIT_LENGTH - Byte.SIZE);
            // 128比特
            testRandomFulRootlInterpolation(CommonConstants.BLOCK_BIT_LENGTH);
            // 128 - 8比特
            testRandomFulRootlInterpolation(CommonConstants.BLOCK_BIT_LENGTH - Byte.SIZE);
            // 128 + 8比特
            testRandomFulRootlInterpolation(CommonConstants.BLOCK_BIT_LENGTH + Byte.SIZE);
        }
    }

    private void testRandomFulRootlInterpolation(int l) {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, l);
        BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM)
            .mapToObj(index -> new BigInteger(zpPoly.getL(), SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger y = new BigInteger(zpPoly.getL(), SECURE_RANDOM);
        BigInteger[] coefficients = zpPoly.rootInterpolate(DEFAULT_NUM, xArray, y);
        // 验证多项式结果长度
        Assert.assertEquals(DEFAULT_NUM + 1, coefficients.length);
        // 验证求值
        testRootEvaluate(zpPoly, coefficients, xArray, y);
    }

    @Test
    public void testRandomHalfRootInterpolation() {
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            // 40比特
            testRandomHalfRootInterpolation(CommonConstants.STATS_BIT_LENGTH);
            // 40 + 8比特
            testRandomHalfRootInterpolation(CommonConstants.STATS_BIT_LENGTH + Byte.SIZE);
            // 40 - 8比特
            testRandomHalfRootInterpolation(CommonConstants.STATS_BIT_LENGTH - Byte.SIZE);
            // 128比特
            testRandomHalfRootInterpolation(CommonConstants.BLOCK_BIT_LENGTH);
            // 128 - 8比特
            testRandomHalfRootInterpolation(CommonConstants.BLOCK_BIT_LENGTH - Byte.SIZE);
            // 128 + 8比特
            testRandomHalfRootInterpolation(CommonConstants.BLOCK_BIT_LENGTH + Byte.SIZE);
        }
    }

    private void testRandomHalfRootInterpolation(int l) {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, l);
        BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM / 2)
            .mapToObj(index -> new BigInteger(zpPoly.getL(), SECURE_RANDOM))
            .toArray(BigInteger[]::new);
        BigInteger y = new BigInteger(zpPoly.getL(), SECURE_RANDOM);
        BigInteger[] coefficients = zpPoly.rootInterpolate(DEFAULT_NUM, xArray, y);
        // 验证多项式结果长度
        Assert.assertEquals(DEFAULT_NUM + 1, coefficients.length);
        // 验证求值
        testRootEvaluate(zpPoly, coefficients, xArray, y);
    }

    @Test
    public void testRootParallel() {
        ZpPoly zpPoly = ZpPolyFactory.createInstance(type, CommonConstants.STATS_BIT_LENGTH);
        ArrayList<BigInteger[]> xArrayList = new ArrayList<>();
        ArrayList<BigInteger> yList = new ArrayList<>();
        IntStream.range(0, MAX_PARALLEL).forEach(parallel -> {
            BigInteger[] xArray = IntStream.range(0, DEFAULT_NUM / 2)
                .mapToObj(index -> new BigInteger(zpPoly.getL(), SECURE_RANDOM))
                .toArray(BigInteger[]::new);
            BigInteger y = new BigInteger(zpPoly.getL(), SECURE_RANDOM);
            xArrayList.add(xArray);
            yList.add(y);
        });
        IntStream.range(0, MAX_PARALLEL).parallel().forEach(parallel -> {
            BigInteger[] xArray = xArrayList.get(parallel);
            BigInteger y = yList.get(parallel);
            BigInteger[] coefficients = zpPoly.rootInterpolate(DEFAULT_NUM, xArray, y);
            testRootEvaluate(zpPoly, coefficients, xArray, y);
        });
    }

    private void testRootEvaluate(ZpPoly zpPoly, BigInteger[] coefficients, BigInteger[] xArray, BigInteger y) {
        // 逐一求值
        Arrays.stream(xArray)
            .map(x -> zpPoly.evaluate(coefficients, x))
            .forEach(evaluation -> Assert.assertEquals(y, evaluation));
        // 批量求值
        Arrays.stream(zpPoly.evaluate(coefficients, xArray))
            .forEach(evaluation -> Assert.assertEquals(y, evaluation));
    }
}
