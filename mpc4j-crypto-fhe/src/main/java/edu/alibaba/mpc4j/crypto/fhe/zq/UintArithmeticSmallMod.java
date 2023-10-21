package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;

import java.util.Arrays;

/**
 * Uint arithmetic small mod, the modulus is at most 61-bit value.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/uintarithsmallmod.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/5
 */
public class UintArithmeticSmallMod {

    /**
     * Compute \sum a_i * b_i mod modulus, i\in [0, count), a is operand1, b is operand2.
     *
     * @param operand1 an array, each value is a 64-bit value
     * @param operand2 an array, each value is a 64-bit value
     * @param count    count of 64-bit value used in operand1 and operand2
     * @param modulus  a Modulus object
     * @return \sum a_i * b_i mod modulus, i\in [0, count)
     */
    public static long dotProductMod(long[] operand1, long[] operand2, int count, Modulus modulus) {
        assert count >= 0;

        long[] accumulator = new long[2];
        switch (count) {
            case 0:
                return 0;
            case 1:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 1);
                break;
            case 2:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 2);
                break;
            case 3:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 3);
                break;
            case 4:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 4);
                break;
            case 5:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 5);
                break;
            case 6:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 6);
                break;
            case 7:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 7);
                break;
            case 8:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 8);
                break;
            case 9:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 9);
                break;
            case 10:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 10);
                break;
            case 11:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 11);
                break;
            case 12:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 12);
                break;
            case 13:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 13);
                break;
            case 14:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 14);
                break;
            case 15:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 15);
                break;
            case 16:
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 16);
                break;
            default:

                long[] c1 = Arrays.copyOfRange(operand1, 16, count);
                long[] c2 = Arrays.copyOfRange(operand2, 16, count);
                accumulator[0] = dotProductMod(c1, c2, count - 16, modulus);
                UintArithmetic.multiplyAccumulateUint64(operand1, 0, operand2, 0, accumulator, 16);
                break;
        }
        return barrettReduce128(accumulator, modulus);
    }

    /**
     * Compute (a^e mod modulus).
     * Correctness: Follows the condition of barrett_reduce_128.
     *
     * @param operand  a 64-bit value
     * @param exponent a 64-bit value
     * @param modulus  a Modulus object
     * @return a^e mod modulus
     */
    public static long exponentUintMod(long operand, long exponent, Modulus modulus) {

        assert !modulus.isZero();
        assert operand < modulus.getValue();

        if (exponent == 0) {
            return 1;
        }
        if (exponent == 1) {
            return operand;
        }

        // Perform binary exponentiation.
        long power = operand;
        long product;
        long intermediate = 1;

        // Initially: power = operand and intermediate = 1, product is irrelevant.
        while (true) {
            if ((exponent & 1) > 0) {
                product = multiplyUintMod(power, intermediate, modulus);
                // update intermediate
                intermediate = product;
            }
            exponent >>>= 1;
            if (exponent == 0) {
                break;
            }
            product = multiplyUintMod(power, power, modulus);
            // update power
            power = product;
        }
        return intermediate;
    }


    /**
     * Compute operand^{-1} mod modulus and store it in result[0].
     *
     * @param operand a base-2^64 value
     * @param modulus a Modulus object
     * @param result  result[0] store operand^{-1} mod modulus
     * @return if under modulus operand's invert exists, return true, otherwise return false.
     */
    public static boolean tryInvertUintMod(long operand, Modulus modulus, long[] result) {
        return Numth.tryInvertUintMod(operand, modulus.getValue(), result);
    }

    /**
     * Compute operand^{-1} mod modulus and store it in result[0].
     *
     * @param operand a base-2^64 value
     * @param modulus a Modulus's value
     * @param result  result[0] store operand^{-1} mod modulus
     * @return if under modulus operand's invert exists, return true, otherwise return false.
     */
    public static boolean tryInvertUintMod(long operand, long modulus, long[] result) {
        return Numth.tryInvertUintMod(operand, modulus, result);
    }


    /**
     * Compute value[0, valueUint64Count) mod modulus, which is a base-2^64 value.
     * The result is stored in values[0].
     *
     * @param values            a base-2^64 value
     * @param valuesUint64Count number of  uint64 used in values
     * @param modulus           a Modulus object
     */
    public static void moduloUintInplace(long[] values, int valuesUint64Count, Modulus modulus) {

        assert values != null;
        assert valuesUint64Count > 0;

        if (valuesUint64Count == 1) {
            if (values[0] >= modulus.getValue()) {
                values[0] = barrettReduce64(values[0], modulus);
            }
            // 如果 value[0] < modulus.getValue() 不需要再模约减，直接返回
            return;
        }
        int i = valuesUint64Count - 1;
        long[] tmp = new long[2];
        while (i-- > 0) {
            System.arraycopy(values, i, tmp, 0, 2);
            values[i] = barrettReduce128(tmp, modulus);
            values[i + 1] = 0;
        }
    }

    /**
     * Compute value[0, valueUint64Count), which is a base-2^64 value, mod modulus
     *
     * @param value            a base-2^64 value
     * @param valueUint64Count number of  uint64 used in values
     * @param modulus          a Modulus object
     * @return value[0, valueUint64Count) mod modulus
     */
    public static long moduloUint(long[] value, int valueUint64Count, Modulus modulus) {

        assert valueUint64Count > 0;

        if (valueUint64Count == 1) {
            if (value[0] < modulus.getValue()) {
                return value[0];
            } else {
                return barrettReduce64(value[0], modulus);
            }
        }

        long[] tmp = new long[]{0, value[valueUint64Count - 1]};
        // 从高位往低位依次 reduce
        // (n-2, n-1) ---> reduce
        // (n-3, n-2) ---> reduce、
        // i-->0  1) i > 0; 2) i--; 3) enter the for loop
        for (int i = valueUint64Count - 1; i-- > 0; ) {
            tmp[0] = value[i];
            tmp[1] = barrettReduce128(tmp, modulus);
        }
        return tmp[1];
    }


    /**
     * Compute value[startIndex, startIndex + valueUint64Count) , which is  a base-2^64 value, then mod modulus.
     *
     * @param value            a base-2^64 value
     * @param startIndex       the start index of value
     * @param valueUint64Count number of uint64 used in value
     * @param modulus          a Modulus object
     * @return value[startIndex, startIndex + valueUint64Count) mod modulus
     */
    public static long moduloUint(long[] value, int startIndex, int valueUint64Count, Modulus modulus) {

        assert valueUint64Count > 0;

        if (valueUint64Count == 1) {
            if (value[startIndex] < modulus.getValue()) {
                return value[startIndex];
            } else {
                return barrettReduce64(value[startIndex], modulus);
            }
        }

        long[] tmp = new long[]{0, value[startIndex + valueUint64Count - 1]};
        // 从高位往低位依次 reduce
        // (n-2, n-1) ---> reduce
        // (n-3, n-2) ---> reduce
        // i-->0  1) i > 0; 2) i--; 3) enter the for loop
        for (int i = startIndex + valueUint64Count - 1; i-- > startIndex; ) {
            tmp[0] = value[i];
            tmp[1] = barrettReduce128(tmp, modulus);
        }
        return tmp[1];
    }


    /**
     * Compute (operand1 * operand2) + operand3 mod modulus.
     *
     * @param operand1 a 64-bit value
     * @param operand2 a 64-bit value
     * @param operand3 a 64-bit value
     * @param modulus  a Modulus object
     * @return (operand1 * operand2) + operand3  mod modulus
     */
    public static long multiplyAddUintMod(long operand1, long operand2, long operand3, Modulus modulus) {
        long[] tmp = new long[2];
        UintArithmetic.multiplyUint64(operand1, operand2, tmp);
        long[] addTmp = new long[1];
        long carry = UintArithmetic.addUint64(tmp[0], operand3, addTmp);
        // update low 64 bits
        tmp[0] = addTmp[0];
        // add carry
        tmp[1] += carry;
        // mod reduce
        return barrettReduce128(tmp, modulus);
    }

    /**
     * Compute (operand1 * operand2) + operand3 mod modulus.
     * Correctness: Follows the condition of multiply_uint_mod.
     *
     * @param operand1 a 64-bit value
     * @param operand2 a MultiplyUintModOperand object
     * @param operand3 a 64-bit value
     * @param modulus  a Modulus object
     * @return (operand1 * operand2) + operand3 mod modulus.
     */
    public static long multiplyAddUintMod(long operand1, MultiplyUintModOperand operand2, long operand3, Modulus modulus) {

        return addUintMod(multiplyUintMod(operand1, operand2, modulus), barrettReduce64(operand3, modulus), modulus);

    }


    /**
     * Compute (operand1 * operand2) mod modulus.
     * Correctness: Follows the condition of barrett_reduce_128.
     *
     * @param operand1 a 64-bit value
     * @param operand2 a 64-bit value
     * @param modulus  a Modulus object
     * @return (operand1 * operand2) mod modulus
     */
    public static long multiplyUintMod(long operand1, long operand2, Modulus modulus) {
        long[] z = new long[2];
        UintArithmetic.multiplyUint64(operand1, operand2, z);
        return barrettReduce128(z, modulus);
    }

    /**
     * Compute x * y mod modulus.
     * This is a highly-optimized variant of Barrett reduction, or called shoup-mul.
     * Correctness: modulus should be at most 63-bit, and y must be less than modulus.
     *
     * @param x       a 64-bit value
     * @param y       a MultiplyUintModOperand object
     * @param modulus a Modulus object
     * @return x * y mod modulus.
     */
    public static long multiplyUintMod(long x, MultiplyUintModOperand y, Modulus modulus) {

        assert y.operand < modulus.getValue() : "y: " + y.operand + ", modulus: " + modulus.getValue();

        long tmp1, tmp2;
        long p = modulus.getValue();
        tmp1 = UintArithmetic.multiplyUint64Hw64(x, y.quotient);
        tmp2 = y.operand * x - tmp1 * p;
        return tmp2 >= p ? tmp2 - p : tmp2;
    }

    /**
     * Compute (x * y mod modulus) or [x * y mod modulus + modulus]
     * This is a highly-optimized variant of Barrett reduction and reduce to [0, 2 * modulus - 1].
     * Correctness: modulus should be at most 63-bit, and y must be less than modulus.
     *
     * @param x       a 64-bit value
     * @param y       a MultiplyUintModOperand object
     * @param modulus a Modulus object
     * @return x * y mod modulus or [(x * y mod modulus) + modulus]
     */
    public static long multiplyUintModLazy(long x, MultiplyUintModOperand y, Modulus modulus) {

        assert y.operand < modulus.getValue();

        long tmp1;
        long p = modulus.getValue();
        tmp1 = UintArithmetic.multiplyUint64Hw64(x, y.quotient);
        // res \in [0, 2p)
        return y.operand * x - tmp1 * p;
    }


    /**
     * Compute input mod modulus and return the result.
     *
     * @param input   a base-2^64 value, at most 64-bit
     * @param modulus a Modulus object, the value must be at most 63-bit
     * @return input % modulus
     */
    public static long barrettReduce64(long input, Modulus modulus) {

        // Reduces input using base 2^64 Barrett reduction
        // floor(2^64 / mod) == floor( floor(2^128 / mod) )
        long q = UintArithmetic.multiplyUint64Hw64(input, modulus.getConstRatio()[1]);
        long res = input - q * modulus.getValue();
        return res >= modulus.getValue() ? res - modulus.getValue() : res;
    }

    /**
     * Compute input mod modulus and return the result.
     *
     * @param input   a base-2^64 value, at most 128-bit
     * @param modulus a Modulus object, the value must be at most 63-bit
     * @return input mod modulus
     */
    public static long barrettReduce128(long[] input, Modulus modulus) {
        assert input.length == 2;

        long tmp1, tmp3, carry;
        long[] tmp2 = new long[2];
        // Round 1
        // (x0 * m0)_1 , 即 x0 * m0 的高 64 bits
        carry = UintArithmetic.multiplyUint64Hw64(input[0], modulus.getConstRatio()[0]);
        // tmp2 = [(x0 * m1)_0, (x0 * m1)_1]
        UintArithmetic.multiplyUint64(input[0], modulus.getConstRatio()[1], tmp2);
        // index 0 is result, index 1 is the carry
        // x0 * m0 的高位 + x0 * m1 的低位， 对应 {(x0*m0)_1 + (x0 * m1)_0} >> 64
        long[] addTmp = new long[1];
        long carryTmp = UintArithmetic.addUint64(tmp2[0], carry, addTmp);
        // x0 * m0 的高位 + x0 * m1 的低位 的值
        tmp1 = addTmp[0];
        // tmp3 = (x0 * m1)_1 + 进位
        // + add carry
        tmp3 = tmp2[1] + carryTmp;

        // Round 2
        // tmp2 = [(x1 * m0)_0, (x1 * m0)_1]
        UintArithmetic.multiplyUint64(input[1], modulus.getConstRatio()[0], tmp2);
        carryTmp = UintArithmetic.addUint64(tmp1, tmp2[0], addTmp);
        // (x1 * m0)_1 + [ (x0 * m1)_0 + (x0 * m0)_1] +  (x1 * m0)_0 产生的进位
        // carry 其实就是 (x1 * m0)_0 / 2^64 + x1 * m0
        carry = tmp2[1] + carryTmp;
        // x1 * m1 + (x0 * m1)_1 + (x1 * m0)_1 + carry
        // 这里就对应最后公式求和的结果
        tmp1 = input[1] * modulus.getConstRatio()[1] + tmp3 + carry;

        // reduction
        tmp3 = input[0] - tmp1 * modulus.getValue();

        return tmp3 >= modulus.getValue() ? tmp3 - modulus.getValue() : tmp3;
    }


    /**
     * Compute (operand + 1) mod modulus.
     * Correctness: operand must be at most (2 * modulus -2) for correctness.
     *
     * @param operand a base-2^64 value
     * @param modulus a Modulus object
     * @return (input + 1) mod modulus
     */
    public static long incrementUintMod(long operand, Modulus modulus) {
        // a <= 2p - 2
        assert Long.compareUnsigned(operand, (modulus.getValue() - 1) << 1) <= 0;

        operand++;
        // 如果 a = 2p - 1, a + 1 = 2p, 则 这里的结果为 2p - p = p, 显然是错的
        // -1 表示 0xFFFFFFFFFFFFFFFF
        return operand - (modulus.getValue() & ((operand >= modulus.getValue() ? -1 : 0)));
    }

    /**
     * Compute (operand - 1) mod modulus.
     *
     * @param operand a base-2^64 value, at most (modulus - 1)
     * @param modulus a Modulus object
     * @return (operand - 1) mod modulus
     */
    public static long decrementUintMod(long operand, Modulus modulus) {

        assert !modulus.isZero();
        // operand < p
        assert Long.compareUnsigned(operand, modulus.getValue()) < 0;

        long carry = operand == 0 ? 1 : 0;
        // 如果 operand = p, 结果为 p - 1 - 1 = p - 2 显然是错的
        return operand - 1 + (modulus.getValue() & -carry);
    }

    /**
     * Compute (-operand) mod modulus.
     * Correctness: operand must be at most modulus for correctness.
     *
     * @param operand a base-2^64 value， at most modulus
     * @param modulus a Modulus object
     * @return (- operand) mod modulus
     */
    public static long negateUintMod(long operand, Modulus modulus) {
        assert !modulus.isZero();
        // a < p
        assert Long.compareUnsigned(operand, modulus.getValue()) < 0;

        long nonZero = operand != 0 ? 1 : 0;
        return (modulus.getValue() - operand) & (-nonZero);
    }

    /**
     * Compute (operand * inv (2)) mod modulus.
     * Correctness: operand must [be even and at most (2 * modulus - 2)] or [odd and at most (modulus - 2)].
     *
     * @param operand a base-2^64 value, at most (modulus - 1)
     * @param modulus a Modulus object
     * @return (operand * inv ( 2)) mod modulus
     */
    public static long div2UintMod(long operand, Modulus modulus) {

        assert !modulus.isZero();
        assert Long.compareUnsigned(operand, modulus.getValue()) < 0;

        // odd value
        // a * inv(2) mod p =  (a + p) >>> 1
        if ((operand & 1) > 0) {
            long[] tmp = new long[1];
            long carry = UintArithmetic.addUint64(operand, modulus.getValue(), 0, tmp);
            operand = tmp[0] >>> 1;
            // 如果有溢出，需要把 operand 的最高位置1
            if (carry > 0) {
                return operand | (1L << (UintArithmetic.UINT64_BITS - 1));
            }
            return operand;
        }
        // even value
        return operand >>> 1;
    }

    /**
     * Compute (a + b) mod modulus.
     * Correctness: (a + b) must be at most (2 * modulus - 1).
     *
     * @param a       an unsigned 64-bit value
     * @param b       an unsigned 64-bit value
     * @param modulus a Modulus object
     * @return (a + b) mod modulus
     */
    public static long addUintMod(long a, long b, Modulus modulus) {

        assert !modulus.isZero();
        /*
            a + b < 2p
            如果 a + b = 2p, 按照如下的算法 最后结果是 p, 这显然是错误的
         */
        assert Long.compareUnsigned(a + b, modulus.getValue() << 1) < 0;

        // Sum of a + b modulo Modulus can never wrap around 2^64
        a += b;
        return a >= modulus.getValue() ? a - modulus.getValue() : a;
    }

    /**
     * Compute (a - b) mod modulus.
     * Correctness: (a - b) must be at most (modulus - 1) and at least (-modulus).
     *
     * @param a       a base-2^64 value, at most (modulus - 1)
     * @param b       a base-2^64 value, at most (modulus - 1)
     * @param modulus a Modulus object
     * @return (a - b) mod modulus
     */
    public static long subUintMod(long a, long b, Modulus modulus) {

        assert !modulus.isZero();
        // a < p
        assert Long.compareUnsigned(a, modulus.getValue()) < 0;
        // b < p
        assert Long.compareUnsigned(b, modulus.getValue()) < 0;

        // tmp[0] is sub result
        long[] tmp = new long[1];
        long borrow = UintArithmetic.subUint64(a, b, 0, tmp);
        // if borrow = 1, return tmp[0] + modulus
        // if borrow = 0, return tmp[0]
        return tmp[0] + (modulus.getValue() & (-borrow));
    }


}
