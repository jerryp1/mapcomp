package edu.alibaba.mpc4j.s2pc.pir.keyword;

import java.util.Vector;
import java.util.stream.IntStream;

/**
 * 多项式工具类。
 *
 * @author Liqiang Peng
 * @date 2022/5/25
 */
public class PolynomialUtils {
    /**
     * 模指数运算。
     *
     * @param base     底数。
     * @param exponent 指数。
     * @param modulus  模数。
     * @return 底数^指数 % 模数。
     */
    public static long modExp(long base, long exponent, long modulus) {
        if (exponent == 0) {
            return 1;
        }
        long temp = modExp(base, exponent/2, modulus);
        long result = mulMod(temp, temp, modulus);
        return exponent % 2 == 0 ? result : mulMod(result, base, modulus);
    }

    /**
     * 模乘运算。
     *
     * @param a 乘数。
     * @param b 乘数。
     * @param c 模数。
     * @return 乘数*乘数 % 模数。
     */
    public static long mulMod(long a, long b, long c) {
        assert a < (1L << 32) & b < (1L << 32);
        return (a*b) % c;
    }

    /**
     * 模逆运算。
     *
     * @param x       输入。
     * @param modulus 模数。
     * @return 输入^(-1) % 模数。
     */
    public static long modInv(long x, long modulus) {
        return modExp(x, modulus - 2, modulus);
    }

    /**
     * 从根计算多项式系数。
     *
     * @param roots   根。
     * @param modulus 模数。
     * @return 多项式系数。
     */
    public static long[] polynomialFromRoots(Vector<Long> roots, long modulus) {
        long[] coeffs = new long[roots.size() + 1];
        coeffs[0] = 1L;
        for (int i = 1; i < coeffs.length; i++) {
            coeffs[i] = 0L;
        }
        for (int i = 0; i < roots.size(); i++) {
            // multiply coeffs by (x - root)
            long negRoot = modulus - (roots.get(i) % modulus);
            for (int j = i + 1; j > 0; j--) {
                coeffs[j] = (coeffs[j-1] + mulMod(negRoot, coeffs[j], modulus)) % modulus;
            }
            coeffs[0] = mulMod(coeffs[0], negRoot, modulus);
        }
        return coeffs;
    }

    /**
     * 计算幂次方。
     *
     * @param base      底数。
     * @param modulus   模数。
     * @param exponents 指数。
     * @return 幂次方。
     */
    public static long[][] computePowers(long[] base, long modulus, int[] exponents) {
        long[][] result = new long[exponents.length][];
        assert exponents[0] == 1;
        result[0] = base;
        for (int i = 1; i < exponents.length; i++) {
            long[] temp = new long[base.length];
            for (int j = 0; j < base.length; j++) {
                temp[j] = modExp(base[j], exponents[i], modulus);
            }
            result[i] = temp;
        }
        return result;
    }

    /**
     * 从点计算多项式系数。
     *
     * @param xAxis   x轴坐标。
     * @param yAxis   y轴坐标。
     * @param modulus 模数。
     * @return 多项式系数。
     */
    public static long[] polynomialFromPoints(Vector<Long> xAxis, Vector<Long> yAxis, long modulus) {
        for (int i = 0; i < xAxis.size(); i++) {
            for (int j = i+1; j < xAxis.size(); j++) {
                assert !xAxis.get(i).equals(xAxis.get(j));
            }
        }
        assert xAxis.size() == yAxis.size();
        long[] coeffs = new long[xAxis.size()];
        if (xAxis.isEmpty()) {
            return coeffs;
        }
        // at iteration i of the loop, basis contains the coefficients of the basis
        // polynomial (x - xs[0]) * (x - xs[1]) * ... * (x - xs[i - 1])
        long[] basis = new long[xAxis.size()];
        basis[0] = 1L;
        IntStream.range(1, basis.length).forEach(i -> basis[i] = 0L);
        // at iteration i of the loop, ddif[j] contains the divided difference
        // [ys[j], ys[j + 1], ..., ys[j + i]]. thus initially, when i = 0,
        // ddif[j] = [ys[j]] = ys[j]
        long[] ddif = yAxis.stream().mapToLong(yAxi -> yAxi).toArray();
        for (int i = 0; i < xAxis.size(); i++) {
            for (int j = 0; j < i + 1; j++) {
                long temp = (coeffs[j] + mulMod(ddif[0], basis[j], modulus)) % modulus;
                coeffs[j] = temp;
            }
            if (i < xAxis.size() - 1) {
                // update basis: multiply it by (x - xs[i])
                long negX = modulus - (xAxis.get(i) % modulus);
                for (int j = i + 1; j > 0; j--) {
                    long temp = (basis[j-1] + mulMod(negX, basis[j], modulus)) % modulus;
                    basis[j] = temp;
                }
                basis[0] = mulMod(basis[0], negX, modulus);
                // update ddif: compute length-(i + 1) divided differences
                for (int j = 0; j + i + 1 < xAxis.size(); j++) {
                    // dd_{j,j+i+1} = (dd_{j+1, j+i+1} - dd_{j, j+i}) / (x_{j+i+1} - x_j)
                    long num = (ddif[j+1] - ddif[j] + modulus) % modulus;
                    long den = (xAxis.get(j+i+1) - xAxis.get(j) + modulus) % modulus;
                    ddif[j] = mulMod(num, modInv(den, modulus), modulus);
                }
            }
        }
        return coeffs;
    }
}
