package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;

import java.util.Arrays;

/**
 * small mod means the modulus up to 61 bits, single long can implement computation
 *
 * @author Qixian Zhou
 * @date 2023/8/5
 */
public class UintArithmeticSmallMod {


    /**
     *
     * @param operand1
     * @param operand2
     * @param count
     * @param modulus
     * @return \sum a_i * b_i mod modulus
     */
    public static long dotProductMod(long[] operand1, long[] operand2, int count, Modulus modulus) {
        assert count >= 0;

        long[] accumulator = new long[2];
        switch (count) {
            case 0:
                return 0;
            case 1:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 1);
                break;
            case 2:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 2);
                break;
            case 3:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 3);
                break;
            case 4:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 4);
                break;
            case 5:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 5);
                break;
            case 6:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 6);
                break;
            case 7:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 7);
                break;
            case 8:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 8);
                break;
            case 9:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 9);
                break;
            case 10:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 10);
                break;
            case 11:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 11);
                break;
            case 12:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 12);
                break;
            case 13:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 13);
                break;
            case 14:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 14);
                break;
            case 15:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 15);
                break;
            case 16:
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 16);
                break;
            default:

                long[] c1 = Arrays.copyOfRange(operand1, 16, count);
                long[] c2 = Arrays.copyOfRange(operand2, 16, count);
                accumulator[0] = dotProductMod(c1, c2, count - 16, modulus);
                UintArithmetic.multiplyAccumulateUint64(operand1, operand2, accumulator, 16);
                break;
        }
        return barrettReduce128(accumulator, modulus);
    }


    /**
     *
     * @param operand
     * @param exponent
     * @param modulus
     * @return a^e mod modulus
     */
    public static long exponentUintMod(long operand, long exponent, Modulus modulus) {

        assert operand < modulus.getValue();

        if (exponent == 0) return 1;
        if (exponent == 1) return operand;

        // Perform binary exponentiation.
        long power = operand;
        long product = 0;
        long intermediate = 1;

        // Initially: power = operand and intermediate = 1, product is irrelevant.
        while (true) {
            if ((exponent & 1) > 0) {
                product = multiplyUintMod(power, intermediate, modulus);
                // update intermediate
                intermediate = product;
            }
            exponent >>>= 1;
            if (exponent == 0) break;
            product = multiplyUintMod(power, power, modulus);
            // update power
            power = product;
        }
        return intermediate;
    }



    /**
     *
     * @param operand
     * @param modulus
     * @param result
     * @return if under modulus operand's invert exists, return true, otherwise return false. the invert store in long[] result, which is a length=1 array.
     */
    public static boolean tryInvertUintMod(long operand, Modulus modulus, long[] result) {
        assert result.length == 1;
        return Numth.tryInvertUintMod(operand, modulus.getValue(), result);
    }

    public static boolean tryInvertUintMod(long operand, long modulus, long[] result) {
        assert result.length == 1;
        return Numth.tryInvertUintMod(operand, modulus, result);
    }



    /**
     * values[0] =  values mod modulus, note that values is a base-2^64 number, or is a valuesUint64Count * 64 bits number
     * modulus is up to 61 bits, so values[0] can represent result.
     * @param values
     * @param valuesUint64Count
     * @param modulus
     */
    public static void moduloUintInplace(long[] values, int valuesUint64Count, Modulus modulus) {

        assert valuesUint64Count > 0;

        if (valuesUint64Count == 1) {
            if (values[0] < modulus.getValue()) {
                return;
            }else {
                values[0] = barrettReduce64(values[0], modulus);
                return;
            }
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
     *
     * @param value a base-2^64 value
     * @param valueUint64Count how many uint64 used in values
     * @param modulus modulus
     * @return Uint mod modulus, long[] mod long
     */
    public static long moduloUint(long[] value, int valueUint64Count, Modulus modulus) {

        assert valueUint64Count > 0;

        if (valueUint64Count == 1) {
            if (value[0] < modulus.getValue()) {
                return value[0];
            }else {
                return barrettReduce64(value[0], modulus);
            }
        }

        long[] tmp = new long[] {0, value[valueUint64Count - 1]};
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
     * treat value[startIndex, startIndex + valueUint64Count) as a base-2^64 value, then mod modulus.
     *
     * @param value
     * @param startIndex
     * @param valueUint64Count
     * @param modulus
     * @return
     */
    public static long moduloUint(long[] value, int startIndex, int valueUint64Count, Modulus modulus) {

        assert valueUint64Count > 0;

        if (valueUint64Count == 1) {
            if (value[startIndex] < modulus.getValue()) {
                return value[startIndex];
            }else {
                return barrettReduce64(value[startIndex], modulus);
            }
        }

        long[] tmp = new long[] {0, value[startIndex + valueUint64Count - 1]};
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
     *
     * @param operand1
     * @param operand2
     * @param operand3
     * @param modulus
     * @return (operand1 * operand2) + operand3  mod modulus
     */
    public static long multiplyAddUintMod(long operand1, long operand2, long operand3, Modulus modulus) {
        long[] tmp = new long[2];
        UintArithmetic.multiplyUint64(operand1, operand2, tmp);
        long[] addTmp = new long[1];
        long carry = UintArithmetic.addUint64(tmp[0], operand3, addTmp);
        tmp[0] = addTmp[0]; // update low 64 bits
        tmp[1] += carry; // add carry
        return barrettReduce128(tmp, modulus);
    }

    /**
     * Returns (operand1 * operand2) + operand3 mod modulus.
     * Correctness: Follows the condition of multiply_uint_mod.
     * @param operand1
     * @param operand2
     * @param operand3
     * @param modulus
     * @return
     */
    public static long multiplyAddUintMod(long operand1, MultiplyUintModOperand operand2, long operand3, Modulus modulus) {

        return addUintMod(multiplyUintMod(operand1, operand2, modulus), barrettReduce64(operand3, modulus), modulus);

    }




    /**
     * Returns (operand1 * operand2) mod modulus.
     *         Correctness: Follows the condition of barrett_reduce_128.
     * @param operan1
     * @param operand2
     * @param modulus
     * @return
     */
    public static long multiplyUintMod(long operan1, long operand2, Modulus modulus) {
        long[] z = new long[2];
        UintArithmetic.multiplyUint64(operan1, operand2, z);
        return barrettReduce128(z, modulus);
    }

    /**
     *  Returns x * y mod modulus.
     *  This is a highly-optimized variant of Barrett reduction.
     *  Correctness: modulus should be at most 63-bit, and y must be less than modulus.
     *
     * @param x
     * @param y
     * @param modulus
     * @return
     */
    public static long multiplyUintMod(long x, MultiplyUintModOperand y, Modulus modulus) {

        assert y.operand < modulus.getValue(): "y: " + y.operand + ", modulus: " + modulus.getValue();

        long tmp1, tmp2;
        long p = modulus.getValue();
        tmp1 = UintArithmetic.multiplyUint64Hw64(x, y.quotient);
        tmp2 = y.operand * x - tmp1 * p;
        return tmp2 >= p ? tmp2 - p : tmp2;
    }

    /**
     * Returns x * y mod modulus or x * y mod modulus + modulus.
     * This is a highly-optimized variant of Barrett reduction and reduce to [0, 2 * modulus - 1].
     *  Correctness: modulus should be at most 63-bit, and y must be less than modulus.
     * @param x
     * @param y
     * @param modulus
     * @return
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
     *
     * @param input input
     * @param modulus a Modulus object
     * @return
     */
    public static long barrettReduce64(long input, Modulus modulus) {

        // Reduces input using base 2^64 Barrett reduction
        // floor(2^64 / mod) == floor( floor(2^128 / mod) )
        long q = UintArithmetic.multiplyUint64Hw64(input, modulus.getConstRatio()[1]);

        long res = input - q * modulus.getValue();
//        System.out.println("q: " + Long.toHexString(q));
//        System.out.println("res: " + Long.toHexString(res));

        return res >= modulus.getValue() ? res - modulus.getValue() : res;
    }

    /**
     *
     * @param input
     * @param modulus
     * @return
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
        tmp3 = tmp2[1] + carryTmp; // + add carry

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
     * Correctness: operand must be at most (2 * modulus -2) for correctness.
     * @param operand
     * @param modulus
     * @return (input + 1) mod modulus
     */
    public static long incrementUintMod(long operand, Modulus modulus) {

        assert Long.compareUnsigned(operand, (modulus.getValue() - 1) << 1) <= 0;

        operand++;
        return operand - (modulus.getValue() & ((operand >= modulus.getValue() ? -1 : 0)) );
    }

    /**
     *
     * @param operand at most modulus - 1
     * @param modulus
     * @return
     */
    public static long decrementUintMod(long operand, Modulus modulus) {

        assert Long.compareUnsigned(operand, modulus.getValue()) < 0;

        long carry = operand == 0 ? 1 : 0;
        return operand - 1 + (modulus.getValue() & -carry);
    }


    public static long negateUintMod(long operand, Modulus modulus) {

        assert Long.compareUnsigned(operand, modulus.getValue()) < 0;

        long nonZero = operand != 0 ? 1 : 0;

        return (modulus.getValue() - operand) & (-nonZero);

    }

    /**
     * Correctness: operand must be even and at most (2 * modulus - 2) or odd and at most (modulus - 2).
     * @param operand
     * @param modulus
     * @return (operand * inv(2)) mod modulus
     */
    public static long div2UintMod(long operand, Modulus modulus) {

        assert Long.compareUnsigned(operand, modulus.getValue()) < 0;

        // if operand is an odd value
        if ((operand & 1) > 0) {
            long tmp[] = new long[1];
            long carry = UintArithmetic.addUint64(operand, modulus.getValue(), 0, tmp);
            operand = tmp[0] >>> 1;
            if (carry > 0) {
                return operand | (1L << (UintArithmetic.UINT64_BITS - 1));
            }
            return operand;
        }

        // even value
        return operand >>> 1;
    }

    /**
     * Correctness: (operand1 + operand2) must be at most (2 * modulus - 1).
     * @param a
     * @param b
     * @param modulus
     * @return (a + b) % modulus
     */
    public static long addUintMod(long a, long b, Modulus modulus) {

//        assert a >= 0 && b >= 0;
        // a + b < 2p
        assert Long.compareUnsigned(a + b, modulus.getValue() << 1) < 0;
//        assert ((a + b) < modulus.getValue() << 1);

//        if ((a + b) >= modulus.value << 1) {
//            throw  new IllegalArgumentException("input out of range");
//        }
        // up to 61 bits
        a += b;
        return a >= modulus.getValue() ? a - modulus.getValue() : a;
    }

    /**
     *
     * @param a
     * @param b
     * @param modulus
     * @return (a - b ) % modulus
     */
    public static long subUintMod(long a, long b, Modulus modulus) {

        assert Long.compareUnsigned(a, modulus.getValue()) < 0;
        assert Long.compareUnsigned(b, modulus.getValue()) < 0;

        // tmp[0] is sub result, tmp[1] is the borrow
        long[] tmp = new long[1];
        long borrow = UintArithmetic.subUint64(a, b, 0, tmp);
        return tmp[0] + (modulus.getValue() & (-borrow));
    }




}
