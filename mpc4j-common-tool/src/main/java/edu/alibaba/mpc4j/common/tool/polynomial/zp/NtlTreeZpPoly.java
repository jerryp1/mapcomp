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
public class NtlTreeZpPoly extends RingsTreeZpPoly {

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
