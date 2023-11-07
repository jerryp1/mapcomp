package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIter;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintArithmeticSmallMod;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * This class used for converting x value in RNS-Base Q = [q1, q2, ..., qk] into another RNS-Base M = [m1, m2, ..., mn].
 * The scheme comes from:
 * <p>
 * Section 3.1, equation (2) in the following paper:
 * A full rns variant of fv like somewhat homomorphic encryption schemes(BEHZ). https://eprint.iacr.org/2016/510
 * <p/>
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/a0fc0b732f44fa5242593ab488c8b2b3076a5f76/native/src/seal/util/rns.h#L129
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/19
 */
public class BaseConverter {
    /**
     * input base, size of k: q1, q2, ..., qk, prod is q
     */
    private final RnsBase inBase;
    /**
     * output base, size of n: m1, m2, ...., mk, prod is m
     */
    private final RnsBase outBase;
    /**
     * baseChangeMatrix[i][j] = q_j^* mod m_i, q_j^* = q/q_j is a multi-precision integer,
     * m_i is up to 61-bit, so use 2D-array is enough
     * the matrix as follows:
     *   [ q_1^* mod m_1, q_2^* mod m_1, ...,  q_k^* mod m_1]
     *   [ q_1^* mod m_2, q_2^* mod m_2, ...,  q_k^* mod m_2]
     *   ............
     *   [ q_1^* mod m_n, q_2^* mod m_n, ...,  q_k^* mod m_n]
     * the shape is: n * k, n is the size of outBase, k is the size of inBase
     */
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

    /**
     * initialize base converter.
     */
    private void initialize() {
        Common.mulSafe(inBase.getSize(), outBase.getSize(), false);
        // n * k
        baseChangeMatrix = new long[outBase.getSize()][inBase.getSize()];
        for (int i = 0; i < outBase.getSize(); i++) {
            for (int j = 0; j < inBase.getSize(); j++) {
                baseChangeMatrix[i][j] = UintArithmeticSmallMod.moduloUint(
                    inBase.getPuncturedProdArray(j), inBase.getSize(), outBase.getBase(i)
                );
            }
        }
    }

    /**
     * fast base convert, Ref: Section 3.1, equation(2).
     *
     * @param in  a value x in [0, q) under inBase: [x_1, x_2, ...,x_k]^T, x_j = [x] mod {q_j}.
     * @param out the same x in [0, q) under outBase: [x_1, x_2, ..., x_n]^T x_i = [x] mod {m_i}.
     */
    public void fastConvert(long[] in, long[] out) {
        assert in.length == inBase.getSize();
        assert out.length == outBase.getSize();
        // temp = x_i * \hat{q_i} mod q_i
        long[] temp = IntStream.range(0, inBase.getSize())
            .mapToLong(i ->
                UintArithmeticSmallMod.multiplyUintMod(
                    in[i], inBase.getInvPuncturedProdModBaseArray(i), inBase.getBase(i)
                )
            )
            .toArray();
        // \sum (x_i * \hat{q_i} mod q_i) * q_i^* mod m_j
        // dot product: (n, k) * (k, 1) --> (n, 1)
        for (int i = 0; i < outBase.getSize(); i++) {
            out[i] = UintArithmeticSmallMod.dotProductMod(temp, baseChangeMatrix[i], inBase.getSize(), outBase.getBase(i));
        }
    }

    /**
     * Note that, we should understand input as follows.
     * Suppose N = 2, denote as x1, x2 \in [0, q). The input matrix shape is k*N:
     * [x1]_{q_1} [x2]_{q_1}
     * [x1]_{q_2} [x2]_{q_2}
     * .....
     * [x1]_{q_k} [x2]_{q_2};
     * Now convert input matrix into a matrix with shape n*N
     * [x1]_{m_1} [x2]_{m_1}
     * [x1]_{m_2} [x2]_{m_2}
     * .....
     * [x1]_{m_n} [x2]_{m_n}.
     * the core is that we treat the matrix as column vector
     *
     * @param in  k * N, N is the value number or coeff count.
     * @param out n * N, N is the value number or coeff count.
     */
    public void fastConvertArray(RnsIter in, RnsIter out) {
        assert in.getRnsBaseSize() == inBase.getSize();
        assert out.getRnsBaseSize() == outBase.getSize();
        assert in.getPolyModulusDegree() == out.getPolyModulusDegree();
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
                        in.getCoeff(i, j), inBase.getInvPuncturedProdModBaseArray(i), inBase.getBase(i)
                    );
                }
            }
        }
        for (int i = 0; i < outBase.getSize(); i++) {
            for (int j = 0; j < count; j++) {
                out.setCoeff(
                    i,
                    j,
                    UintArithmeticSmallMod.dotProductMod(temp[j], baseChangeMatrix[i], inBase.getSize(), outBase.getBase(i))
                );
            }
        }
    }

    /**
     * fast base convert.
     *
     * @param in                  an array, the length is at least inCoeffCount * inBase.size.
     * @param inStartIndex        start index of input array.
     * @param inCoeffCount        coeff count.
     * @param inCoeffModulusSize  input coeff modulus size.
     * @param out                 an array, the length is at least outCoeffCount * outBase.size.
     * @param outStartIndex       start index of output array.
     * @param outCoeffCount       coeff count.
     * @param outCoeffModulusSize output coeff modulus size.
     */
    public void fastConvertArrayRnsIter(long[] in, int inStartIndex, int inCoeffCount, int inCoeffModulusSize,
                                        long[] out, int outStartIndex, int outCoeffCount, int outCoeffModulusSize) {
        assert inCoeffModulusSize == inBase.getSize();
        assert outCoeffModulusSize == outBase.getSize();
        assert inCoeffCount == outCoeffCount;
        // N * k
        long[][] temp = new long[inCoeffCount][inCoeffModulusSize];
        //  |x_i * \tilde{q_i}|_{q_i} i \in [0, k), the result is length-k array
        //  Now we have N x, so need N * k array store
        for (int i = 0; i < inBase.getSize(); i++) {
            if (inBase.getInvPuncturedProdModBaseArray(i).operand == 1) {
                for (int j = 0; j < inCoeffCount; j++) {
                    temp[j][i] = UintArithmeticSmallMod.barrettReduce64(
                        in[inStartIndex + i * inCoeffCount + j], inBase.getBase(i)
                    );
                }
            } else {
                for (int j = 0; j < inCoeffCount; j++) {
                    temp[j][i] = UintArithmeticSmallMod.multiplyUintMod(
                        in[inStartIndex + i * inCoeffCount + j], inBase.getInvPuncturedProdModBaseArray(i), inBase.getBase(i)
                    );
                }
            }
        }
        for (int i = 0; i < outBase.getSize(); i++) {
            for (int j = 0; j < inCoeffCount; j++) {
                out[outStartIndex + i * inCoeffCount + j] = UintArithmeticSmallMod.dotProductMod(
                    temp[j], baseChangeMatrix[i], inBase.getSize(), outBase.getBase(i)
                );
            }
        }
    }

    /**
     * fast base convert.
     *
     * @param in  k * N, N is the value number or coeff count, k is the size of the input base.
     * @param out treat as an array, the length is outBase.size * N
     */
    public void fastConvertArray(RnsIter in, long[] out) {
        assert in.getRnsBaseSize() == inBase.getSize();
        assert out.length == outBase.getSize() * in.polyModulusDegree;
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
                        in.getCoeff(i, j), inBase.getInvPuncturedProdModBaseArray(i), inBase.getBase(i)
                    );
                }
            }
        }
        for (int i = 0; i < outBase.getSize(); i++) {
            for (int j = 0; j < count; j++) {
                out[i * count + j] = UintArithmeticSmallMod.dotProductMod(
                    temp[j], baseChangeMatrix[i], inBase.getSize(), outBase.getBase(i)
                );
            }
        }
    }

    /**
     * convert a value under inBase = [q1, q2, ..., qk] to outBase = {p}
     * Ref: Section 2.2 in An Improved RNS Variant of the BFV Homomorphic Encryption Scheme(HPS).
     *
     * @param in a value under inBase.
     * @return x mod m, m is the outBase.
     */
    public long exactConvert(long[] in) {
        assert in.length == inBase.getSize();
        // the size of out base muse be one
        if (outBase.getSize() != 1) {
            throw new IllegalArgumentException("out base in exact_convert_array must be one");
        }
        Modulus p = outBase.getBase(0);
        if (inBase.getSize() > 1) {
            // v = round( \sum ([x_i * \tilde{q_i}]_{q_i} / q_i))
            // 1. [x_i * \tilde{q_i}]_{q_i}, and the fraction
            double[] fraction = new double[inBase.getSize()];
            long[] xiMulTildeQi = new long[inBase.getSize()];
            for (int i = 0; i < inBase.getSize(); i++) {
                xiMulTildeQi[i] = UintArithmeticSmallMod.multiplyUintMod(
                    in[i], inBase.getInvPuncturedProdModBaseArray(i), inBase.getBase(i)
                );
                fraction[i] = (double) xiMulTildeQi[i] / (double) inBase.getBase(i).getValue();
            }
            // compute v, and rounding
            double v = Arrays.stream(fraction).sum();
            long vRounded = Double.compare(0.5, v) == 0 ? 0 : Math.round(v);
            long qModP = UintArithmeticSmallMod.moduloUint(inBase.getBaseProd(), inBase.getSize(), p);
            // compute \sum ([x_i * \tilde{q_i}]_{q_i} * q_i^*)
            // matrix is 1 * k
            long sumModP = UintArithmeticSmallMod.dotProductMod(xiMulTildeQi, baseChangeMatrix[0], inBase.getSize(), p);
            long vMulQprodModP = UintArithmeticSmallMod.multiplyUintMod(vRounded, qModP, p);
            // [\sum ([x_i * \tilde{q_i}]_{q_i} * q_i^*) - v * q]_p
            return UintArithmeticSmallMod.subUintMod(sumModP, vMulQprodModP, p);
        } else {
            return UintArithmeticSmallMod.moduloUint(in, 1, p);
        }
    }

    /**
     * exact convert array.
     * See "An Improved RNS Variant of the BFV Homomorphic Encryption Scheme" (CT-RSA 2019) for details
     *
     * @param in  k * N rns
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

    /**
     * get input base size.
     *
     * @return return input base size.
     */
    public int getInputBaseSize() {
        return inBase.getSize();
    }

    /**
     * get output base size.
     *
     * @return return output base size.
     */
    public int getOutputBaseSize() {
        return outBase.getSize();
    }

    /**
     * get input base.
     *
     * @return return input base.
     */
    public RnsBase getInputBase() {
        return inBase;
    }

    /**
     * get output base.
     *
     * @return return output base.
     */
    public RnsBase getOutputBase() {
        return outBase;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BaseConverter{" + "inBase=").append(inBase).append(", outBase=").append(outBase);
        sb.append(", baseChangeMatrix=");
        for (long[] changeMatrix : baseChangeMatrix) {
            sb.append(Arrays.toString(changeMatrix));
        }
        sb.append("}");
        return sb.toString();
    }
}
