package edu.alibaba.mpc4j.crypto.fhe.zq;

import java.util.Arrays;

/**
 * Unsigned int arithmetic, or base-2^64 arithmetic. The implementation is from:
 * <p>
 * https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/uintarithmod.h,
 * </p>
 * Modification here is to use long in java equivalent to uint64_t in C++ as the most basic data type.
 *
 * @author Qixian Zhou
 * @date 2023/8/5
 */
public class UintArithmetic {
    /**
     * bit-count of an uint64 value or a long value
     */
    public static final int UINT64_BITS = 64;

    /**
     * Computes (operand1 and operand2) and the result value is stored in result[0, uint64Count).
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of operated uint64.
     * @param result      store in result[0, uint64Count).
     */
    public static void andUint(long[] operand1, long[] operand2, int uint64Count, long[] result) {
        assert uint64Count > 0;
        while (--uint64Count >= 0) {
            result[uint64Count] = operand1[uint64Count] & operand2[uint64Count];
        }
    }

    /**
     * Computes (operand1 or operand2) and the result value is stored in result[0, uint64Count).
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of operated uint64.
     * @param result      store in result[0, uint64Count).
     */
    public static void orUint(long[] operand1, long[] operand2, int uint64Count, long[] result) {
        assert uint64Count > 0;
        while (--uint64Count >= 0) {
            result[uint64Count] = operand1[uint64Count] | operand2[uint64Count];
        }
    }

    /**
     * Computes (operand1 xor operand2) and the result value is stored in result[0, uint64Count).
     *
     * @param operand1    operand1.
     * @param operand2    operand2.
     * @param uint64Count number of operated uint64.
     * @param result      store in result[0, uint64Count).
     */
    public static void xorUint(long[] operand1, long[] operand2, int uint64Count, long[] result) {
        assert uint64Count > 0;
        while (--uint64Count >= 0) {
            result[uint64Count] = operand1[uint64Count] ^ operand2[uint64Count];
        }
    }

    /**
     * Computes (~operand) and the result value is stored in result[0, uint64Count)
     *
     * @param operand     operand.
     * @param uint64Count number of operated uint64.
     * @param result      store in result[0, uint64Count).
     */
    public static void notUint(long[] operand, int uint64Count, long[] result) {
        assert uint64Count > 0;
        while (--uint64Count >= 0) {
            result[uint64Count] = ~operand[uint64Count];
        }
    }

    /**
     * Computes (operand + 1) / 2 and store it in result[0, uint64Count)
     *
     * @param operand     operand.
     * @param uint64Count number of operated uint64.
     * @param result      store in result[0, uint64Count).
     */
    public static void halfRoundUpUint(long[] operand, int uint64Count, long[] result) {
        if (uint64Count == 0) {
            return;
        }
        // Set result to (operand + 1) / 2. To prevent overflowing operand, right shift
        // and then increment result if low-bit of operand was set.
        long lowBitSet = operand[0] & 1;
        // note that we use >>> instead of >>, Because we treat long here as an unsigned int64,
        // we need to use the corresponding logical right shift to ignore the sign bit.
        for (int i = 0; i < uint64Count - 1; i++) {
            result[i] = (operand[i] >>> 1) | (operand[i + 1] << (UINT64_BITS - 1));
        }
        result[uint64Count - 1] = operand[uint64Count - 1] >>> 1;
        // we expect the result is (operand / 2) + 0.5.
        // if lowBitSet = 0, then 0/2 + 0.5 ---> 0; if lowBitSet = 1, then 1/2 + 0.5 ---> 1.
        if (lowBitSet > 0) {
            incrementUint(result, uint64Count, result);
        }
    }

    /**
     * Computes (operand >> shiftAmount) and store it in result[0, uint64Count).
     *
     * @param operand     operand.
     * @param shiftAmount bit-count of right shift.
     * @param uint64Count number of operated uint64.
     * @param result      store in result[0, uint64Count).
     */
    public static void rightShiftUint(long[] operand, int shiftAmount, int uint64Count, long[] result) {
        assert uint64Count > 0;
        assert shiftAmount >= 0 && shiftAmount <= uint64Count * UINT64_BITS;
        // How many words to shift, one words is 64 bits
        int uint64ShiftAmount = shiftAmount / UINT64_BITS;
        // shift words
        System.arraycopy(operand, uint64ShiftAmount, result, 0, uint64Count - uint64ShiftAmount);
        Arrays.fill(result, uint64Count - uint64ShiftAmount, uint64Count, 0L);
        // shift bits
        int bitShiftAmount = shiftAmount - (uint64ShiftAmount * UINT64_BITS);
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT64_BITS - bitShiftAmount;
            for (int i = 0; i < uint64Count - 1; i++) {
                result[i] = (result[i] >>> bitShiftAmount) | (result[i + 1] << negBitShiftAmount);
            }
            result[uint64Count - 1] = result[uint64Count - 1] >>> bitShiftAmount;
        }
    }

    /**
     * Computes (operand << shiftAmount) and store it in result[0, uint64Count).
     *
     * @param operand     operand.
     * @param shiftAmount bit-count of left shift.
     * @param uint64Count number of operated uint64.
     * @param result      store in result[0, uint64Count).
     */
    public static void leftShiftUint(long[] operand, int shiftAmount, int uint64Count, long[] result) {
        assert uint64Count > 0;
        assert shiftAmount >= 0 && shiftAmount <= uint64Count * UINT64_BITS;
        // How many words to shift, one words is 64 bits
        int uint64ShiftAmount = shiftAmount / UINT64_BITS;
        // shift words
        for (int i = 0; i < uint64Count - uint64ShiftAmount; i++) {
            result[uint64Count - i - 1] = operand[uint64Count - i - 1 - uint64ShiftAmount];
        }
        for (int i = uint64Count - uint64ShiftAmount; i < uint64Count; i++) {
            result[uint64Count - i - 1] = 0;
        }
        // shift bits
        int bitShiftAmount = shiftAmount - (uint64ShiftAmount * UINT64_BITS);
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT64_BITS - bitShiftAmount;
            for (int i = uint64Count - 1; i > 0; i--) {
                result[i] = (result[i] << bitShiftAmount) | (result[i - 1] >>> negBitShiftAmount);
            }
            result[0] = result[0] << bitShiftAmount;
        }
    }

    /**
     * Computes -operand and store it in result[0, uint64Count).
     * Note that operand + (-operand) mod 2^64 = 0, and -operand is still a base-2^64 value.
     * For example: operand = 1, then -operand = 2^64 - 1.
     *
     * @param operand     operand.
     * @param uint64Count number of operated uint64.
     * @param result      store in result[0, uint64Count).
     */
    public static void negateUint(long[] operand, int uint64Count, long[] result) {
        assert uint64Count > 0;
        long[] tmp = new long[1];
        long carry;
        // Negation is equivalent to inverting bits and adding 1.
        carry = addUint64(~operand[0], 1, tmp);
        result[0] = tmp[0];
        int i = 1;
        while (--uint64Count > 0) {
            carry = addUint64(~operand[i], 0, carry, tmp);
            result[i] = tmp[0];
            i++;
        }
    }

    /**
     * compute (as - 1) ---> (diff, borrow), diff is stored in result[0, uint64Count) and borrow is returned.
     *
     * @param as          a base-2^64 value, which length is uint64Count
     * @param uint64Count number of uint64 used in as
     * @param result      result[0, uint64Count) store the result
     * @return (as - 1)'s borrow
     */
    public static long decrementUint(long[] as, int uint64Count, long[] result) {
        assert uint64Count > 0;
        return subUint(as, uint64Count, 1, result);
    }

    /**
     * compute (as + 1) ---> (sum, carry), sum is stored in result[0, uint64Count) and carry is returned.
     *
     * @param as          a base-2^64 value, which length is uint64Count
     * @param uint64Count number of uint64 used in as
     * @param result      result[0, uint64Count) store the (as + 1)
     * @return (as + 1)'s carry
     */
    public static long incrementUint(long[] as, int uint64Count, long[] result) {
        assert uint64Count > 0;
        return addUint(as, uint64Count, 1, result);
    }


    /**
     * compute (as + bs) --> (sum, carry), sum is stored in result[0, 2) and carry is returned.
     *
     * @param as     an unsigned 128-bit value
     * @param bs     an unsigned 128-bit value
     * @param result result[0, 2) store (as + bs)'s low 128-bit value
     * @return (as + bs)'s carry, which value is 0 or 1
     */
    public static long addUint128(long[] as, long[] bs, long[] result) {

        long[] tmp = new long[1];
        long carry = addUint64(as[0], bs[0], tmp);
        result[0] = tmp[0];
        carry = addUint64(as[1], bs[1], carry, tmp);
        result[1] = tmp[0];

        return carry;
    }


    /**
     * Compute (as * bs) and stores it low n digit.
     * 一般来说，n-digit * n-digit 得到的结果是一个 2n-digit 的数，这里就是只保存 低 n-digit.
     *
     * @param as          a base-2^64 value
     * @param bs          a base-2^64 value
     * @param uint64Count umber of uint64 used in as and bs
     * @param result      result[0, uint64Count) stores the (as * bs)'s low n digit.
     */
    public static void multiplyTruncateUint(long[] as, long[] bs, int uint64Count, long[] result) {
        multiplyUint(as, uint64Count, bs, uint64Count, uint64Count, result);
    }

    /**
     * compute (as * bs) and store it in result[0, uint64Count).
     *
     * @param as          a base-2^64 value
     * @param bs          a base-2^64 value
     * @param uint64Count number of uint64 used in as and bs
     * @param result      result[0, uint64Count) stores (as * bs)
     */
    public static void multiplyUint(long[] as, long[] bs, int uint64Count, long[] result) {
        multiplyUint(as, uint64Count, bs, uint64Count, uint64Count * 2, result);
    }

    /**
     * compute (as * bs) and store it in result[0, resultUint64Count).
     *
     * @param as                a base-2^64 value
     * @param asUint64Count     number of uint64 used in as
     * @param bs                a base-2^64 value
     * @param bsUint64Count     number of uint64 used in bs
     * @param resultUint64Count number of uint64 used in result
     * @param result            result[0, resultUint64Count) stores (as * bs)
     */
    public static void multiplyUint(long[] as, int asUint64Count, long[] bs, int bsUint64Count, int resultUint64Count, long[] result) {
        assert asUint64Count >= 0;
        assert bsUint64Count >= 0;
        assert resultUint64Count > 0;
        assert result != as && result != bs;

        if (asUint64Count == 0 || bsUint64Count == 0) {
            // todo: 保持SEAL的风格，还是全面取消？
            UintCore.setZeroUint(resultUint64Count, result);
            return;
        }
        if (resultUint64Count == 1) {
            result[0] = as[0] * bs[0];
            return;
        }
        // 只需要处理真正的有效位，例如 [1, 0, 0] * [1, 0] ,我们只需要处理 1 * 1 = 1
        asUint64Count = UintCore.getSignificantUint64CountUint(as, asUint64Count);
        bsUint64Count = UintCore.getSignificantUint64CountUint(bs, bsUint64Count);
        // more fast
        if (asUint64Count == 1) {
            multiplyUint(bs, bsUint64Count, as[0], resultUint64Count, result);
            return;
        }
        if (bsUint64Count == 1) {
            multiplyUint(as, asUint64Count, bs[0], resultUint64Count, result);
            return;
        }
        // clear result
        UintCore.setZeroUint(resultUint64Count, result);
        // long[] * long[]

        int asIndexMax = Math.min(asUint64Count, resultUint64Count);
        for (int asIndex = 0; asIndex < asIndexMax; asIndex++) {
            long[] innerBs = new long[bs.length];
            System.arraycopy(bs, 0, innerBs, 0, bs.length);
            long[] innerResult = new long[resultUint64Count - asIndex];
            // create new innerResult from result, note that result's start index is asIndex
            System.arraycopy(result, asIndex, innerResult, 0, innerResult.length);

            long carry = 0;
            int bsIndex = 0;
            int bsIndexMax = Math.min(bsUint64Count, resultUint64Count - asIndex);
            for (; bsIndex < bsIndexMax; bsIndex++) {
                long[] tempResult = new long[2];
                multiplyUint64(as[asIndex], innerBs[bsIndex], tempResult);
                long[] addTemp = new long[1];
                long tmpCarry = addUint64(tempResult[0], carry, 0, addTemp);
                carry = tempResult[1] + tmpCarry;
                long[] addTemp2 = new long[1];
                long tmpCarry2 = addUint64(innerResult[bsIndex], addTemp[0], 0, addTemp2);
                carry += tmpCarry2;
                innerResult[bsIndex] = addTemp2[0];
            }
            // Write carry if there is room in result
            if (asIndex + bsIndexMax < resultUint64Count) {
                innerResult[bsIndex] = carry;
            }
            // overwrite result, note that result's start index is asIndex
            System.arraycopy(innerResult, 0, result, asIndex, innerResult.length);
        }
    }

    /**
     * compute (as * b) and store it in result[0, resultUint64Count).
     *
     * @param as                a base-2^64 value
     * @param asUint64Count     number of uint64 used in as
     * @param b                 an unsigned 64-bit value
     * @param resultUint64Count number of uint64 used in result
     * @param result            result[0, resultUint64Count) stores (as * b)
     */
    public static void multiplyUint(long[] as, int asUint64Count, long b, int resultUint64Count, long[] result) {

        assert asUint64Count >= 0;
        assert resultUint64Count > 0;
        assert result != as;

        if (asUint64Count == 0 || b == 0) {
            // 置0, 逐步弃用 UintCore 中的方法，因为涉及到数组起点的问题，直接处理
            Arrays.fill(result, 0, resultUint64Count, 0);
            return;
        }
        // 如果 result 的 uint64 count 就是 1，结果只能放置 64-bit大小的数据，这里本质上是保存乘法结果的低64-bit
        // 如果溢出就直接溢出了，不考虑如何处理
        if (resultUint64Count == 1) {
            result[0] = as[0] * b;
            return;
        }
        // clear out result
        UintCore.setZeroUint(resultUint64Count, result);
        // Multiply
        long carry = 0;
        int asIndexMax = Math.min(asUint64Count, resultUint64Count);

        int asIndex = 0;
        // 逐位相乘
        for (; asIndex < asIndexMax; asIndex++) {
            // index = 0 is the low-64bit, index = 1 is the high 64 bits
            long[] mulTemp = new long[2];
            multiplyUint64(as[asIndex], b, mulTemp);
            // addTemp[0] is the add result, the low 64-bit, carryTemp store the carry
            long[] addTemp = new long[1];
            // 当前 乘法计算的低位 加上 上一次计算的进位carry
            long carryTemp = addUint64(mulTemp[0], carry, 0, addTemp);
            // 乘法计算的高位 + carryTemp 等于当前两个位置计算的 进位
            carry = mulTemp[1] + carryTemp;

            result[asIndex] = addTemp[0];
        }
        // 保存 carry 到下一个高位
        if (asIndexMax < resultUint64Count) {
            result[asIndex] = carry;
        }
    }

    /**
     * Compute \Prod_i operands[i] = (operand[0] * operand[1] * .... * operand[count - 1]) and store it in result[0, count).
     * Note that result is a base-2^64 value and it's length is count.
     *
     * @param operands an array, each value is an unsigned value, note that this is not a base-2^64 value
     * @param count    number of elements that need to participate in the multiply
     * @param result   result[0, count) store the \Prod_i operands[i], i = \{0, 1, ..., count-1\}
     */
    public static void multiplyManyUint64(long[] operands, int count, long[] result) {

        assert operands != result;

        if (count == 0) {
            return;
        }

        result[0] = operands[0];
//        UintCore.setUint(operands[0], count, result);

        long[] tempMpi = new long[count];
        for (int i = 1; i < count; i++) {
            multiplyUint(result, i, operands[i], i + 1, tempMpi);
            System.arraycopy(tempMpi, 0, result, 0, i + 1);
//            UintCore.setUint(tempMpi, i + 1, result);
        }
    }

    /**
     * Compute \Prod_i operands[i], i = \in [0, count) and i != except, and store it in result[0, count).
     *
     * @param operands an array, each value is an unsigned value, note that this is not a base-2^64 value
     * @param count    number of elements that need to participate in the multiply
     * @param except   operands[except] will not be multiplied
     * @param result   result[0, count) store the \Prod_i operands[i], i = \in [0, count) and i != except
     */
    public static void multiplyManyUint64Except(long[] operands, int count, int except, long[] result) {

        assert operands != result;
        assert count >= 1;
        assert except >= 0 && except < count;

        // empty product, res = 1
        // when count == 1, valid except must be 0
        if (count == 1) {
            result[0] = 1;
            return;
        }
        // set result is operand[0] unless except = 0
        result[0] = except == 0 ? 1 : operands[0];

        long[] tempMpi = new long[count];
        for (int i = 1; i < count; i++) {
            if (i != except) {
                multiplyUint(result, i, operands[i], i + 1, tempMpi);
                System.arraycopy(tempMpi, 0, result, 0, i + 1);
//                UintCore.setUint(tempMpi, i + 1, result);
            }
        }
    }


    /**
     * compute a * b = r = r0 + r1*2^64, and store it in result[0, 2).
     * The basic idea is: a = (2^32 a + aRight), b = (2^32 b + bRight); then a * b = (2^32 a + aRight) *  (2^32 b + bRight).
     *
     * @param a         an unsigned 64-bit value
     * @param b         an unsigned 64-bit value
     * @param result128 result[0, 2) store a*b, result[0] store the low 64-bit of a*b, result[1] store the high 64-bit of a*b
     */
    public static void multiplyUint64(long a, long b, long[] result128) {
        multiplyUint64Generic(a, b, result128);
    }

    /**
     * compute a * b = r = r0 + r1*2^64, and store it in result[0, 2).
     * The basic idea is: a = (2^32 a + aRight), b = (2^32 b + bRight); then a * b = (2^32 a + aRight) *  (2^32 b + bRight).
     *
     * @param a         an unsigned 64-bit value
     * @param b         an unsigned 64-bit value
     * @param result128 result[0, 2) store a*b, result[0] store the low 64-bit of a*b, result[1] store the high 64-bit of a*b
     */
    public static void multiplyUint64Generic(long a, long b, long[] result128) {

        long aRight = a & 0x00000000FFFFFFFFL;
        long bRight = b & 0x00000000FFFFFFFFL;
        a >>>= 32;
        b >>>= 32;

        long middle1 = a * bRight;
        long middle, carry;
        long[] tmp = new long[1];
        carry = addUint64(middle1, b * aRight, tmp);
        middle = tmp[0];

        long left = a * b + (carry << 32);
        long right = aRight * bRight;
        long tmpSum = (right >>> 32) + (middle & 0x00000000FFFFFFFFL);

        result128[1] = left + (middle >>> 32) + (tmpSum >>> 32);
        result128[0] = (tmpSum << 32) | (right & 0x00000000FFFFFFFFL);
    }


    /**
     * compute (a * b)'s high 64-bit and return it
     *
     * @param a an unsigned 64-bit value
     * @param b an unsigned 64-bit value
     * @return (a * b)'s high 64-bit
     */
    public static long multiplyUint64Hw64(long a, long b) {
        return multiplyUint64Hw64Generic(a, b);
    }

    /**
     * compute (a * b)'s high 64-bit and return it
     *
     * @param a an unsigned 64-bit value
     * @param b an unsigned 64-bit value
     * @return (a * b)'s high 64-bit
     */
    public static long multiplyUint64Hw64Generic(long a, long b) {

        long result;
        long aRight = a & 0x00000000FFFFFFFFL;
        long bRight = b & 0x00000000FFFFFFFFL;
        a >>>= 32;
        b >>>= 32;

        long middle1 = a * bRight;
        long middle, carry;
        long[] tmp = new long[1];
        carry = addUint64(middle1, b * aRight, tmp);
        middle = tmp[0];
        long left = a * b + (carry << 32);
        long right = aRight * bRight;
        long tmpSum = (right >>> 32) + (middle & 0x00000000FFFFFFFFL);

        result = left + (middle >>> 32) + (tmpSum >>> 32);
        return result;
    }

    /**
     * Compute \sum(operand1[i] * operand2[i]), i \in [0, count) and store it in accumulator[0, count)
     *
     * @param operand1    an array, each value is an unsigned value, note that this is not a base-2^64 value
     * @param startIndex1 start index of operand1
     * @param operand2    an array, each value is an unsigned value, note that this is not a base-2^64 value
     * @param startIndex2 start index of operand2
     * @param accumulator store \sum(operand1[i] * operand2[i]), i \in [0, count)
     * @param count       number of elements in operan1 and operand2, that need to participate in the multiply
     */
    public static void multiplyAccumulateUint64(
        long[] operand1,
        int startIndex1,
        long[] operand2,
        int startIndex2,
        long[] accumulator,
        int count) {

        if (count == 0) {
            return;
        }
        long[] qWord = new long[2];
        multiplyUint64(operand1[startIndex1], operand2[startIndex2], qWord);
        // using startIndex to avoid array copy
        multiplyAccumulateUint64(operand1, startIndex1 + 1, operand2, startIndex2 + 1, accumulator, count - 1);
        addUint128(qWord, accumulator, accumulator);
    }


    /**
     * Compute a/b = q + r, a is numerator, b is denominator, q is stored in quotient[0, 2) and r is stored in num numerator[0].
     *
     * @param numerator   a base-2^64 value
     * @param denominator a base-2^64 value
     * @param uint64Count number of uint64 used in numerator and denominator
     * @param quotient    quotient[0, uint64Count) stores (numerator/denominator)'s quotient
     * @param remainder   remainder[0, uint64Count) stores (numerator/denominator)'s remainder
     */
    public static void divideUint(long[] numerator, long[] denominator, int uint64Count, long[] quotient, long[] remainder) {
        UintCore.setUint(numerator, uint64Count, remainder);
        divideUintInplace(remainder, denominator, uint64Count, quotient);
    }


    /**
     * Compute a/b = q + r, a is numerator, b is denominator, q is stored in quotient[0, 2) and r is stored in num numerator[0].
     *
     * @param numerator   a base-2^64 value, numerator[0, uint64Count) stores (numerator/denominator)'s remainder
     * @param denominator a base-2^64 value
     * @param uint64Count number of uint64 used in numerator and denominator
     * @param quotient    quotient[0, uint64Count) stores (numerator/denominator)'s quotient
     */
    public static void divideUintInplace(long[] numerator, long[] denominator, int uint64Count, long[] quotient) {

        assert uint64Count >= 0;
        assert quotient != numerator && quotient != denominator;

        if (uint64Count == 0) {
            return;
        }
        UintCore.setZeroUint(uint64Count, quotient);

        // significant bits
        int numeratorBits = UintCore.getSignificantBitCountUint(numerator, uint64Count);
        int denominatorBits = UintCore.getSignificantBitCountUint(denominator, uint64Count);
        // If numerator has fewer bits than denominator, then done.
        if (numeratorBits < denominatorBits) {
            return;
        }
        // Only perform computation up to last non-zero uint64s.
        uint64Count = UintCore.divideRoundUp(numeratorBits, UINT64_BITS);
        // 如果只有 64 bits，那么 直接在 long 下算
        if (uint64Count == 1) {
            quotient[0] = numerator[0] / denominator[0];
            numerator[0] -= quotient[0] * denominator[0];
            return;
        }

        long[] shiftedDenominator = new long[uint64Count];
        // difference is the updated numerator
        long[] difference = new long[uint64Count];
        int denominatorShift = numeratorBits - denominatorBits;
        leftShiftUint(denominator, denominatorShift, uint64Count, shiftedDenominator);
        denominatorBits += denominatorShift;

        int remainingShifts = denominatorShift;
        while (numeratorBits == denominatorBits) {
            // 分子和分母 最高位对齐，但 分子仍然可能小于分母
            long borrow = subUint(numerator, shiftedDenominator, uint64Count, difference);
            if (borrow > 0) {
                // numerator < shifted_denominator and MSBs are aligned,
                // so current quotient bit is zero and next one is definitely one.

                if (remainingShifts == 0) {
                    break;
                }
                // Effectively shift numerator left by 1 by instead adding
                // numerator to difference (to prevent overflow in numerator).
                addUint(difference, numerator, uint64Count, difference);
                // Adjust quotient and remaining shifts as a result of
                // shifting numerator.
                leftShiftUint(quotient, 1, uint64Count, quotient);
                remainingShifts--;
            }
            // Difference is the new numerator with denominator subtracted.

            // Update quotient to reflect subtraction.
            // 分子已经 减掉一个分母了, 所以商+1
            quotient[0] |= 1;
            // Determine amount to shift numerator to bring MSB in alignment
            // with denominator.
            numeratorBits = UintCore.getSignificantBitCountUint(difference, uint64Count);
            int numeratorShift = denominatorBits - numeratorBits;
            if (numeratorShift > remainingShifts) {
                // Clip the maximum shift to determine only the integer
                // (as opposed to fractional) bits.
                numeratorShift = remainingShifts;
            }
            // Shift and update numerator.
            if (numeratorBits > 0) {
                leftShiftUint(difference, numeratorShift, uint64Count, numerator);
                numeratorBits += numeratorShift;
            } else { // if numeratorBits = 0, mean difference = 0, so numerator = 0
                UintCore.setZeroUint(uint64Count, numerator);
            }
            // Adjust quotient and remaining shifts as a result of shifting numerator.
            // 因为分子左移了，商也要跟着左移
            leftShiftUint(quotient, numeratorShift, uint64Count, quotient);
            remainingShifts -= numeratorShift;
        }
        // Correct numerator (which is also the remainder) for shifting of
        // denominator, unless it is just zero.
        if (numeratorBits > 0) {
            rightShiftUint(numerator, denominatorShift, uint64Count, numerator);
        }
    }


    /**
     * Compute a/b = q + r, (128-bit/64-bit) a is numerator, b is denominator, q is stored in quotient[0, 2) and r is stored in num numerator[0].
     *
     * @param numerator   an unsigned 128-bit value, numerator[0] store the remainder
     * @param denominator an unsigned 64-bit value
     * @param quotient    quotient[0, 2) stores the (numerator/denominator)'s quotient
     */
    public static void divideUint128Uint64InplaceGeneric(long[] numerator, long denominator, long[] quotient) {

        assert numerator != null;
        assert denominator != 0;
        assert quotient != null;
        assert numerator != quotient;

        // expect 128 bits input
        int uint64Count = 2;
        // 初始化商 为 0
        quotient[0] = 0;
        quotient[1] = 0;

        // 分子和分母 bit-count
        int numeratorBits = UintCore.getSignificantBitCountUint(numerator, uint64Count);
        int denominatorBits = UintCore.getSignificantBitCount(denominator);
        // 分子 < 分母, 商为0， 余数就是分子本身
        if (numeratorBits < denominatorBits) {
            return;
        }
        // 分子实际的 uint64 count
        uint64Count = UintCore.divideRoundUp(numeratorBits, UINT64_BITS);
        // 如果分子 uint64Count = 1, 那就是 64-bit / 64-bit, 可以直接进行除法, 可以理解为 base-2^64 下的 单精度除法
        if (uint64Count == 1) {
            // 可以用带余除法来理解：a = q * b + r , 其中 a 是分子，b 是分母， q 是商， r 是余数
            // q = a / b
            quotient[0] = numerator[0] / denominator;
            // r = a - q * b
            numerator[0] -= quotient[0] * denominator;
            return;
        }
        // 如果 uint64Count == 2, 那么就执行下面的更通用的逻辑

        // 先拷贝分母
        long[] shiftedDenominator = new long[uint64Count];
        shiftedDenominator[0] = denominator;
        // 记录计算过程中的差
        long[] difference = new long[uint64Count];
        // 分子和分母的 bit-count 的差值, numeratorBits >= denominatorBits
        int denominatorShift = numeratorBits - denominatorBits;
        // 左移 shiftedDenominator, 与分子对齐 最高有效位
        leftShiftUint128(shiftedDenominator, denominatorShift, shiftedDenominator);
        denominatorBits += denominatorShift;

        // Perform bit-wise division algorithm.
        int remainingShifts = denominatorShift;
        while (numeratorBits == denominatorBits) {

            // 分母与分子对齐后，分子是有可能小于 分母的
            // difference = numerator - shiftedDenominator
            long borrow = subUint(numerator, shiftedDenominator, uint64Count, difference);
            if (borrow > 0) {
                // 如果 numerator < shiftedDenominator, 并且需要处理的shifts 已经完成，则
                if (remainingShifts == 0) {
                    break;
                }
                // difference = difference + numerator
                addUint(difference, numerator, uint64Count, difference);

                // 这里的逻辑把 quotient 整体看待为 128-bit, 这里就是整体的 左移 1-bit
                quotient[1] = (quotient[1] << 1) | (quotient[0] >>> (UINT64_BITS - 1));
                quotient[0] <<= 1;
                remainingShifts--;
            }
            // 到这里, difference = 分子 - 分母, difference 就是 新的 分子，回想
            // 更新分子 bit-count
            numeratorBits = UintCore.getSignificantBitCountUint(difference, uint64Count);
            // 分子的
            int numeratorShift = Math.min(denominatorBits - numeratorBits, remainingShifts);
            numerator[0] = 0;
            numerator[1] = 0;

            if (numeratorBits > 0) {
                leftShiftUint128(difference, numeratorShift, numerator);
                numeratorBits += numeratorShift;
            }
            // 更新商，因为完成了一次 减法
            quotient[0] |= 1;

            leftShiftUint128(quotient, numeratorShift, quotient);
            remainingShifts -= numeratorShift;
        }

        if (numeratorBits > 0) {
            rightShiftUint128(numerator, denominatorShift, numerator);
        }

    }


    /**
     * Compute a/b = q + r (128-bit/64-bit), a is numerator, b is denominator, q is stored in quotient[0, 2) and r is stored in num numerator[0].
     *
     * @param numerator   an unsigned 128-bit value, numerator[0] store the remainder
     * @param denominator an unsigned 64-bit value
     * @param quotient    quotient[0, 2) stores the (numerator/denominator)'s quotient
     */
    public static void divideUint128Inplace(long[] numerator, long denominator, long[] quotient) {

        divideUint128Uint64InplaceGeneric(numerator, denominator, quotient);
    }


    /**
     * Compute a/b = q + r (192-bit/64-bit), a is numerator, b is denominator, q is stored in quotient[0, 3) and r is stored in num numerator[0].
     *
     * @param numerator   an unsigned 192-bit value, numerator[0] store the remainder
     * @param denominator an unsigned 64-bit value
     * @param quotient    quotient[0, 3) stores the (numerator/denominator)'s quotient
     */
    public static void divideUint192Inplace(long[] numerator, long denominator, long[] quotient) {

        assert numerator != null;
        assert denominator != 0;
        assert quotient != null;
        assert numerator != quotient;

        int uint64Count = 3;
        quotient[0] = 0;
        quotient[1] = 0;
        quotient[2] = 0;


        int numeratorBits = UintCore.getSignificantBitCountUint(numerator, uint64Count);
        int denominatorBits = UintCore.getSignificantBitCount(denominator);

        if (numeratorBits < denominatorBits) {
            return;
        }
        uint64Count = UintCore.divideRoundUp(numeratorBits, UINT64_BITS);
        if (uint64Count == 1) {
            quotient[0] = numerator[0] / denominator;
            numerator[0] -= quotient[0] * denominator;
            return;
        }

        long[] shiftedDenominator = new long[uint64Count];
        shiftedDenominator[0] = denominator;

        long[] difference = new long[uint64Count];
        int denominatorShift = numeratorBits - denominatorBits;

        leftShiftUint192(shiftedDenominator, denominatorShift, shiftedDenominator);

        denominatorBits += denominatorShift;

        int remainingShifts = denominatorShift;

        while (numeratorBits == denominatorBits) {
            long borrow = subUint(numerator, shiftedDenominator, uint64Count, difference);
            if (borrow > 0) {
                if (remainingShifts == 0) {
                    break;
                }
                addUint(difference, numerator, uint64Count, difference);
                // quotient 整体左移 1-bit
                leftShiftUint192(quotient, 1, quotient);
                remainingShifts--;
            }
            quotient[0] |= 1;

            numeratorBits = UintCore.getSignificantBitCountUint(difference, uint64Count);
            int numeratorShift = Math.min(denominatorBits - numeratorBits, remainingShifts);

            if (numeratorBits > 0) {
                leftShiftUint192(difference, numeratorShift, numerator);
                numeratorBits += numeratorShift;
            } else {
                UintCore.setZeroUint(uint64Count, numerator);
            }

            leftShiftUint192(quotient, numeratorShift, quotient);
            remainingShifts -= numeratorShift;
        }

        if (numeratorBits > 0) {
            rightShiftUint192(numerator, denominatorShift, numerator);
        }
    }

    /**
     * compute (operand >> shiftAmount) and store it in result[0, 2)
     *
     * @param operand     a base-2^64 value, length is 2, can be viewed as an unsigned 128-bit value
     * @param shiftAmount bit-count of right shift
     * @param result      result[0, 2) store the (operand >> shiftAmount)
     */
    public static void rightShiftUint128(long[] operand, int shiftAmount, long[] result) {

        assert operand.length == 2;
        assert shiftAmount >= 0 && shiftAmount <= 3 * UINT64_BITS;
        assert operand.length == result.length;

        if (shiftAmount == 0) {
            System.arraycopy(operand, 0, result, 0, operand.length);
            return;
        }

        // shiftAmount >= 64-bit, one word shift
        if ((shiftAmount & UINT64_BITS) > 0) {
            result[0] = operand[1];
            result[1] = 0;
        } else { // shiftAmount in [0， 64） no word shift
            result[1] = operand[1];
            result[0] = operand[0];
        }
        int bitShiftAmount = shiftAmount & (UINT64_BITS - 1);
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT64_BITS - bitShiftAmount;
            result[0] = (result[0] >>> bitShiftAmount) | (result[1] << negBitShiftAmount);
            result[1] = result[1] >>> bitShiftAmount;
        }
    }

    /**
     * compute (operand >> shiftAmount) and store it in result[0, 3)
     *
     * @param operand     a base-2^64 value, length is 3, can be viewed as an unsigned 192-bit value
     * @param shiftAmount bit-count of right shift
     * @param result      result[0, 3) store the (operand >> shiftAmount)
     */
    public static void rightShiftUint192(long[] operand, int shiftAmount, long[] result) {

        assert operand.length == 3;
        assert shiftAmount >= 0 && shiftAmount <= 3 * UINT64_BITS;
        assert operand.length == result.length;

        if (shiftAmount == 0) {
            System.arraycopy(operand, 0, result, 0, operand.length);
            return;
        }

        // 右移 超过 128 bits
        if ((shiftAmount & UINT64_BITS << 1) > 0) {
            result[0] = operand[2];
            result[1] = 0;
            result[2] = 0;
        } else if ((shiftAmount & UINT64_BITS) > 0) { // 右移在 [64, 128) 之间
            result[0] = operand[1];
            result[1] = operand[2];
            result[2] = 0;
        } else { // [0， 64） no word shift
            result[2] = operand[2];
            result[1] = operand[1];
            result[0] = operand[0];
        }
        int bitShiftAmount = shiftAmount & (UINT64_BITS - 1);
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT64_BITS - bitShiftAmount;
            result[0] = (result[0] >>> bitShiftAmount) | (result[1] << negBitShiftAmount);
            result[1] = (result[1] >>> bitShiftAmount) | (result[2] << negBitShiftAmount);
            result[2] = result[2] >>> bitShiftAmount;
        }
    }

    /**
     * compute (operand << shiftAmount) and store it in result[0, 2).
     *
     * @param operand     a base^64 value, length is 2, can view it as a 128-bit value
     * @param shiftAmount the bit count of left shift
     * @param result      result[0, 2) store the (operand << shiftAmount)
     */
    public static void leftShiftUint128(long[] operand, int shiftAmount, long[] result) {

        assert shiftAmount >= 0 && shiftAmount <= 2 * UINT64_BITS : "shiftAmount: " + shiftAmount;

        if (shiftAmount == 0) {
            System.arraycopy(operand, 0, result, 0, operand.length);
            return;
        }
        // only shiftAmount >= 64, this condition will be true
        if ((shiftAmount & UINT64_BITS) > 0) {
            result[1] = operand[0];
            result[0] = 0;
        } else {
            result[1] = operand[1];
            result[0] = operand[0];
        }
        // 计算 shiftAmount % 64
        int bitShiftAmount = shiftAmount & (UINT64_BITS - 1);
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT64_BITS - bitShiftAmount;
            // 高位左移，低位的高n-bit 移入高位
            result[1] = (result[1] << bitShiftAmount) | (result[0] >>> negBitShiftAmount);
            // 低位左移
            result[0] = result[0] << bitShiftAmount;
        }
    }

    /**
     * compute (operand << shiftAmount) and store it in result[0, 2).
     *
     * @param operand     a base-2^64 value, length is 3, can be view as an unsigned 192-bit value
     * @param shiftAmount the bit count of left shift
     * @param result      result[0, 3) store the (operand << shiftAmount)
     */
    public static void leftShiftUint192(long[] operand, int shiftAmount, long[] result) {
        assert shiftAmount >= 0 && shiftAmount <= 3 * UINT64_BITS : "shiftAmount: " + shiftAmount;

        if (shiftAmount == 0) {
            System.arraycopy(operand, 0, result, 0, operand.length);
            return;
        }
        // only shiftAmount >= 128, this condition will be true
        if ((shiftAmount & (UINT64_BITS << 1)) > 0) {
            result[2] = operand[0];
            result[1] = 0;
            result[0] = 0;
            // only shiftAmount >= 64, this condition will be true
        } else if ((shiftAmount & UINT64_BITS) > 0) {
            result[2] = operand[1];
            result[1] = operand[0];
            result[0] = 0;
        } else {
            result[2] = operand[2];
            result[1] = operand[1];
            result[0] = operand[0];
        }
        int bitShiftAmount = shiftAmount & (UINT64_BITS - 1);
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT64_BITS - bitShiftAmount;
            // right shift must be use unsigned right shift
            result[2] = (result[2] << bitShiftAmount) | (result[1] >>> negBitShiftAmount);
            result[1] = (result[1] << bitShiftAmount) | (result[0] >>> negBitShiftAmount);
            result[0] = result[0] << bitShiftAmount;
        }
    }

    /**
     * compute (a - b) ---> (diff, borrow), diff is stored in result[0], borrow is returned.
     *
     * @param a      an unsigned 64-bit value
     * @param b      an unsigned 64-bit value
     * @param result result[0] store (a - b)
     * @return (a - b)'s borrow, which value is 0 or 1
     */
    public static long subUint64(long a, long b, long[] result) {
        result[0] = a - b;
        // if b > a, then must produce borrow
        return Long.compareUnsigned(b, a) > 0 ? 1 : 0;
    }

    /**
     * compute (a - b - borrow) ---> (diff, borrow), diff is stored in result[0], borrow is returned.
     *
     * @param a      an unsigned 64-bit value
     * @param b      an unsigned 64-bit value
     * @param borrow given borrow, 0 or 1
     * @param result result[0] store a - b - borrow
     * @return (a - b - borrow)'s borrow, which value is 0 or 1
     */
    public static long subUint64(long a, long b, long borrow, long[] result) {
        return subUint64Generic(a, b, borrow, result);
    }

    /**
     * compute (a - b - borrow) ---> (diff, borrow), diff is stored in result[0], borrow is returned.
     *
     * @param a      an unsigned 64-bit value
     * @param b      an unsigned 64-bit value
     * @param borrow given borrow, 0 or 1
     * @param result result[0] store the (a-b)
     * @return (a - b)'s borrow, which value is 0 or 1
     */
    public static long subUint64Generic(long a, long b, long borrow, long[] result) {

        long diff = a - b;
        result[0] = diff - borrow;
        // diff > a, must produce borrow,
        // diff < borrow, only in this case: borrow = 1, diff = 0, will produce new borrow
        return (Long.compareUnsigned(diff, a) > 0 || Long.compareUnsigned(diff, borrow) < 0) ? 1 : 0;
    }


    /**
     * compute (as -b) ---> (diff, borrow), diff is stored in result[0, uint64Count), borrow is returned.
     *
     * @param as          a base-2^64 value
     * @param uint64Count number of uint64 used in as
     * @param b           an unsigned 64-bit value
     * @param result      result[0, uint64Count) store (as - b)
     * @return (as - b)'s borrow, which value is 0 or 1
     */
    public static long subUint(long[] as, int uint64Count, long b, long[] result) {

        assert uint64Count > 0;
        long[] tmp = new long[1];
        long borrow;
        borrow = subUint64(as[0], b, tmp);
        result[0] = tmp[0];

        int i = 1;
        while (--uint64Count > 0) {
            borrow = subUint64(as[i], 0, borrow, tmp);
            result[i] = tmp[0];
            i++;
        }
        return borrow;
    }

    /**
     * compute (as -bs) ---> (diff, borrow), diff is stored in result[0, uint64Count), borrow is returned.
     *
     * @param as          a base-2^64 value
     * @param bs          a base-2^64 value
     * @param uint64Count number of uint64 used in as and bs
     * @param result      result[0, uint64Count) store (as - bs)
     * @return (as - bs)'s borrow, which value is 0 or 1
     */
    public static long subUint(long[] as, long[] bs, int uint64Count, long[] result) {

        assert uint64Count > 0;
        long[] tmp = new long[1];
        long borrow = subUint64(as[0], bs[0], tmp);
        result[0] = tmp[0];
        int i = 1;
        while (--uint64Count > 0) {
            borrow = subUint64(as[i], bs[i], borrow, tmp);
            result[i] = tmp[0];
            i++;
        }
        return borrow;
    }

    /**
     * compute (as -bs) ---> (diff, borrow), diff is stored in result[0, uint64Count), borrow is returned.
     *
     * @param as            a base-2^64 value
     * @param asUint64Count number of uint64 used in as
     * @param bs            a base-2^64 value
     * @param bsUint64Count number of uint64 used in bs
     * @param borrow        given borrow, which value is 0 or 1
     * @param uint64Count   number of uint64 used in result
     * @param result        result[0, uint64Count) store (as - bs)
     * @return (as - bs)'s borrow
     */
    public static long subUint(long[] as, int asUint64Count, long[] bs, int bsUint64Count, long borrow, int uint64Count, long[] result) {
        assert uint64Count > 0;
        long[] tmp = new long[1];
        for (int i = 0; i < uint64Count; i++) {
            borrow = subUint64(
                i < asUint64Count ? as[i] : 0,
                i < bsUint64Count ? bs[i] : 0,
                borrow,
                tmp
            );
            result[i] = tmp[0];
        }
        return borrow;
    }


    /**
     * compute (a + b) ---> (sum, carry), sum is stored in result[0], carry is returned.
     *
     * @param a      an unsigned 64-bit value
     * @param b      an unsigned 64-bit value
     * @param result result[0] store lower 64-bit part of (a + b)
     * @return (a + b)'s carry, which value is 0 or 1.
     */
    public static long addUint64(long a, long b, long[] result) {
        // 不需要这个 assert, 反正结果保存在 result[0], 至于 result 多长, 不需要关心
        result[0] = a + b;
        return Long.compareUnsigned(result[0], a) < 0 ? 1 : 0;
    }

    /**
     * compute (a + b + carry) ---> (sum, carry), sum is stored in result[0] and carry is returned..
     *
     * @param a      a 64-bit value
     * @param b      a 64-bit value
     * @param carry  given carry, 0 or 1
     * @param result result[0] store lower 64-bit part of (a + b + carry)
     * @return (a + b + carry)'s carry, which value is 0 or 1.
     */
    public static long addUint64(long a, long b, long carry, long[] result) {
        return addUint64Generic(a, b, carry, result);
    }

    /**
     * compute (a + b + carry) ---> (sum, carry), sum is stored in result[0] and carry is returned.
     *
     * @param a      a 64-bit value
     * @param b      a 64-bit value
     * @param carry  given carry, 0 or 1
     * @param result result[0] store lower 64-bit part of (a + b + carry)
     * @return (a + b + carry)'s carry, which value is 0 or 1
     */
    public static long addUint64Generic(long a, long b, long carry, long[] result) {
        long sum = a + b;
        result[0] = sum + carry;
        boolean isCarry = Long.compareUnsigned(sum, a) < 0 || (sum == -1 && carry == 1);
        return isCarry ? 1 : 0;
    }


    /**
     * compute (as + b) --> (sum, carry), sum store in result[0, uint64Count), carry is returned.
     *
     * @param as          a base-2^64 value
     * @param uint64Count number of uint64 used in as
     * @param b           an unsigned 64-bit value
     * @param result      a base-2^64 value, which store the (as + b)
     * @return (as + b)'s carry, which value is 0 or 1
     */
    public static long addUint(long[] as, int uint64Count, long b, long[] result) {

        assert uint64Count > 0;
        long[] tmp = new long[1];
        long carry = addUint64(as[0], b, tmp);
        result[0] = tmp[0];
        int i = 1;
        while (--uint64Count > 0) {
            carry = addUint64(as[i], 0, carry, tmp);
            result[i] = tmp[0];
            i++;
        }
        return carry;
    }

    /**
     * compute (as + bs) --> (sum, carry), sum is stored in result[0, uint64Count), carry is returned.
     *
     * @param as          a base-2^64 value
     * @param bs          a base-2^64 value
     * @param uint64Count number of uint64 used in as and bs
     * @param result      a base-2^64 value, result[0, uint64Count) store the (as + bs)
     * @return (as + bs)'s carry, which value is 0 or 1
     */
    public static long addUint(long[] as, long[] bs, int uint64Count, long[] result) {

        assert uint64Count > 0;

        long[] tmp = new long[1];
        long carry;
        carry = addUint64(as[0], bs[0], tmp);
        result[0] = tmp[0];

        int i = 1;
        while (--uint64Count > 0) {
            carry = addUint64(as[i], bs[i], carry, tmp);
            result[i] = tmp[0];
            i++;
        }
        return carry;
    }

    /**
     * compute (as[0, asUint64Count) + bs[0, bsUint64Count) ---> (sum, carry),
     * sum is stored in result[0, uint64Count) and carry is returned.
     *
     * @param as            a base-2^64 value
     * @param asUint64Count number of uint64 used in as
     * @param bs            a base-2^64 value
     * @param bsUint64Count number of uint64 used in bs
     * @param carry         given carry, 0 or 1
     * @param uint64Count   number of uint64 used in result
     * @param result        result[0, uint64Count) store the (as + bs)
     * @return (as + bs)'s carry, which value is 0 or 1
     */
    public static long addUint(long[] as, int asUint64Count, long[] bs, int bsUint64Count, long carry, int uint64Count, long[] result) {
        assert uint64Count > 0;
        long[] tmp = new long[1];
        for (int i = 0; i < uint64Count; i++) {
            carry = addUint64(
                i < asUint64Count ? as[i] : 0,
                i < bsUint64Count ? bs[i] : 0,
                carry,
                tmp
            );
            result[i] = tmp[0];
        }
        return carry;
    }


    /**
     * compute exponent ^ exponent
     *
     * @param operand  a unsigned 64-bit value
     * @param exponent exponent
     * @return operand ^ exponent
     */
    public static long exponentUint(long operand, long exponent) {
        if (operand == 0) {
            return 0;
        }
        if (exponent == 0) {
            return 1;
        }
        if (operand == 1) {
            return 1;
        }
        // Perform binary exponentiation.
        long power = operand;
        long product;
        long intermediate = 1;

        // Initially: power = operand and intermediate = 1, product irrelevant.
        while (true) {
            if ((exponent & 1) > 0) {
                product = power * intermediate;
                // swap
                intermediate = product;
            }
            exponent >>>= 1;
            if (exponent == 0) {
                break;
            }
            product = power * power;
            //swap
            power = product;
        }
        return intermediate;
    }

}
