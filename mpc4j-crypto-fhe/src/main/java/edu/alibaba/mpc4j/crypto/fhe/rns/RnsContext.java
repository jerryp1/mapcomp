package edu.alibaba.mpc4j.crypto.fhe.rns;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * RNS Base Class.
 * Represents a group of co-prime moduli: [q1, q2, ..., qk], q = \prod q_i
 * providing decompose(convert x in Z_q to Z_{q_i} ) and compose (convert x_i \in Z_{q_i} to x \in Z_q)
 * functions.
 *
 * @author Qixian Zhou
 * @date 2023/8/15
 */
public class RnsContext {

    public long[] base;
    // Z_{q_i}, i = 1, 2, ..., k
    public Modulus[] moduli;
    // size of moduli or number of qi
    private int size;
    // q_i^* is the Q/q_i, will be a large number, so use long[] store, representing a base-2^64 number
    public long[][] qStar;
    // \hat{q}_i = (q_i^*)^{-1} mod q_i \in Z_{q_i}
    public long[] qTilde;
    // Shoup representation of \hat{q}_i, used to accelerate multiplication
    public long[] qTildeShoup;
    // q_i^* * \hat{q}_i \in Z, note that here no mod operation, which satisfying (q_i^* * \hat{q}_i)  mod q_i = 1
    public long[][] garner;
    // Q = q1 * q2 * ... * qk, a base-2^64 number
    public long[] baseProd;


    /**
     * @param base [q1, q2, ..., qk]
     */
    public RnsContext(long[] base) {
        assert base.length > 0;
        // assert gcd(qi, qj) = 1
        for (int i = 0; i < base.length; i++) {
            for (int j = 0; j < i; j++) {
                if (Numth.gcd(base[i], base[j]) != 1) {
                    throw new IllegalArgumentException("The moduli are not co-prime, left: " + base[i] + ", right: " + base[j]);
                }
            }
        }
        size = base.length;
        Common.mulSafe(size, size, false);// why need check this in seal?

        this.base = base;
        moduli = new Modulus[size];
        qTilde = new long[size];
        qTildeShoup = new long[size];
        qStar = new long[size][size];
        garner = new long[size][size];
        // 遍历 each qi
        for (int i = 0; i < size; i++) {
            moduli[i] = new Modulus(base[i]);
            // q* = Q/qi = \prod p_j, j \ne i.
            UintArithmetic.multiplyManyUint64Except(base, size, i, qStar[i]);
            // q~ = (Q/qi)^{-1} mod qi
            // first reduce (Q/qi) to [0, qi), then compute inv
            long tmp = UintArithmeticSmallMod.moduloUint(qStar[i], size, moduli[i]);
            qTilde[i] = moduli[i].inv(tmp);
            qTildeShoup[i] = moduli[i].shoup(qTilde[i]);
            // garner = q* * q*^{-1} \in Z,  long[] * long
            UintArithmetic.multiplyUint(qStar[i], qStar[i].length, qTilde[i], garner[i].length, garner[i]);
        }
        // compute baseProd
        baseProd = new long[size];
        UintArithmetic.multiplyUint(qStar[0], size, base[0], size, baseProd);
    }

    /**
     * @param moduli Z_{q_i}, i = 1, 2, ..., k
     */
    public RnsContext(Modulus[] moduli) {
        assert moduli.length > 0;
        // assert gcd(qi, qj) = 1
        for (int i = 0; i < moduli.length; i++) {
            for (int j = 0; j < i; j++) {
                if (Numth.gcd(moduli[i].getValue(), moduli[j].getValue()) != 1) {
                    throw new IllegalArgumentException("The moduli are not co-prime, left: " + moduli[i].getValue() + ", right: " + moduli[j].getValue());
                }
            }
        }
        size = moduli.length;
        Common.mulSafe(size, size, false);// why need check this in seal?
        this.base = Arrays.stream(moduli).mapToLong(Modulus::getValue).toArray();
        this.moduli = moduli;
        qTilde = new long[size];
        qTildeShoup = new long[size];
        qStar = new long[size][size];
        garner = new long[size][size];
        // 遍历 each qi
        for (int i = 0; i < size; i++) {
            // q* = Q/qi = \prod p_j, j \ne i.
            UintArithmetic.multiplyManyUint64Except(base, size, i, qStar[i]);
            // q~ = (Q/qi)^{-1} mod qi
            // first reduce (Q/qi) to [0, qi), then compute inv
            long tmp = UintArithmeticSmallMod.moduloUint(qStar[i], size, moduli[i]);
            qTilde[i] = moduli[i].inv(tmp);
            qTildeShoup[i] = moduli[i].shoup(qTilde[i]);
            // garner = q* * q*^{-1} \in Z,  long[] * long
            UintArithmetic.multiplyUint(qStar[i], qStar[i].length, qTilde[i], garner[i].length, garner[i]);
        }
        // compute baseProd
        baseProd = new long[size];
        UintArithmetic.multiplyUint(qStar[0], size, base[0], size, baseProd);
    }


    /**
     * convert a value to CRT representation.
     *
     * @param value a base-2^64 value
     * @return [a mod q1, a mod q2, ..., a mod qn]
     */
    public long[] decompose(long[] value) {
        return IntStream.range(0, size)
                .parallel()
                .mapToLong(i -> UintArithmeticSmallMod.moduloUint(value, value.length, moduli[i]))
                .toArray();
    }

    /**
     * CRT reconstruct: v = v1 * q1 * q1^{-1} + v2 * q2 * q2^{-1} * ..... mod Q
     *
     * @param crtValue [v1 = v mod q1, v2 = v mod q2, ..., vn = v mod qn]
     * @return long[]  represent a base-2^64 number v in [0, q1 * q2 * ... qn)
     */
    public long[] compose(long[] crtValue) {
        assert crtValue.length == size;
        long[] result = new long[size];
        long[] mulTmp = new long[size];
        // \sum x_i * \hat{q_i} * q_i^* mod Q
        for (int i = 0; i < size; i++) {
            // tmp = x_i * \hat{q_i} mod q_i \in Z_{q_i}
            long tmpProd = moduli[i].mulShoup(crtValue[i], qTilde[i], qTildeShoup[i]);
            // tmp * q_i^* \in Z_Q, because tmp \in  Z_{q_i}, q_i^* \in Z_{Q/q_i}
            UintArithmetic.multiplyUint(qStar[i], qStar[i].length, tmpProd, mulTmp.length, mulTmp);
            // sum(tmp * q_i^*) mod Q \in Z_Q
            UintArithmeticMod.addUintUintMod(result, mulTmp, baseProd, crtValue.length, result);
        }
        return result;
    }


    public long[] getGarner(int index) {
        return garner[index];
    }

    /**
     *
     * @return Q = q1 * q2 * ... * qk by base-2^64 style
     */
    public long[] getBaseProd() {
        return baseProd;
    }


    @Override
    public String toString() {

        return "RnsContext{" +
                " \n moduliU64=" + Arrays.toString(base) +
                ",\n moduli=" + Arrays.deepToString(moduli) +
                ",\n qTilde=" + Arrays.toString(qTilde) +
                ",\n qTildeShoup=" + Arrays.toString(qTildeShoup) +
                ",\n qStar=" + Arrays.deepToString(qStar) +
                ",\n garner=" + Arrays.deepToString(garner) +
                ",\n product=" + Arrays.toString(baseProd) +
                '}';
    }
}


