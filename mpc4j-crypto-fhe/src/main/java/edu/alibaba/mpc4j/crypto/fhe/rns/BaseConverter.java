package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIter;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * This class used for converting x value in  RNS-Base Q = [q1, q2, ..., qk]  into another RNS-Base M = [m1, m2, ..., mn].
 * Ref: Section 3.1, equation (2) in the following paper:
 * A Full RNS Variant of FV like Somewhat Homomorphic Encryption Schemes (BEHZ).
 *
 * @author Qixian Zhou
 * @date 2023/8/19
 */
public class BaseConverter {

    // input base, size of k: q1, q2, ..., qk, prod is q
    private RnsBase inBase;

    // output base, size of n: m1, m2, ...., mk, prod is m
    private RnsBase outBase;

    // baseChangeMatrix[i][j] = q_j^* mod m_i, q_j^* = q/q_j is a multi-precision integer, m_i is up to 61-bit, so use 2D-array is enough
    // the matrix as follow:
    //         [ q_1^* mod m_1, q_2^* mod m_1, ...,  q_k^* mod m_1]
    //         [ q_1^* mod m_2, q_2^* mod m_2, ...,  q_k^* mod m_2]
    //         ............
    //         [ q_1^* mod m_n, q_2^* mod m_n, ...,  q_k^* mod m_n]
    // the shape is: n * k
    private long[][] baseChangeMatrix;


    /**
     * @param inBase  input RnsBase Q = [q1, q2, ..., qk]
     * @param outBase output RnsBase M = [m1, m2, ..., mn].
     */
    public BaseConverter(RnsBase inBase, RnsBase outBase) {

        this.inBase = inBase;
        this.outBase = outBase;

        initialize();
    }


    private void initialize() {

        Common.mulSafe(inBase.getSize(), outBase.getSize(), false);
        // n * k
        baseChangeMatrix = new long[outBase.getSize()][inBase.getSize()];
        /**
         compute the matrix as follow:
         [ q_1^* mod m_1, q_2^* mod m_1, ...,  q_k^* mod m_1]
         [ q_1^* mod m_2, q_2^* mod m_2, ...,  q_k^* mod m_2]
         ............
         [ q_1^* mod m_n, q_2^* mod m_n, ...,  q_k^* mod m_n]
         */
        IntStream.range(0, outBase.getSize()).parallel().forEach(
                i -> {
                    IntStream.range(0, inBase.getSize()).parallel().forEach(
                            j -> {
                                baseChangeMatrix[i][j] = UintArithmeticSmallMod.moduloUint(
                                        inBase.getPuncturedProdArray(j),
                                        inBase.getSize(),
                                        outBase.getBase(i));
                            }
                    );
                }
        );
//
//        for (int i = 0; i < outBase.getSize(); i++) {
//            for (int j = 0; j < inBase.getSize(); j++) {
//                baseChangeMatrix[i][j] = UintArithmeticSmallMod.moduloUint(
//                        inBase.getPuncturedProdArray(j),
//                        inBase.getSize(),
//                        outBase.getBase(i));
//            }
//        }
    }


    /**
     * Ref: Section 3.1, equation(2).
     *
     * @param in  a value x in [0, q) under inBase: [x_1, x_2, ...,x_k]^T, x_j = [x]_{q_j}, can treat as a column vector
     * @param out the same x in [0, q) udner outBase: [x_1, x_2, ..., x_n]^T x_i = [x]_{m_i}, can treat as a column vector
     */
    public void fastConvert(long[] in, long[] out) {

        assert in.length == inBase.getSize();
        assert out.length == outBase.getSize();

        long[] temp = new long[inBase.getSize()];
        //  temp = x_i * \hat{q_i} mod q_i
        IntStream.range(0, inBase.getSize()).parallel().forEach(
                i -> {

                    temp[i] = UintArithmeticSmallMod.multiplyUintMod(
                            in[i],
                            inBase.getInvPuncturedProdModBaseArray(i),
                            inBase.getBase(i)
                            );
                });
        // \sum (x_i * \hat{q_i} mod q_i) * q_i^* mod m_j
        // dot product: (n, k) * (k, 1) --> (n, 1)
        IntStream.range(0, outBase.getSize()).parallel().forEach(
                i -> {
                    out[i] = UintArithmeticSmallMod.dotProductMod(
                            temp,
                            baseChangeMatrix[i],
                            inBase.getSize(),
                            outBase.getBase(i));
                });
    }

    /**
     * Note that, we should understand the in as follows.
     * Now, we have N value, suppose N = 2, x1, x2, both in [0, q). The in matrix shape is k*N:
     * [x1]_{q_1} [x2]_{q_1}
     * [x1]_{q_2} [x2]_{q_1}
     * .....
     * [x1]_{q_k} [x2]_{q_1};
     *
     * Now convert tht in matrix into a matrix with shape n*N
     * [x1]_{m_1} [x2]_{m_1}
     * [x1]_{m_2} [x2]_{m_2}
     * .....
     * [x1]_{m_n} [x2]_{m_n}.
     *
     * the core is that we treat the matrix as column vector
     *
     * @param in k * N, N is the value number or coeff count
     * @param out n * N, N is the value number or coeff count
     */
    public void fastConvertArray(RnsIter in, RnsIter out) {

        assert in.getRnsBaseSize() == inBase.getSize(); // k
        assert out.getRnsBaseSize() == outBase.getSize(); // n
        assert in.getPolyModulusDegree() == out.getPolyModulusDegree(); // N

        int count = in.getPolyModulusDegree();
        // N * k
        long[][] temp = new long[in.getPolyModulusDegree()][in.getRnsBaseSize()];
        //  |x_i * \tilde{q_i}|_{q_i}  i \in [0, k), the result is length-k array
        //  Now we have N x, so need N * k array store
        IntStream.range(0, inBase.getSize()).parallel().forEach(
                i -> {
                    if (inBase.getInvPuncturedProdModBaseArray(i).operand == 1) {
                        // no need mul
                        IntStream.range(0, count).parallel().forEach(
                                j -> temp[j][i] = UintArithmeticSmallMod.barrettReduce64(in.getCoeff(i, j), inBase.getBase(i)));
                    }else {
                        // need mul
                        IntStream.range(0, count).parallel().forEach(
                                j -> temp[j][i] = UintArithmeticSmallMod.multiplyUintMod(
                                        in.getCoeff(i, j),
                                        inBase.getInvPuncturedProdModBaseArray(i),
                                        inBase.getBase(i)));
                    }
                }
            );


        IntStream.range(0, outBase.getSize()).parallel().forEach(
                i -> {
                    IntStream.range(0, count).parallel().forEach(
                            j -> {
                                out.setCoeff(i, j,
                                        UintArithmeticSmallMod.dotProductMod(
                                                temp[j],
                                                baseChangeMatrix[i],
                                                inBase.getSize(),
                                                outBase.getBase(i))
                                        );
                            }
                    );
                }
        );
    }

    public void fastConvertArray(RnsIter in, long[][] out) {

        assert in.getRnsBaseSize() == inBase.getSize(); // k
        assert out.length == outBase.getSize(); // n
        assert in.getPolyModulusDegree() == out[0].length; // N

        int count = in.getPolyModulusDegree();
        // N * k
        long[][] temp = new long[in.getPolyModulusDegree()][in.getRnsBaseSize()];
        //  |x_i * \tilde{q_i}|_{q_i}  i \in [0, k), the result is length-k array
        //  Now we have N x, so need N * k array store
        IntStream.range(0, inBase.getSize()).parallel().forEach(
                i -> {
                    if (inBase.getInvPuncturedProdModBaseArray(i).operand == 1) {
                        // no need mul
                        IntStream.range(0, count).parallel().forEach(
                                j -> temp[j][i] = UintArithmeticSmallMod.barrettReduce64(in.getCoeff(i, j), inBase.getBase(i)));
                    }else {
                        // need mul
                        IntStream.range(0, count).parallel().forEach(
                                j -> temp[j][i] = UintArithmeticSmallMod.multiplyUintMod(
                                        in.getCoeff(i, j),
                                        inBase.getInvPuncturedProdModBaseArray(i),
                                        inBase.getBase(i)));
                    }
                }
        );


        IntStream.range(0, outBase.getSize()).parallel().forEach(
                i -> {
                    IntStream.range(0, count).parallel().forEach(
                            j -> {
                                out[i][j] =
                                        UintArithmeticSmallMod.dotProductMod(
                                                temp[j],
                                                baseChangeMatrix[i],
                                                inBase.getSize(),
                                                outBase.getBase(i));
                            }
                    );
                }
        );

    }


    /**
     * Convert input RnsIter to output RnsIter
     * @param in input RnsIter, an array under RnsBase Q
     * @param out output RnsIter, an array under RnsBase M
     */


    /**
     * convert a value under inBase = [q1, q2, ..., qk] to outBase = {p}
     * note that only used for outBase is one.
     * Ref: Section 2.2 in An Improved RNS Variant of the BFV Homomorphic Encryption Scheme(HPS).
     *
     * @param in  a value under inBase
     */
    public long exactConvert(long[] in) {
        assert in.length == inBase.getSize();

        if (outBase.getSize() != 1) {
            throw new IllegalArgumentException("out base in exact_convert_array must be one");
        }

        // v = round( \sum ([x_i * \tilde{q_i}]_{q_i} / q_i))
        // 1. [x_i * \tilde{q_i}]_{q_i}, and the fraction
        long[] xiMulTildeQi = new long[inBase.getSize()];
        double[] fraction = new double[inBase.getSize()];
        IntStream.range(0, inBase.getSize()).parallel().forEach(
                i -> {
                    xiMulTildeQi[i] = UintArithmeticSmallMod.multiplyUintMod(
                            in[i],
                            inBase.getInvPuncturedProdModBaseArray(i),
                            inBase.getBase(i)
                    );
                    //
                    fraction[i] = (double) xiMulTildeQi[i] / (double) inBase.getBase(i).getValue();
                }
        );

        // compute v, and rounding
        double v = Arrays.stream(fraction).sum();
        long vRounded;
        if (v == 0.5) {
            vRounded = 0;
        }else{
            vRounded = Math.round(v);
        }
        System.out.println("v: " + v + ", round(v): " + vRounded);


        Modulus p = outBase.getBase(0);
        long qModP = UintArithmeticSmallMod.moduloUint(inBase.getBaseProd(), inBase.getSize(), p);
        // compute \sum ([x_i * \tilde{q_i}]_{q_i} * q_i^*)
        // matrix is 1 * k
        long sumModP = UintArithmeticSmallMod.dotProductMod(xiMulTildeQi, baseChangeMatrix[0], inBase.getSize(), p);
        long vMulQModP = UintArithmeticSmallMod.multiplyUintMod(vRounded, qModP, p);
        // [\sum ([x_i * \tilde{q_i}]_{q_i} * q_i^*) - v * q]_p
        return UintArithmeticSmallMod.subUintMod(sumModP, vMulQModP, p);
    }


    /**
     *
     * @param in k * N
     * @param out 1 * N
     */
    public void exactConvertArray(RnsIter in, long[] out) {
        assert in.getPolyModulusDegree() == out.length;
        assert in.getRnsBaseSize() == inBase.getSize();

        // transpose coeffIter of in
        long[][] inCoeffs = in.getCoeffIter();
        long[][] inColumns = new long[in.getPolyModulusDegree()][in.getRnsBaseSize()];
        // transpose
        IntStream.range(0, in.getPolyModulusDegree()).parallel().forEach(
                j -> {
                    IntStream.range(0, in.getRnsBaseSize()).parallel().forEach(
                            i -> inColumns[j][i] = inCoeffs[i][j]
                    );
                }
        );
        // exact convert by column
        IntStream.range(0, in.getPolyModulusDegree()).parallel().forEach(
                i -> out[i] = exactConvert(inColumns[i])
        );
    }












    public int getInputBaseSize() {
        return inBase.getSize();
    }

    public int getOutputBaseSize() {
        return outBase.getSize();
    }

    public RnsBase getInputBase() {
        return inBase;
    }

    public RnsBase getOutputBase() {
        return outBase;
    }
}
