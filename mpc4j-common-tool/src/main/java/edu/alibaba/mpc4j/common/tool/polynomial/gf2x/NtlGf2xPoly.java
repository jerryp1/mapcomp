package edu.alibaba.mpc4j.common.tool.polynomial.gf2x;

import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eManager;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2x.Gf2xPolyFactory.Gf2xPolyType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.security.SecureRandom;

/**
 * NTL实现的GF(2^l)多项式运算。
 *
 * @author Weiran Liu
 * @date 2021/12/11
 */
public class NtlGf2xPoly implements Gf2xPoly {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * 随机状态
     */
    private final SecureRandom secureRandom;
    /**
     * GF(2^l)不可约多项式
     */
    private final byte[] minBytes;
    /**
     * l的字节长度
     */
    private final int byteL;

    /**
     * 初始化NTL实现的GF(2^l)多项式运算。
     *
     * @param l GF(2^l)有限域比特长度。
     */
    public NtlGf2xPoly(int l) {
        assert l > 0 && l % Byte.SIZE == 0;
        byteL = l / Byte.SIZE;
        FiniteField<UnivariatePolynomialZp64> finiteField = Gf2eManager.getFiniteField(l);
        // 设置不可约多项式，系数个数为l + 1
        int minNum = l + 1;
        int minByteNum = CommonUtils.getByteLength(minNum);
        int minRoundBytes = minByteNum * Byte.SIZE;
        minBytes = new byte[minByteNum];
        UnivariatePolynomialZp64 minimalPolynomial = finiteField.getMinimalPolynomial();
        for (int i = 0; i <= minimalPolynomial.degree(); i++) {
            boolean coefficient = minimalPolynomial.get(i) != 0L;
            BinaryUtils.setBoolean(minBytes, minRoundBytes - 1 - i, coefficient);
        }
        secureRandom = new SecureRandom();
    }

    @Override
    public Gf2xPolyType getGf2xPolyType() {
        return Gf2xPolyType.NTL;
    }

    @Override
    public int getByteL() {
        return byteL;
    }

    @Override
    public byte[][] interpolate(int num, byte[][] xArray, byte[][] yArray) {
        assert xArray.length == yArray.length;
        // 不要求至少有1个插值点，只要求总数量大于1
        assert num > 1 && xArray.length <= num;
        for (int i = 0; i < xArray.length; i++) {
            assert xArray[i].length == byteL;
            assert yArray[i].length == byteL;
        }
        // 调用本地函数完成插值
        return NtlNativeGf2xPoly.interpolate(minBytes, byteL, num, xArray, yArray);
    }

    @Override
    public byte[][] rootInterpolate(int num, byte[][] xArray, byte[] yBytes) {
        // 不要求至少有1个插值点，只要求总数量大于1
        assert num > 1 && xArray.length <= num;
        if (xArray.length == 0) {
            // 返回随机多项式
            byte[][] coefficients = new byte[num + 1][byteL];
            for (int index = 0; index < num; index++) {
                secureRandom.nextBytes(coefficients[index]);
            }
            // 将最高位设置为1
            coefficients[num][byteL - 1] = (byte)0x01;
            return coefficients;
        }
        // 如果有插值数据，则调用本地函数完成插值
        for (byte[] xBytes : xArray) {
            assert xBytes.length == byteL;
        }
        return NtlNativeGf2xPoly.rootInterpolate(minBytes, byteL, num, xArray, yBytes);
    }

    @Override
    public byte[] evaluate(byte[][] coefficients, byte[] xBytes) {
        assert coefficients.length > 1;
        for (byte[] coefficient : coefficients) {
            assert coefficient.length == byteL;
        }
        assert xBytes.length == byteL;
        return NtlNativeGf2xPoly.singleEvaluate(minBytes, byteL, coefficients, xBytes);
    }

    @Override
    public byte[][] evaluate(byte[][] coefficients, byte[][] xArray) {
        for (byte[] x : xArray) {
            assert x.length == byteL;
        }
        assert coefficients.length > 1;
        for (byte[] coefficient : coefficients) {
            assert coefficient.length == byteL;
        }
        return NtlNativeGf2xPoly.evaluate(minBytes, byteL, coefficients, xArray);
    }
}
