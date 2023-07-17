package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

/**
 * blazing fast Garbled Cuckoo Table with 3 hash functions utilities.
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
public class H3BlazeGctDovsUtils {
    /**
     * private constructor
     */
    private H3BlazeGctDovsUtils() {
        // empty
    }

    /**
     * weight
     */
    private static final int W = 3;
    /**
     * e^*.
     */
    private static final double E_STAR = 1.223;

    /**
     * Gets left m. The result is shown in Equation (2) of the paper, i.e., w = 3, e^* = 1.223.
     *
     * @param n number of key-value pairs.
     * @return lm = ε_l * n, with lm % Byte.SIZE == 0.
     */
    public static int getLm(int n) {
        double e = getE(n);
        return CommonUtils.getByteLength((int) Math.ceil(e * n)) * Byte.SIZE;
    }

    /**
     * Gets right m. The result is shown in the final part of Section 3.2. That is,
     * <p>
     * 2^α = λ / e = 0.555 * log_2(n) + 0.093 * (w')^3 - 1.010 * (w')^2 + 2.925 * (w') - 0.133
     * </p>
     * for the range 3 ≤ w ≤ 20, where w' = log_2(w). Then, we need to solve for e such that the line going through the
     * phase transition point f = (e^*, -9.2) with slope 2^α passes through the point (e, λ), and computes
     * <p>
     * g^ = λ / ((w - 2) log_2(en)) + λ.
     * </p>
     *
     * @param n number of key-value pairs.
     * @return rm = (1 + ε_r) * log(n) + λ, with with rm % Byte.SIZE == 0.
     */
    public static int getRm(int n) {
        double e = getE(n);
        // g^ = λ / ((w - 2) log_2(en)) + λ.
        int g = (int) Math.ceil(CommonConstants.STATS_BIT_LENGTH / ((W - 2) * DoubleUtils.log2(e * n)));
        return CommonUtils.getByteLength(g + CommonConstants.STATS_BIT_LENGTH) * Byte.SIZE;
    }

    private static double getE(int n) {
        MathPreconditions.checkPositive("n", n);
        double logN = DoubleUtils.log2(n);
        double logW = DoubleUtils.log2(W);
        double alpha = 0.555 * logN + 0.093 * Math.pow(logW, 3) - 1.010 * Math.pow(logW, 2) + 2.925 * logW - 0.133;
        double twoExpAlpha = Math.pow(2, alpha);
        // f(x) = 2^α * x + b that passes through the point (e^*, -9.2), b = -9.2 - 2^α * e^*
        double b = -9.2 - twoExpAlpha * E_STAR;
        // find e such that λ = 2^α * e + b
        return (CommonConstants.STATS_BIT_LENGTH - b) / twoExpAlpha;
    }
}
