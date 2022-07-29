package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import java.math.BigInteger;

/**
 * Zp多项式插值接口。
 *
 * @author Weiran Liu
 * @date 2022/01/05
 */
public interface ZpPoly {

    /**
     * 返回Zp多项式插值类型。
     *
     * @return Zp多项式插值类型。
     */
    ZpPolyFactory.ZpPolyType getZpPolyType();

    /**
     * 返回l的比特长度。
     *
     * @return l的比特长度。
     */
    int getL();

    /**
     * 返回Z_p群的模质数p。
     *
     * @return 模质数p。
     */
    BigInteger getPrime();

    /**
     * 得到插值多项式f(x)，使得y_i = f(x_i)。在插值点中补充随机元素，使插值数量为num。此插值结果的系数数量为num。
     *
     * @param num    插值点数量。
     * @param xArray x_i数组。
     * @param yArray y_i数组。
     * @return 插值多项式的系数。
     */
    BigInteger[] interpolate(int num, BigInteger[] xArray, BigInteger[] yArray);

    /**
     * 得到插值多项式f(x)，使得对于所有x_i，都有y = f(x_i)，在插值点中补充随机元素，使插值数量为num。此插值结果的系数数量为num + 1。
     *
     * @param num    所需插值点数量。
     * @param xArray x_i数组。
     * @param y      y的值。
     * @return 插值多项式的系数。
     */
    BigInteger[] rootInterpolate(int num, BigInteger[] xArray, BigInteger y);

    /**
     * 计算y = f(x)。
     *
     * @param coefficients 多项式系数。
     * @param x            输入x。
     * @return f(x)。
     */
    BigInteger evaluate(BigInteger[] coefficients, BigInteger x);

    /**
     * 计算y_i = f(x_i)。
     *
     * @param coefficients 多项式系数。
     * @param xArray       x_i数组。
     * @return f(x_i)数组。
     */
    BigInteger[] evaluate(BigInteger[] coefficients, BigInteger[] xArray);
}
