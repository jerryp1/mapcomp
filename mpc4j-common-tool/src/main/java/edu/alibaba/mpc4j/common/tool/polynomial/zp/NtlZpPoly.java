package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.polynomial.zp.ZpPolyFactory.ZpPolyType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * NTL的Zp有限域多项式插值本地函数。
 *
 * @author Weiran Liu
 * @date 2022/01/04
 */
public class NtlZpPoly implements ZpPoly {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * 随机状态
     */
    private final SecureRandom secureRandom;
    /**
     * 质数p
     */
    private final BigInteger p;
    /**
     * 有限域质数p的字节数组
     */
    private final byte[] pByteArray;
    /**
     * 有限域比特长度
     */
    private final int l;
    /**
     * 有限域质数p的字节长度，可能会大于byteL
     */
    private final int pByteLength;

    public NtlZpPoly(int l) {
        assert l > 0 && l % Byte.SIZE == 0;
        p = ZpManager.getPrime(l);
        this.l = l;
        pByteArray = BigIntegerUtils.bigIntegerToByteArray(p);
        pByteLength = pByteArray.length;
        secureRandom = new SecureRandom();
    }

    @Override
    public ZpPolyType getZpPolyType() {
        return ZpPolyType.NTL;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public BigInteger getPrime() {
        return p;
    }

    @Override
    public BigInteger[] interpolate(int num, BigInteger[] xArray, BigInteger[] yArray) {
        assert xArray.length == yArray.length;
        // 不要求至少有1个插值点，只要求总数量大于1
        assert num > 1 && xArray.length <= num;
        for (int i = 0; i < xArray.length; i++) {
            assert BigIntegerUtils.greaterOrEqual(xArray[i], BigInteger.ZERO) && xArray[i].bitLength() <= l;
            assert BigIntegerUtils.greaterOrEqual(yArray[i], BigInteger.ZERO) && yArray[i].bitLength() <= l;
        }
        byte[][] xByteArray = bigIntegersToByteArrays(xArray);
        byte[][] yByteArray = bigIntegersToByteArrays(yArray);
        // 调用本地函数完成插值
        byte[][] polynomial = nativeInterpolate(pByteArray, num, xByteArray, yByteArray);
        // 转换为大整数
        return byteArraysToBigIntegers(polynomial);
    }

    @Override
    public BigInteger[] rootInterpolate(int num, BigInteger[] xArray, BigInteger y) {
        // 不要求至少有1个插值点，只要求总数量大于1
        assert num > 1 && xArray.length <= num;
        if (xArray.length == 0) {
            // 返回随机多项式
            BigInteger[] coefficients = new BigInteger[num + 1];
            for (int index = 0; index < num; index++) {
                coefficients[index] = BigIntegerUtils.randomPositive(p, secureRandom);
            }
            // 将最高位设置为1
            coefficients[num] = BigInteger.ONE;
            return coefficients;
        }
        // 如果有插值数据，则继续插值
        for (BigInteger x : xArray) {
            assert BigIntegerUtils.greaterOrEqual(x, BigInteger.ZERO) && x.bitLength() <= l;
        }
        assert BigIntegerUtils.greaterOrEqual(y, BigInteger.ZERO) && y.bitLength() <= l;
        byte[][] xByteArray = bigIntegersToByteArrays(xArray);
        byte[] yBytes = BigIntegerUtils.nonNegBigIntegerToByteArray(y, pByteLength);
        // 调用本地函数完成插值
        byte[][] polynomial = nativeRootInterpolate(pByteArray, num, xByteArray, yBytes);
        // 转换为大整数
        return byteArraysToBigIntegers(polynomial);
    }

    /**
     * NTL底层库的虚拟点插值。
     *
     * @param primeBytes 质数字节数组。
     * @param num        插值点数量。
     * @param xArray     x_i数组。
     * @param yBytes     y。
     * @return 插值多项式的系数。
     */
    private static native byte[][] nativeRootInterpolate(byte[] primeBytes, int num, byte[][] xArray, byte[] yBytes);

    /**
     * NTL底层库的虚拟点插值。
     *
     * @param primeBytes 质数字节数组。
     * @param num        插值点数量。
     * @param xArray     x_i数组。
     * @param yArray     y_i数组。
     * @return 插值多项式的系数。
     */
    private static native byte[][] nativeInterpolate(byte[] primeBytes, int num, byte[][] xArray, byte[][] yArray);

    @Override
    public BigInteger evaluate(BigInteger[] coefficients, BigInteger x) {
        // 至少包含2个系数，每个系数都属于Zp
        assert coefficients.length > 1;
        for (BigInteger coefficient : coefficients) {
            assert BigIntegerUtils.greaterOrEqual(coefficient, BigInteger.ZERO);
            assert BigIntegerUtils.less(coefficient, p);
        }
        // 验证x的有效性
        assert BigIntegerUtils.greaterOrEqual(x, BigInteger.ZERO);
        assert x.bitLength() <= l;

        byte[][] coefficientByteArrays = bigIntegersToByteArrays(coefficients);
        byte[] xByteArray = BigIntegerUtils.nonNegBigIntegerToByteArray(x, pByteLength);
        // 调用本地函数完成求值
        byte[] yByteArray = nativeSingleEvaluate(pByteArray, coefficientByteArrays, xByteArray);
        // 转换为大整数
        return BigIntegerUtils.byteArrayToNonNegBigInteger(yByteArray);
    }

    /**
     * 多项式求值。
     *
     * @param primeBytes   质数字节数组。
     * @param coefficients 插值多项式系数。
     * @param x            输入x。
     * @return f(x)。
     */
    private static native byte[] nativeSingleEvaluate(byte[] primeBytes, byte[][] coefficients, byte[] x);

    @Override
    public BigInteger[] evaluate(BigInteger[] coefficients, BigInteger[] xArray) {
        // 至少包含2个系数，每个系数都属于Zp
        assert coefficients.length > 1;
        for (BigInteger coefficient : coefficients) {
            assert BigIntegerUtils.greaterOrEqual(coefficient, BigInteger.ZERO);
            assert BigIntegerUtils.less(coefficient, p);
        }
        // 验证xArray的有效性
        for (BigInteger x : xArray) {
            assert BigIntegerUtils.greaterOrEqual(x, BigInteger.ZERO);
            assert x.bitLength() <= l;
        }

        byte[][] coefficientByteArrays = bigIntegersToByteArrays(coefficients);
        byte[][] xByteArrays = bigIntegersToByteArrays(xArray);
        // 调用本地函数完成求值
        byte[][] yByteArrays = nativeEvaluate(pByteArray, coefficientByteArrays, xByteArrays);

        return byteArraysToBigIntegers(yByteArrays);
    }

    /**
     * 多项式求值。
     *
     * @param primeBytes   质数字节数组。
     * @param coefficients 插值多项式。
     * @param xArray       x_i数组。
     * @return f(x_i)数组。
     */
    private static native byte[][] nativeEvaluate(byte[] primeBytes, byte[][] coefficients, byte[][] xArray);

    /**
     * 将BigInteger[]形式的数据转换为byte[][]形式的数据。
     *
     * @param bigIntegers BigInteger[]形式的数据。
     * @return 转换结果。
     */
    private byte[][] bigIntegersToByteArrays(BigInteger[] bigIntegers) {
        return Arrays.stream(bigIntegers)
            .map(x -> BigIntegerUtils.nonNegBigIntegerToByteArray(x, pByteLength))
            .toArray(byte[][]::new);
    }

    /**
     * 将byte[][]形式的数据转换为BigInteger[]形式的数据。
     *
     * @param byteArrays byte[][]形式的数据。
     * @return 转换结果。
     */
    private static BigInteger[] byteArraysToBigIntegers(byte[][] byteArrays) {
        return Arrays.stream(byteArrays)
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
    }
}
