package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIter;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;

import java.util.Arrays;

/**
 * This class used for converting x value in  RNS-Base Q = [q1, q2, ..., qk]  into another RNS-Base M = [m1, m2, ..., mn].
 * The scheme comes from:
 * <p>
 * Section 3.1, equation (2) in the following paper:
 * A full rns variant of fv like somewhat homomorphic encryption schemes(BEHZ). https://eprint.iacr.org/2016/510
 * <p/>
 * <p>
 * The implementation is from:
 * https://github.com/microsoft/SEAL/blob/a0fc0b732f44fa5242593ab488c8b2b3076a5f76/native/src/seal/util/rns.h#L129
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/19
 */
public class BaseConverter {


    /**
     * input base, size of k: q1, q2, ..., qk, prod is q
     */
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
    // n is the size of outBase, k is the size of inBase
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
        for (int i = 0; i < outBase.getSize(); i++) {
            for (int j = 0; j < inBase.getSize(); j++) {
                baseChangeMatrix[i][j] = UintArithmeticSmallMod.moduloUint(
                        inBase.getPuncturedProdArray(j),
                        inBase.getSize(),
                        outBase.getBase(i));
            }
        }
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
        for (int i = 0; i < inBase.getSize(); i++) {
            temp[i] = UintArithmeticSmallMod.multiplyUintMod(
                    in[i],
                    inBase.getInvPuncturedProdModBaseArray(i),
                    inBase.getBase(i)
            );

        }

        // \sum (x_i * \hat{q_i} mod q_i) * q_i^* mod m_j
        // dot product: (n, k) * (k, 1) --> (n, 1)
        for (int i = 0; i < outBase.getSize(); i++) {
            out[i] = UintArithmeticSmallMod.dotProductMod(
                    temp,
                    baseChangeMatrix[i],
                    inBase.getSize(),
                    outBase.getBase(i));
        }
    }

    /**
     * Note that, we should understand the in as follows.
     * Now, we have N value, suppose N = 2, x1, x2, both in [0, q). The in matrix shape is k*N:
     * [x1]_{q_1} [x2]_{q_1}
     * [x1]_{q_2} [x2]_{q_1}
     * .....
     * [x1]_{q_k} [x2]_{q_1};
     * <p>
     * Now convert tht in matrix into a matrix with shape n*N
     * [x1]_{m_1} [x2]_{m_1}
     * [x1]_{m_2} [x2]_{m_2}
     * .....
     * [x1]_{m_n} [x2]_{m_n}.
     * <p>
     * the core is that we treat the matrix as column vector
     *
     * @param in  k * N, N is the value number or coeff count
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

        for (int i = 0; i < inBase.getSize(); i++) {
            if (inBase.getInvPuncturedProdModBaseArray(i).operand == 1) {
                for (int j = 0; j < count; j++) {
                    temp[j][i] = UintArithmeticSmallMod.barrettReduce64(in.getCoeff(i, j), inBase.getBase(i));
                }
            } else {
                for (int j = 0; j < count; j++) {
                    temp[j][i] = UintArithmeticSmallMod.multiplyUintMod(
                            in.getCoeff(i, j),
                            inBase.getInvPuncturedProdModBaseArray(i),
                            inBase.getBase(i));

                }
            }
        }

        for (int i = 0; i < outBase.getSize(); i++) {

            for (int j = 0; j < count; j++) {
                out.setCoeff(i, j, UintArithmeticSmallMod.dotProductMod(
                        temp[j],
                        baseChangeMatrix[i],
                        inBase.getSize(),
                        outBase.getBase(i)));
            }

        }
    }

    /**
     * RnsIter = long[] + index + N + k
     * <p>
     * out 这种表示 整个 long[] 就是一个完整的 RnsIter, 所以不需要其他辅助信息来确定
     *
     * @param in
     * @param inStartIndex
     * @param inCoeffCount
     * @param inCoeffModulusSize
     * @param out
     */
    public void fastConvertArrayRnsIter(
            long[] in,
            int inStartIndex,
            int inCoeffCount,
            int inCoeffModulusSize,
            long[] out,
            int outStartIndex,
            int outCoeffCount,
            int outCoeffModulusSize
    ) {

        assert inCoeffModulusSize == inBase.getSize(); // k
        assert outCoeffModulusSize == outBase.getSize();
        assert inCoeffCount == outCoeffCount;


        int count = inCoeffCount;
        // N * k
        long[][] temp = new long[inCoeffCount][inCoeffModulusSize];
        // 暂时没有把 temp 改为 1D Array 是因为  dotProductMod 的对应改动会比较复杂
        // 后面尝试修改

        //  |x_i * \tilde{q_i}|_{q_i}  i \in [0, k), the result is length-k array
        //  Now we have N x, so need N * k array store

        for (int i = 0; i < inBase.getSize(); i++) {


            if (inBase.getInvPuncturedProdModBaseArray(i).operand == 1) {

                for (int j = 0; j < count; j++) {
                    temp[j][i] = UintArithmeticSmallMod.barrettReduce64(
                            in[inStartIndex + i * inCoeffCount + j], inBase.getBase(i));
                }

            } else {
                for (int j = 0; j < count; j++) {
                    temp[j][i] = UintArithmeticSmallMod.multiplyUintMod(
                            in[inStartIndex + i * inCoeffCount + j],
                            inBase.getInvPuncturedProdModBaseArray(i),
                            inBase.getBase(i));
                }
            }

        }


        for (int i = 0; i < outBase.getSize(); i++) {
            for (int j = 0; j < count; j++) {
                out[outStartIndex + i * count + j] = UintArithmeticSmallMod.dotProductMod(
                        temp[j],
                        baseChangeMatrix[i],
                        inBase.getSize(),
                        outBase.getBase(i));

            }
        }

    }


    /**
     * @param in
     * @param out treat as an array: outBasse.size * outBase.polyModulus
     */
    public void fastConvertArray(RnsIter in, long[] out) {
        // k
        assert in.getRnsBaseSize() == inBase.getSize();
        // N
        assert out.length == outBase.getSize() * in.polyModulusDegree;
//        assert in.getPolyModulusDegree() == out[0].length; // N

        int count = in.getPolyModulusDegree();
        // N * k
        long[][] temp = new long[in.getPolyModulusDegree()][in.getRnsBaseSize()];
        // 暂时没有把 temp 改为 1D Array 是因为  dotProductMod 的对应改动会比较复杂
        // 后面尝试修改

        //  |x_i * \tilde{q_i}|_{q_i}  i \in [0, k), the result is length-k array
        //  Now we have N x, so need N * k array store

        for (int i = 0; i < inBase.getSize(); i++) {
            if (inBase.getInvPuncturedProdModBaseArray(i).operand == 1) {
                for (int j = 0; j < count; j++) {
                    temp[j][i] = UintArithmeticSmallMod.barrettReduce64(in.getCoeff(i, j), inBase.getBase(i));
                }
            } else {

                for (int j = 0; j < count; j++) {
                    temp[j][i] = UintArithmeticSmallMod.multiplyUintMod(
                            in.getCoeff(i, j),
                            inBase.getInvPuncturedProdModBaseArray(i),
                            inBase.getBase(i));
                }

            }
        }

        for (int i = 0; i < outBase.getSize(); i++) {
            for (int j = 0; j < count; j++) {
                out[i * count + j] =
                        UintArithmeticSmallMod.dotProductMod(
                                temp[j],
                                baseChangeMatrix[i],
                                inBase.getSize(),
                                outBase.getBase(i));
            }
        }

    }


    /**
     * convert a value under inBase = [q1, q2, ..., qk] to outBase = {p}
     * note that only used for outBase is one.
     * Ref: Section 2.2 in An Improved RNS Variant of the BFV Homomorphic Encryption Scheme(HPS).
     * todo: 存在测试错误的情况
     *
     * @param in a value under inBase
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

        for (int i = 0; i < inBase.getSize(); i++) {
            xiMulTildeQi[i] = UintArithmeticSmallMod.multiplyUintMod(
                    in[i],
                    inBase.getInvPuncturedProdModBaseArray(i),
                    inBase.getBase(i)
            );
            //
            fraction[i] = (double) xiMulTildeQi[i] / (double) inBase.getBase(i).getValue();
        }

        // compute v, and rounding
        double v = Arrays.stream(fraction).sum();
        long vRounded;
        if (v == 0.5) {
            vRounded = 0;
        } else {
            vRounded = Math.round(v);
        }

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
     * See "An Improved RNS Variant of the BFV Homomorphic Encryption Scheme" (CT-RSA 2019) for details
     *
     * @param in  k * N
     * @param out 1 * N
     */
    public void exactConvertArray(RnsIter in, long[] out) {
        assert in.getPolyModulusDegree() == out.length;
        assert in.getRnsBaseSize() == inBase.getSize();

        // transpose coeffIter of in
        // Temporarily use a two-dimensional array to do it
        long[][] inCoeffs = RnsIter.to2dArray(in);
        long[][] inColumns = new long[in.getPolyModulusDegree()][in.getRnsBaseSize()];
        // transpose
        for (int j = 0; j < in.getPolyModulusDegree(); j++) {
            for (int i = 0; i < in.getRnsBaseSize(); i++) {
                inColumns[j][i] = inCoeffs[i][j];
            }

        }
        // exact convert by column
        for (int i = 0; i < in.getPolyModulusDegree(); i++) {
            out[i] = exactConvert(inColumns[i]);
        }
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


    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("BaseConverter{" +
                "inBase=" + inBase +
                ", outBase=" + outBase);
        sb.append(", baseChangeMatrix=");
        for (long[] changeMatrix : baseChangeMatrix) {
            sb.append(Arrays.toString(changeMatrix));
        }
        sb.append("}");

        return sb.toString();

    }
}
