package edu.alibaba.mpc4j.common.tool.polynomial.gf2x;

/**
 * NTL的GF(2^l)有限域多项式插值本地函数。
 *
 * @author Weiran Liu
 * @date 2021/12/11
 */
class NtlNativeGf2xPoly {

    private NtlNativeGf2xPoly() {
        // empty
    }

    /**
     * NTL底层库插值。
     *
     * @param minBytes 最小多项式系数。
     * @param byteL    l字节长度。
     * @param num      插值点数量。
     * @param xArray   x_i数组。
     * @param yArray   y_i数组。
     * @return 插值多项式的系数。
     */
    static native byte[][] interpolate(byte[] minBytes, int byteL, int num, byte[][] xArray, byte[][] yArray);

    /**
     * NTL底层库插值。
     *
     * @param minBytes 最小多项式系数。
     * @param byteL    l字节长度。
     * @param num      插值点数量。
     * @param xArray   x_i数组。
     * @param yBytes   y。
     * @return 插值多项式的系数。
     */
    static native byte[][] rootInterpolate(byte[] minBytes, int byteL, int num, byte[][] xArray, byte[] yBytes);

    /**
     * 多项式求值。
     *
     * @param minBytes   最小多项式系数。
     * @param byteL      l字节长度。
     * @param polynomial 插值多项式。
     * @param xBytes     输入x。
     * @return f(x)。
     */
    static native byte[] singleEvaluate(byte[] minBytes, int byteL, byte[][] polynomial, byte[] xBytes);

    /**
     * 多项式求值。
     *
     * @param minBytes   最小多项式系数。
     * @param byteL      l字节长度。
     * @param polynomial 插值多项式。
     * @param xArray     x_i数组。
     * @return f(x_i)数组。
     */
    static native byte[][] evaluate(byte[] minBytes, int byteL, byte[][] polynomial, byte[][] xArray);
}
