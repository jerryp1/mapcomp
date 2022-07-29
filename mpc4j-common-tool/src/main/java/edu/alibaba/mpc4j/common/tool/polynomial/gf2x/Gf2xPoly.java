package edu.alibaba.mpc4j.common.tool.polynomial.gf2x;

import edu.alibaba.mpc4j.common.tool.polynomial.gf2x.Gf2xPolyFactory.Gf2xPolyType;

/**
 * GF(2^l)多项式插值接口。
 *
 * @author Weiran Liu
 * @date 2021/12/11
 */
public interface Gf2xPoly {
    /**
     * 返回GF(2^l)多项式插值类型。
     *
     * @return GF(2 ^ l)多项式插值类型。
     */
    Gf2xPolyType getGf2xPolyType();

    /**
     * 返l对应的字节长度。
     *
     * @return l对应的字节长度。
     */
    int getByteL();

    /**
     * 返回l的比特长度。
     *
     * @return l的比特长度。
     */
    default int getL() {
        return getByteL() * Byte.SIZE;
    }

    /**
     * 得到插值多项式f(x)，使得y_i = f(x_i)。在插值点中补充随机元素，使插值数量为num。此插值结果的系数数量为num。
     *
     * @param num    所需插值点数量。
     * @param xArray x_i数组。
     * @param yArray y_i数组。
     * @return 插值多项式的系数。
     */
    byte[][] interpolate(int num, byte[][] xArray, byte[][] yArray);

    /**
     * 得到插值多项式f(x)，使得对于所有x_i，都有y = f(x_i)，在插值点中补充随机元素，使插值数量为num。此插值结果的系数数量为num + 1。
     *
     * @param num    所需插值点数量。
     * @param xArray x_i数组。
     * @param yBytes y的值。
     * @return 插值多项式的系数。
     */
    byte[][] rootInterpolate(int num, byte[][] xArray, byte[] yBytes);

    /**
     * 计算y = f(x)。
     *
     * @param coefficients 多项式系数。
     * @param xBytes       输入x。
     * @return f(x)。
     */
    byte[] evaluate(byte[][] coefficients, byte[] xBytes);

    /**
     * 计算y_i = f(x_i)。
     *
     * @param coefficients 多项式系数。
     * @param xArray       x_i数组。
     * @return f(x_i)数组。
     */
    byte[][] evaluate(byte[][] coefficients, byte[][] xArray);
}
