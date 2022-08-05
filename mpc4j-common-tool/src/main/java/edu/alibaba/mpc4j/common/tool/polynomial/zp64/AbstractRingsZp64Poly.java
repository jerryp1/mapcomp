package edu.alibaba.mpc4j.common.tool.polynomial.zp64;

import cc.redberry.rings.IntegersZp64;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.Zp64.Zp64Manager;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 应用Rings的Zp64多项式差值抽象类。
 *
 * @author Weiran Liu
 * @date 2022/8/3
 */
abstract class AbstractRingsZp64Poly implements Zp64Poly {
    /**
     * Zp64有限域
     */
    private final IntegersZp64 finiteField;
    /**
     * 有限域模数p
     */
    protected final long p;
    /**
     * 有限域比特长度
     */
    private final int l;

    AbstractRingsZp64Poly(int l) {
        p = Zp64Manager.getPrime(l);
        finiteField = Zp64Manager.getFiniteField(l);
        this.l = l;
    }

    AbstractRingsZp64Poly(long p) {
        assert BigInteger.valueOf(p).isProbablePrime(CommonConstants.STATS_BIT_LENGTH) : "p is probably not prime: " + p;
        this.p = p;
        finiteField = new IntegersZp64(p);
        this.l = LongUtils.ceilLog2(p) - 1;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public long getPrime() {
        return p;
    }

    @Override
    public int coefficientNum(int num) {
        assert num > 1 : "# of points must be greater than 1: " + num;
        return num;
    }

    @Override
    public long[] interpolate(int num, long[] xArray, long[] yArray) {
        assert xArray.length == yArray.length;
        // 不要求至少有1个插值点，只要求总数量大于1
        assert num > 1 && xArray.length <= num;
        for (long x : xArray) {
            assert validPoint(x);
        }
        for (long y : yArray) {
            assert validPoint(y);
        }
        // 插值
        UnivariatePolynomialZp64 interpolatePolynomial = polynomialInterpolate(num, xArray, yArray);
        // 如果插值点数量小于最大点数量，则补充虚拟点
        if (xArray.length < num) {
            // 计算(x - x_1) * ... * (x - x_m')
            UnivariatePolynomialZp64 p1 = UnivariatePolynomialZp64.one(finiteField);
            for (long x : xArray) {
                p1 = p1.multiply(UnivariatePolynomialZp64.zero(finiteField).createLinear(finiteField.negate(x), 1L));
            }
            // 构造随机多项式
            long[] prCoefficients = new long[num - xArray.length];
            for (int index = 0; index < prCoefficients.length; index++) {
                prCoefficients[index] = finiteField.randomElement();
            }
            UnivariatePolynomialZp64 pr = UnivariatePolynomialZp64.create(finiteField, prCoefficients);
            // 计算P_0(x) + P_1(x) * P_r(x)
            interpolatePolynomial = interpolatePolynomial.add(p1.multiply(pr));
        }
        return polynomialToLongs(num, interpolatePolynomial);
    }

    @Override
    public int rootCoefficientNum(int num) {
        assert num > 1 : "# of points must be greater than 1: " + num;
        return num + 1;
    }

    @Override
    public long[] rootInterpolate(int num, long[] xArray, long y) {
        // 不要求至少有1个插值点，只要求总数量大于1
        assert num > 1 && xArray.length <= num;
        if (xArray.length == 0) {
            // 返回随机多项式
            long[] coefficients = new long[num + 1];
            for (int index = 0; index < num; index++) {
                coefficients[index] = finiteField.randomElement();
            }
            // 将最高位设置为1
            coefficients[num] = 1L;
            return coefficients;
        }
        // 如果有插值数据，则继续插值
        for (long x : xArray) {
            assert validPoint(x);
        }
        assert validPoint(y);
        // 插值
        UnivariatePolynomialZp64 polynomial = UnivariatePolynomialZp64.one(finiteField);
        // f(x) = (x - x_0) * (x - x_1) * ... * (x - x_m)
        for (long x : xArray) {
            UnivariatePolynomialZp64 linear = polynomial.createLinear(finiteField.negate(x), 1L);
            polynomial = polynomial.multiply(linear);
        }
        if (xArray.length < num) {
            // 构造随机多项式
            long[] prCoefficients = IntStream.range(0, num - xArray.length)
                .mapToLong(index -> finiteField.randomElement())
                .toArray();
            UnivariatePolynomialZp64 dummyPolynomial = UnivariatePolynomialZp64.create(finiteField, prCoefficients);
            // 把最高位设置为1
            dummyPolynomial.set(num - xArray.length, 1L);
            // 计算P_0(x) * P_r(x)
            polynomial = polynomial.multiply(dummyPolynomial);
        }
        polynomial = polynomial.add(UnivariatePolynomialZp64.constant(finiteField, y));

        return rootPolynomialToLongs(num, polynomial);
    }

    /**
     * 多项式插值，得到多项式f(x)，使得y = f(x)，返回多项式本身。
     *
     * @param num    插值点数量。
     * @param xArray x数组。
     * @param yArray y数组。
     * @return 多项式。
     */
    protected abstract UnivariatePolynomialZp64 polynomialInterpolate(int num, long[] xArray, long[] yArray);

    @Override
    public long evaluate(long[] coefficients, long x) {
        // 至少包含2个系数，每个系数都属于Zp
        assert coefficients.length > 1;
        for (long coefficient : coefficients) {
            assert validPoint(coefficient);
        }
        // 验证x的有效性
        assert validPoint(x);
        // 求值
        UnivariatePolynomialZp64 polynomial = UnivariatePolynomialZp64.create(finiteField, coefficients);
        return polynomial.evaluate(x);
    }

    @Override
    public long[] evaluate(long[] coefficients, long[] xArray) {
        UnivariatePolynomialZp64 polynomial = UnivariatePolynomialZp64.create(finiteField, coefficients);
        return Arrays.stream(xArray).map(polynomial::evaluate).toArray();
    }

    long[] polynomialToLongs(int num, UnivariatePolynomialZp64 polynomial) {
        long[] coefficients = new long[num];
        // 低阶系数正常拷贝，高阶系数默认为0
        IntStream.range(0, polynomial.degree() + 1).forEach(degreeIndex ->
            coefficients[degreeIndex] = polynomial.get(degreeIndex)
        );
        return coefficients;
    }

    long[] rootPolynomialToLongs(int num, UnivariatePolynomialZp64 polynomial) {
        long[] coefficients = new long[num + 1];
        // 低阶系数正常拷贝
        IntStream.range(0, polynomial.degree() + 1).forEach(degreeIndex ->
            coefficients[degreeIndex] = polynomial.get(degreeIndex)
        );
        // 高阶系数补0
        IntStream.range(polynomial.degree() + 1, coefficients.length).forEach(degreeIndex ->
            coefficients[degreeIndex] = 0L
        );

        return coefficients;
    }

    private boolean validPoint(long point) {
        return point >= 0 && point < p;
    }
}
