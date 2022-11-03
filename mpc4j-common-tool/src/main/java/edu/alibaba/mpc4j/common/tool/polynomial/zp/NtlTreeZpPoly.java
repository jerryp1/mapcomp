package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;

/**
 * 用NTL实现的二叉树快速插值。方案描述参见下述论文完整版的附录C：Fast Interpolation and Multi-point Evaluation
 * <p>
 * Pinkas, Benny, Mike Rosulek, Ni Trieu, and Avishay Yanai. Spot-light: Lightweight private set intersection from
 * sparse OT extension. CRYPTO 2019, pp. 401-431. Springer, Cham, 2019.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/2
 */
public class NtlTreeZpPoly extends AbstractZpPoly {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * 有限域质数p的字节数组
     */
    private final byte[] pByteArray;
    /**
     * 有限域质数p的字节长度，可能会大于byteL
     */
    private final int pByteLength;

    public NtlTreeZpPoly(int l) {
        super(l);
        pByteArray = BigIntegerUtils.bigIntegerToByteArray(p);
        pByteLength = pByteArray.length;
    }

    @Override
    public ZpPolyFactory.ZpPolyType getType() {
        return ZpPolyFactory.ZpPolyType.NTL_TREE;
    }

    @Override
    public int coefficientNum(int pointNum, int expectNum) {
        assert pointNum >= 0 && pointNum <= expectNum : "point num must be in range [0, " + expectNum + "]: " + pointNum;
        // 用二叉树快速插值时，pointNum个点的插值多项式包含pointNum + 1个系数，但如果补了随机点，则会等于expectNum
        if (pointNum == expectNum) {
            return expectNum + 1;
        } else {
            return expectNum;
        }
    }

    @Override
    public BigInteger[] interpolate(int expectNum, BigInteger[] xArray, BigInteger[] yArray) {
        assert xArray.length == yArray.length;
        assert expectNum >= 1 && xArray.length <= expectNum;
        for (BigInteger x : xArray) {
            assert validPoint(x);
        }
        for (BigInteger y : yArray) {
            assert validPoint(y);
        }
        byte[][] xByteArray = BigIntegerUtils.nonNegBigIntegersToByteArrays(xArray, pByteLength);
        byte[][] yByteArray = BigIntegerUtils.nonNegBigIntegersToByteArrays(yArray, pByteLength);
        // 调用本地函数完成插值
        byte[][] polynomial = nativeTreeInterpolate(pByteArray, expectNum, xByteArray, yByteArray);
        // 转换为大整数
        return BigIntegerUtils.byteArraysToNonNegBigIntegers(polynomial);
    }

    @Override
    public int rootCoefficientNum(int pointNum, int expectNum) {
        assert pointNum >= 0 && pointNum <= expectNum : "point num must be in range [0, " + expectNum + "]: " + pointNum;
        return expectNum + 1;
    }

    @Override
    public BigInteger[] rootInterpolate(int expectNum, BigInteger[] xArray, BigInteger y) {
        assert expectNum >= 1 && xArray.length <= expectNum;
        if (xArray.length == 0) {
            // 返回随机多项式
            BigInteger[] coefficients = new BigInteger[expectNum + 1];
            for (int index = 0; index < expectNum; index++) {
                coefficients[index] = BigIntegerUtils.randomNonNegative(p, secureRandom);
            }
            // 将最高位设置为1
            coefficients[expectNum] = BigInteger.ONE;
            return coefficients;
        }
        // 如果有插值数据，则继续插值
        for (BigInteger x : xArray) {
            assert validPoint(x);
        }
        assert validPoint(y);
        byte[][] xByteArray = BigIntegerUtils.nonNegBigIntegersToByteArrays(xArray, pByteLength);
        byte[] yBytes = BigIntegerUtils.nonNegBigIntegerToByteArray(y, pByteLength);
        // 调用本地函数完成插值
        byte[][] polynomial = nativeRootInterpolate(pByteArray, expectNum, xByteArray, yBytes);
        // 转换为大整数
        return BigIntegerUtils.byteArraysToNonNegBigIntegers(polynomial);
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
    private static native byte[][] nativeTreeInterpolate(byte[] primeBytes, int num, byte[][] xArray, byte[][] yArray);

    @Override
    public BigInteger evaluate(BigInteger[] coefficients, BigInteger x) {
        assert coefficients.length >= 1;
        for (BigInteger coefficient : coefficients) {
            validPoint(coefficient);
        }
        // 验证x的有效性
        assert validPoint(x);

        byte[][] coefficientByteArrays = BigIntegerUtils.nonNegBigIntegersToByteArrays(coefficients, pByteLength);
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
        assert coefficients.length >= 1;
        for (BigInteger coefficient : coefficients) {
            assert validPoint(coefficient);
        }
        // 验证xArray的有效性
        for (BigInteger x : xArray) {
            assert validPoint(x);
        }

        byte[][] coefficientByteArrays = BigIntegerUtils.nonNegBigIntegersToByteArrays(coefficients, pByteLength);
        byte[][] xByteArrays = BigIntegerUtils.nonNegBigIntegersToByteArrays(xArray, pByteLength);
        // 调用本地函数完成求值
        byte[][] yByteArrays = nativeTreeEvaluate(pByteArray, coefficientByteArrays, xByteArrays);

        return BigIntegerUtils.byteArraysToNonNegBigIntegers(yByteArrays);
    }

    private static native byte[][] nativeTreeEvaluate(byte[] primeBytes, byte[][] coefficients, byte[][] xs);
}
