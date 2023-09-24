package edu.alibaba.mpc4j.crypto.fhe.zq;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/8/3
 */
public class UintArithmetic {


    public static final int UINT64_BITS = 64;



    public static void andUint(long[] as, long[] bs, int uint64Count, long[] result) {

        assert uint64Count > 0;
        assert as.length == uint64Count && result.length == uint64Count;
        assert as.length == bs.length;

        while (--uint64Count >= 0) {
            result[uint64Count] = as[uint64Count] & bs[uint64Count];
        }
    }

    public static void orUint(long[] as, long[] bs, int uint64Count, long[] result) {

        assert uint64Count > 0;
        assert as.length == uint64Count && result.length == uint64Count;
        assert as.length == bs.length;

        while (--uint64Count >= 0) {
            result[uint64Count] = as[uint64Count] | bs[uint64Count];
        }
    }

    public static void xorUint(long[] as, long[] bs, int uint64Count, long[] result) {

        assert uint64Count > 0;
        assert as.length == uint64Count && result.length == uint64Count;
        assert as.length == bs.length;

        while (--uint64Count >= 0) {
            result[uint64Count] = as[uint64Count] ^ bs[uint64Count];
        }
    }

    public static void notUint(long[] operand, int uint64Count, long[] result) {

        assert uint64Count > 0;
        assert operand.length >= uint64Count && result.length >= uint64Count;
        while (--uint64Count >= 0) {
            result[uint64Count] = ~operand[uint64Count];
        }
    }



    /**
     *
     * @param operand
     * @param uint64Count
     * @param result (operand + 1) / 2
     */
    public static void halfRoundUpUint(long[] operand, int uint64Count, long[] result) {

        if (uint64Count <= 0) {
            return;
        }
        // Set result to (operand + 1) / 2. To prevent overflowing operand, right shift
        // and then increment result if low-bit of operand was set.
        long lowBitSet = operand[0] & 1;

        for (int i = 0; i < uint64Count - 1; i++) {
            result[i] = (operand[i] >>> 1) | (operand[i + 1] << (UINT64_BITS - 1));
        }
        result[uint64Count - 1] = operand[uint64Count - 1] >>> 1;

        if (lowBitSet > 0) {
            incrementUint(result, uint64Count, result);
        }
    }





    /**
     * @param operand
     * @param shiftAmount
     * @param uint64Count
     * @param result
     */
    public static void rightShiftUint(long[] operand, int shiftAmount, int uint64Count, long[] result) {

        assert uint64Count > 0;
        assert shiftAmount >= 0 && shiftAmount <= uint64Count * UINT64_BITS;

        // How many words to shift, one words is 64 bits
        int uint64ShiftAmount = shiftAmount / UINT64_BITS;
        // shift words
        for (int i = 0; i < uint64Count - uint64ShiftAmount; i++) {
            result[i] = operand[i + uint64ShiftAmount];
        }
        for (int i = uint64Count - uint64ShiftAmount; i < uint64Count; i++) {
            result[i] = 0;
        }
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
     * left shift the operand with length uint64Count to given shiftAmount bits
     *
     * @param operand
     * @param shiftAmount
     * @param uint64Count
     * @param result
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
     * @param as
     * @param uint64Count
     * @param result
     * @return -long[]
     */
    public static void negateUint(long[] as, int uint64Count, long[] result) {

        assert uint64Count > 0;

        long[] tmp = new long[1];
        long carry;
        carry = addUint64(~as[0], 1, tmp);
        result[0] = tmp[0];
        int i = 1;
        while (--uint64Count > 0) {
            carry = addUint64(~as[i], 0, carry, tmp);
            result[i] = tmp[0];
            i++;
        }
    }


    /**
     * @param as
     * @param uint64Count
     * @param result
     * @return long[] - 1
     */
    public static long decrementUint(long[] as, int uint64Count, long[] result) {
        assert uint64Count > 0;
        return subUint(as, uint64Count, 1, result);
    }

    /**
     * @param as
     * @param uint64Count
     * @param result
     * @return long[] + 1
     */
    public static long incrementUint(long[] as, int uint64Count, long[] result) {
        assert uint64Count > 0;
        return addUint(as, uint64Count, 1, result);
    }


    /**
     *
     * @param as 128-bit
     * @param bs 128-bit
     * @param result sum of 128-bit + 128-bit
     * @return carry
     */
    public static long addUint128(long[] as, long[] bs, long[] result) {
        assert as.length == 2;
        assert as.length == bs.length;
        assert result.length == 2;

        long[] tmp = new long[1];

        long carry = addUint64(as[0], bs[0], tmp);
        result[0] = tmp[0];
        carry = addUint64(as[1], bs[1], carry, tmp);
        result[1] = tmp[0];

        return carry;
    }


    /**
     * long[n] * long[n] = long[n], high n uint64 count do not save
     * @param as
     * @param bs
     * @param uint64Count
     * @param result
     */
    public static void multiplyTruncateUint(long[] as, long[] bs, int uint64Count, long[] result) {
        multiplyUint(as, uint64Count, bs, uint64Count, uint64Count, result);
    }
    /**
     * long[n] * long[n] = long[2n]
     * @param as
     * @param bs
     * @param uint64Count
     * @param result
     */
    public static void multiplyUint(long[] as, long[] bs, int uint64Count, long[] result) {
        multiplyUint(as, uint64Count, bs, uint64Count, uint64Count * 2, result);
    }

    /**
     *  return long[] * long[] = long[]
     * @param as
     * @param asUint64Count
     * @param bs
     * @param bsUint64Count
     * @param resultUint64Count
     * @param result
     */
    public static void multiplyUint(long[] as, int asUint64Count, long[] bs, int bsUint64Count, int resultUint64Count, long[] result) {
        assert asUint64Count >= 0;
        assert bsUint64Count >= 0;
        assert resultUint64Count > 0;
        assert result != as && result != bs;

        if (asUint64Count == 0 || bsUint64Count == 0){
            UintCore.setZeroUint(resultUint64Count, result);
            return;
        }
        if (resultUint64Count == 1) {
            result[0] = as[0] * bs[0];
            return;
        }
        // the significant
        asUint64Count = UintCore.getSignificantUint64CountUint(as, asUint64Count);
        bsUint64Count = UintCore.getSignificantUint64CountUint(bs, bsUint64Count);
        // more fast
        if (asUint64Count == 1) { // only index = 0 is meaningful computation
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
     * return long[] * long = long[]
     * @param as
     * @param asUint64Count
     * @param b
     * @param resultUint64Count
     * @param result
     */
    public static void multiplyUint(long[] as, int asUint64Count, long b, int resultUint64Count, long[] result) {

        assert asUint64Count >= 0;
        assert resultUint64Count > 0;
        assert result != as;

        if (asUint64Count == 0 || b == 0) {
            UintCore.setZeroUint(resultUint64Count, result);
            return;
        }
        if (resultUint64Count == 1) {
            result[0] = as[0] * b;
            return;
        }
        // clear result
        UintCore.setZeroUint(resultUint64Count, result);
        // Multiply
        long carry = 0;
        int asIndexMax = Math.min(asUint64Count, resultUint64Count);

        int asIndex = 0;
        // 逐位相乘
        for (; asIndex < asIndexMax; asIndex++) {
            long[] mulTemp = new long[2];// index = 0 is the low-64bit, index = 1 is the high 64 bits
            multiplyUint64(as[asIndex], b, mulTemp);
            // addTemp[0] is the add result, addTemp[1] is the add carry
            long[] addTemp = new long[1];
            long carryTmp = addUint64(mulTemp[0], carry, 0, addTemp);
            carry = mulTemp[1] + carryTmp;
            result[asIndex] = addTemp[0];
        }
        if (asIndexMax < resultUint64Count) {
            result[asIndex] = carry;
        }
    }

    /**
     * prod operation, result = \Prod_i operands[i]
     * @param operands
     * @param count
     * @param result
     */
    public static void multiplyManyUint64(long[] operands, int count, long[] result) {

        assert operands.length >= count;
//        assert result.length >= count;

        if (count == 0){
            return;
        }
        UintCore.setUint(operands[0], count, result);

        long[] tempMpi = new long[count];
        for (int i = 1; i < count; i++) {
            multiplyUint(result, i, operands[i], i + 1, tempMpi);
            UintCore.setUint(tempMpi, i + 1, result);
        }
    }

    /**
     *  result = \Prod_i operands[i], i != except
     * @param operands
     * @param count
     * @param except
     * @param result
     */
    public static void multiplyManyUint64Except(long[] operands, int count, int except, long[] result) {

        assert count >= 1;
        assert except >= 0 && except < count;

        // empty product, res = 1
        // when count == 1, valid except must be 0
        if (count == 1){
            UintCore.setUint(1, count, result);
            return;

        }
        // set result is operand[0] unless except = 0
        UintCore.setUint( except == 0 ? 1 : operands[0], count, result);

        long[] tempMpi = new long[count];
        for (int i = 1; i < count; i++) {
            if (i != except) {
                multiplyUint(result, i, operands[i], i + 1, tempMpi);
                UintCore.setUint(tempMpi, i + 1, result);
            }
        }
    }



    /**
     * function ref: https://learn.microsoft.com/en-us/cpp/intrinsics/umul128?view=msvc-170
     * basic idea is :
     * a = (2^32 a + aRight)
     * b = (2^32 b + bRight)
     * the a * b = (2^32 a + aRight) *  (2^32 b + bRight)
     *
     * @param a
     * @param b
     * @param result128
     * @return a * b = 128 bits, store in long[2]
     */
    public static void multiplyUint64(long a, long b, long[] result128) {

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
     * function ref: https://learn.microsoft.com/en-us/cpp/intrinsics/umul128?view=msvc-170
     * basic idea is :
     * a = (2^32 a + aRight)
     * b = (2^32 b + bRight)
     * the a * b = (2^32 a + aRight) *  (2^32 b + bRight)
     *
     * @param a 64 bits
     * @param b 64 bits
     * @return thr high 64 bits of a * b
     */
    public static long multiplyUint64Hw64(long a, long b) {

        long result;

        long aRight = a & 0x00000000FFFFFFFFL;
        long bRight = b & 0x00000000FFFFFFFFL;
        a >>>= 32;
        b >>>= 32;

//        System.out.println("aRight: " + Long.toHexString(aRight));
//        System.out.println("bRight: " + Long.toHexString(bRight));
//        System.out.println("a >>>=32: " + Long.toHexString(a));
//        System.out.println("b >>>=32: " + Long.toHexString(b));

        long middle1 = a * bRight;
//        System.out.println("middle1 = a * bRight: " + Long.toHexString(middle1));


        long middle, carry;
        long[] tmp = new long[1];
        carry = addUint64(middle1, b * aRight, tmp);
        middle = tmp[0];
//        System.out.println("b * aRight: " + Long.toHexString(b * aRight));
//        System.out.println("middle1 + b * aRight: " + Long.toHexString(middle));
//        System.out.println("middle1 + b * aRight(carry): " + Long.toHexString(carry));


        long left = a * b + (carry << 32);
        long right = aRight * bRight;
        long tmpSum = (right >>> 32) + (middle & 0x00000000FFFFFFFFL);

//        System.out.println("left: " + Long.toHexString(left));
//        System.out.println("right: " + Long.toHexString(right));
//        System.out.println("tmpSum: " + Long.toHexString(tmpSum));

        result = left + (middle >>> 32) + (tmpSum >>> 32);

//        System.out.println("middle >> 32: " + Long.toHexString(middle >>> 32));
//        System.out.println("tmpSum >> 32: " + Long.toHexString(tmpSum >>> 32));
//        System.out.println("result: " + Long.toHexString(result));

        return result;
    }

    public static long multiplyUint64Hw64Generic(long a, long b) {

        long result;

        long aRight = a & 0x00000000FFFFFFFFL;
        long bRight = b & 0x00000000FFFFFFFFL;
        a >>>= 32;
        b >>>= 32;

//        System.out.println("aRight: " + Long.toHexString(aRight));
//        System.out.println("bRight: " + Long.toHexString(bRight));
//        System.out.println("a >>>=32: " + Long.toHexString(a));
//        System.out.println("b >>>=32: " + Long.toHexString(b));

        long middle1 = a * bRight;
//        System.out.println("middle1 = a * bRight: " + Long.toHexString(middle1));


        long middle, carry;
        long[] tmp = new long[1];
        carry = addUint64(middle1, b * aRight, tmp);
        middle = tmp[0];
//        System.out.println("b * aRight: " + Long.toHexString(b * aRight));
//        System.out.println("middle1 + b * aRight: " + Long.toHexString(middle));
//        System.out.println("middle1 + b * aRight(carry): " + Long.toHexString(carry));


        long left = a * b + (carry << 32);
        long right = aRight * bRight;
        long tmpSum = (right >>> 32) + (middle & 0x00000000FFFFFFFFL);

//        System.out.println("left: " + Long.toHexString(left));
//        System.out.println("right: " + Long.toHexString(right));
//        System.out.println("tmpSum: " + Long.toHexString(tmpSum));

        result = left + (middle >>> 32) + (tmpSum >>> 32);

//        System.out.println("middle >> 32: " + Long.toHexString(middle >>> 32));
//        System.out.println("tmpSum >> 32: " + Long.toHexString(tmpSum >>> 32));
//        System.out.println("result: " + Long.toHexString(result));

        return result;
    }

    /**
     * TODO: 需要重点优化的函数，这里太多递归，需要 new 太多新的数组了
     * @param operand1
     * @param operand2
     * @param accumulator
     * @param count
     */
    public static void multiplyAccumulateUint64(long[] operand1, long[] operand2, long[] accumulator, int count) {

        if (count == 0) return;

        long[] qWord = new long[2];
        multiplyUint64(operand1[0], operand2[0], qWord);
        // todo: in java, no array slice, each time need to new a Array object, consider better implementation?
        long[] c1 = Arrays.copyOfRange(operand1, 1, count);
        long[] c2 = Arrays.copyOfRange(operand2, 1, count);
        multiplyAccumulateUint64(c1, c2, accumulator, count - 1);
        addUint128(qWord, accumulator, accumulator);
    }

    /**
     * avoid numerator change, the remainder store in remainder
     * @param numerator
     * @param denominator
     * @param uint64Count
     * @param quotient
     * @param remainder
     */
    public static void divideUint(long[] numerator, long[] denominator, int uint64Count, long[] quotient, long[] remainder) {
        UintCore.setUint(numerator, uint64Count, remainder);
        divideUintInplace(remainder, denominator, uint64Count, quotient);
    }


    /**
     * long[] / long[]
     * @param numerator
     * @param denominator
     * @param uint64Count
     * @param quotient
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
            }else { // if numeratorBits = 0, mean difference = 0, so numerator = 0
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
     * 128 bits / 64 bits = long[2] / long
     * @param numerator
     * @param denominator
     * @param quotient
     */
    public static void divideUint128Inplace(long[] numerator, long denominator, long[] quotient) {

        if (denominator == 0) {
            throw new IllegalArgumentException("denominator can not be zero");
        }
        if (numerator == quotient) {
            throw new IllegalArgumentException("quotient cannot point to same value as numerator");
        }
        // expect 128 bits input
        int uint64Count = 2;

        quotient[0] = 0;
        quotient[1] = 0;


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

        leftShiftUint128(shiftedDenominator, denominatorShift, shiftedDenominator);
        denominatorBits += denominatorShift;
        // Perform bit-wise division algorithm.
        int remainingShifts = denominatorShift;
        while (numeratorBits == denominatorBits) {

            long borrow = subUint(numerator, shiftedDenominator, uint64Count, difference);

            if (borrow > 0) {
                if (remainingShifts == 0) {
                    break;
                }
                addUint(difference, numerator, uint64Count, difference);
                // 这里的实现逻辑和前面的都不太一样啊
                quotient[1] = (quotient[1] << 1) | (quotient[0] >>> (UINT64_BITS - 1));
                quotient[0] <<= 1;
                remainingShifts--;
            }


            numeratorBits = UintCore.getSignificantBitCountUint(difference, uint64Count);
            int numeratorShift = denominatorBits - numeratorBits;
            if (numeratorShift > remainingShifts) {
                numeratorShift = remainingShifts;
            }
            numerator[0] = 0;
            numerator[1] = 0;

            if (numeratorBits > 0) {
                leftShiftUint128(difference, numeratorShift, numerator);
                numeratorBits += numeratorShift;
            }
            quotient[0] |= 1;

            leftShiftUint128(quotient, numeratorShift, quotient);
            remainingShifts -= numeratorShift;
        }
        if (numeratorBits > 0) {
            rightShiftUint128(numerator, denominatorShift, numerator);
        }
    }





    /**
     * 192 bits / 64 bits = long[3] / long
     * @param numerator
     * @param denominator
     * @param quotient
     */
    public static void divideUint192Inplace(long[] numerator, long denominator, long[] quotient) {

        if (denominator == 0) {
            throw new IllegalArgumentException("denominator can not be zero");
        }
        if (numerator == quotient) {
            throw new IllegalArgumentException("quotient cannot point to same value as numerator");
        }

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
        int count = 0;
        while (numeratorBits == denominatorBits) {

//            System.out.println("--------------while count: " + count + "--------------");
//            System.out.println("numerator bits: " + numeratorBits);
//            System.out.println("denominator bits: " + denominatorBits);
//            System.out.println("numerator: " + Long.toHexString(numerator[0]) + ", " + Long.toHexString(numerator[1]) + ", " + Long.toHexString(numerator[2]));
//            System.out.println("shifted_denominator: " + Long.toHexString(shiftedDenominator[0]) + ", " + Long.toHexString(shiftedDenominator[1]) + ", " + Long.toHexString(shiftedDenominator[2]));
//            System.out.println("difference: " + Long.toHexString(difference[0]) + ", " + Long.toHexString(difference[1]) + ", " + Long.toHexString(difference[2]));
//            System.out.println("quotient: " + Long.toHexString(quotient[0]) + ", " + Long.toHexString(quotient[1]) + ", " + Long.toHexString(quotient[2]));

            long borrow = subUint(numerator, shiftedDenominator, uint64Count, difference);
//            System.out.println("-----after sub uint-----");
//            System.out.println("difference: " + Long.toHexString(difference[0]) + ", " + Long.toHexString(difference[1]) + ", " + Long.toHexString(difference[2]));
//            System.out.println("borrow: " + borrow);


            if (borrow > 0) {
//                System.out.println("---------borrow>0-----------");
                if (remainingShifts == 0) {
                    break;
                }
                addUint(difference, numerator, uint64Count, difference);
//                System.out.println("-----after add uint-----");
//                System.out.println("difference: " + Long.toHexString(difference[0]) + ", " + Long.toHexString(difference[1]) + ", " + Long.toHexString(difference[2]));

                leftShiftUint192(quotient, 1, quotient);
                remainingShifts--;

//                System.out.println("-----after left shift-----");
//                System.out.println("quotient: " + Long.toHexString(quotient[0]) + ", " + Long.toHexString(quotient[1]) + ", " + Long.toHexString(quotient[2]));
//                System.out.println("remaining shift: " + remainingShifts);
//
//                System.out.println("---------borrow>0-----------");
            }

            quotient[0] |= 1;
//            System.out.println("after quotient[0] |= 1");
//            System.out.println("quotient: " + Long.toHexString(quotient[0]) + ", " + Long.toHexString(quotient[1]) + ", " + Long.toHexString(quotient[2]));


            numeratorBits = UintCore.getSignificantBitCountUint(difference, uint64Count);
            int numeratorShift = denominatorBits - numeratorBits;
//            System.out.println("numerator bits: " + numeratorBits);
//            System.out.println("numerator shift: " + numeratorShift);

            if (numeratorShift > remainingShifts) {
                numeratorShift = remainingShifts;
            }
//            System.out.println("numerator shift: " + numeratorShift);

            if (numeratorBits > 0) {
//                System.out.println("-----numerator bits>0------");

                leftShiftUint192(difference, numeratorShift, numerator);
                numeratorBits += numeratorShift;

//                System.out.println("numerator: " + Long.toHexString(numerator[0]) + ", " + Long.toHexString(numerator[1]) + ", " + Long.toHexString(numerator[2]));
//                System.out.println("numerator bits: " + numeratorBits);
//                System.out.println("-----numerator bits>0------");


            } else {
//                System.out.println("------numerator bits<=0--------");
                UintCore.setZeroUint(uint64Count, numerator);
//                System.out.println("numerator: " + Long.toHexString(numerator[0]) + ", " + Long.toHexString(numerator[1]) + ", " + Long.toHexString(numerator[2]));
//                System.out.println("------numerator bits<=0--------");
            }

            leftShiftUint192(quotient, numeratorShift, quotient);
//            System.out.println("after left shift");
//            System.out.println("quotient: " + Long.toHexString(quotient[0]) + ", " + Long.toHexString(quotient[1]) + ", " + Long.toHexString(quotient[2]));


            remainingShifts -= numeratorShift;
//            System.out.println("remaining shift: " + remainingShifts);
            count++;
        }

//        System.out.println("----out of while-----");
//        System.out.println("numerator bits: " + numeratorBits);
//        System.out.println("denominator bits: " + denominatorBits);
//        System.out.println("numerator: " + Long.toHexString(numerator[0]) + ", " + Long.toHexString(numerator[1]) + ", " + Long.toHexString(numerator[2]));
//        System.out.println("shifted_denominator: " + Long.toHexString(shiftedDenominator[0]) + ", " + Long.toHexString(shiftedDenominator[1]) + ", " + Long.toHexString(shiftedDenominator[2]));
//        System.out.println("difference: " + Long.toHexString(difference[0]) + ", " + Long.toHexString(difference[1]) + ", " + Long.toHexString(difference[2]));
//        System.out.println("quotient: " + Long.toHexString(quotient[0]) + ", " + Long.toHexString(quotient[1]) + ", " + Long.toHexString(quotient[2]));


        if (numeratorBits > 0) {
//            System.out.println("--numerator bits>0--");
            rightShiftUint192(numerator, denominatorShift, numerator);
//            System.out.println("numerator: " + Long.toHexString(numerator[0]) + ", " + Long.toHexString(numerator[1]) + ", " + Long.toHexString(numerator[2]));
        }
    }

    public static void rightShiftUint128(long[] operand, int shiftAmount, long[] result) {

        assert operand.length == 2;
        assert shiftAmount >= 0 && shiftAmount <= 3 * UINT64_BITS;
        assert operand.length == result.length;

        if (shiftAmount == 0) {
            System.arraycopy(operand, 0, result, 0, operand.length);
            return;
        }

        // 右移 超过 128 bits
        if ((shiftAmount & UINT64_BITS) > 0) { // 右移在 [64, 128) 之间
            result[0] = operand[1];
            result[1] = 0;
        } else { // [0， 64） no word shift
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
     * left shift 128 bits, length of operand is 2
     *
     * @param operand
     * @param shiftAmount
     * @param result
     */
    public static void leftShiftUint128(long[] operand, int shiftAmount, long[] result) {
        // 第二个判断条件和 seal 有差距，有奇怪的地方，seal 是 < ，但是seal 在 测试中仍然可以左移 192 bits
        assert shiftAmount >= 0 && shiftAmount <= 2 * UINT64_BITS : "shiftAmount: " + shiftAmount;
//        assert Arrays.stream(operand).allMatch(n -> n >= 0);
        assert operand.length == result.length;
        // 一定要注意这里的处理逻辑和 有返回值的重载实现不一样
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
        int bitShiftAmount = shiftAmount & (UINT64_BITS - 1);
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT64_BITS - bitShiftAmount;
            // right shift must be use unsigned right shift
            result[1] = (result[1] << bitShiftAmount) | (result[0] >>> negBitShiftAmount);
            result[0] = result[0] << bitShiftAmount;
        }
    }

    /**
     * left shift 192 bits, length of operand is 3
     *
     * @param operand
     * @param shiftAmount
     * @param result
     */
    public static void leftShiftUint192(long[] operand, int shiftAmount, long[] result) {
        // 第二个判断条件和 seal 有差距，有奇怪的地方，seal 是 < ，但是seal 在 测试中仍然可以左移 192 bits
        assert shiftAmount >= 0 && shiftAmount <= 3 * UINT64_BITS : "shiftAmount: " + shiftAmount;
//        assert Arrays.stream(operand).allMatch(n -> n >= 0);
        assert operand.length == result.length;
        // 一定要注意这里的处理逻辑和 有返回值的重载实现不一样
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
     *
     * @param a
     * @param b
     * @param result Array length is 1, store a - b
     * @return (a-b)'s borrow, 0 or 1
     */
    public static long subUint64(long a, long b, long[] result) {
        assert result.length == 1;
        result[0] = a - b;

        return Long.compareUnsigned(b, a) > 0 ? 1 : 0;
    }
//
//    public static long[] subUint64(long a, long b) {
//        long[] res = new long[2];
//        res[0] = a - b;
//
//        // if b > a, then must be borrow
//        res[1] = Long.compareUnsigned(b, a) > 0 ? 1 : 0;
//
//        return res;
//    }

//    public static long[] subUint64(long a, long b) {
//        long[] res = new long[2];
//        res[0] = a - b;
//
//
//        // 0 is the smallest, borrow = 1
//        if (a == 0 && b != 0) {
//            res[1] = 1;
//            return res;
//        }
//
//        if (a > 0 && b > 0) {
//            res[1] = a < b ? 1 : 0;
//            return res;
//        }
//        if (a < 0 && b < 0) {
//            res[1] = (a < b) ? 1 : 0;
//            return res;
//        }
//        if (a > 0 && b < 0) {
//            res[1] = 1;
//            return res;
//        }
//
//        return res;
//    }

    /**
     *
     * @param a
     * @param b
     * @param borrow
     * @param result Array length is 1, store a - b - borrow
     * @return (a - b - borrow)'s borrow, 0 or 1
     */
    public static long subUint64(long a, long b, long borrow, long[] result) {
        assert result.length == 1;
        long diff = a - b;
        result[0] = diff - borrow;
        // diff > a, must produce borrow, diff < borrow, only borrow = 1, diff = 0, will produce new borrow
        return (Long.compareUnsigned(diff, a) > 0 || Long.compareUnsigned(diff, borrow) < 0) ? 1 : 0;
    }

    public static long subUint64Generic(long a, long b, long borrow, long[] result) {
        assert result.length == 1;
        long diff = a - b;
        result[0] = diff - borrow;
        // diff > a, must produce borrow, diff < borrow, only borrow = 1, diff = 0, will produce new borrow
        return (Long.compareUnsigned(diff, a) > 0 || Long.compareUnsigned(diff, borrow) < 0) ? 1 : 0;
    }


//    public static long[] subUint64(long a, long b, long borrow) {
//        long[] res = new long[2];
//        long diff = a - b;
//        res[0] = diff - borrow;
//        // diff > a, must produce borrow, diff < borrow, only borrow = 1, diff = 0, will produce new borrow
//        long borrowOut = (Long.compareUnsigned(diff, a) > 0 || Long.compareUnsigned(diff, borrow) < 0) ? 1 : 0;
//        res[1] = borrowOut;
//
//        return res;
//    }

//
//    public static long[] subUint64(long a, long b, long borrow) {
//        long[] res = new long[2];
//        long diff = a - b;
//        res[0] = diff - borrow;
//        // must consider that inputs will be negative, and diff will be negative
//
//        // 0 is the smallest, borrow = 1
//        if (a == 0 && b != 0) {
//            res[1] = 1;
//            return res;
//        }
//        if (diff == 0) {
//            res[1] = borrow;
//            return res;
//        }
//
//        if (a > 0 && b > 0) {
//            res[1] = a < b ? 1 : 0;
//            return res;
//        }
//        if (a < 0 && b < 0) {
//            res[1] = (a < b) ? 1 : 0;
//            return res;
//        }
//        if (a > 0 && b < 0) {
//            res[1] = 1;
//            return res;
//        }
//        return res;
//    }

    /**
     * @param as
     * @param uint64Count
     * @param b
     * @param result
     * @return long[] - long
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
     *
     * @param a a 64-bit value
     * @param b a 64-bit value
     * @param result an array of length 1， which store lower 64-bit part of (a + b)
     * @return (a + b)'s carry
     */
    public static long addUint64(long a, long b, long[] result) {
        assert result.length == 1;
        result[0] = a + b;
        return Long.compareUnsigned(result[0], a) < 0 ? 1: 0;
    }

    /**
     *
     * @param a a 64-bit value
     * @param b a 64-bit value
     * @param carry given carry, 0 or 1
     * @param result an array of length 1 representing a 64-bit value，which store lower 64-bit part of (a + b + carry)
     * @return (a + b + carry)'s carry
     */
    public static long addUint64(long a, long b, long carry, long[] result) {
        assert result.length == 1;
        long sum = a + b;
        result[0] = sum + carry;
        return (Long.compareUnsigned(sum, a) < 0 || (sum == - 1 && carry == 1) ) ? 1: 0;
    }

    public static long addUint64Generic(long a, long b, long carry, long[] result) {
        assert result.length == 1;
        long sum = a + b;
        result[0] = sum + carry;
        return (Long.compareUnsigned(sum, a) < 0 || (sum == - 1 && carry == 1) ) ? 1: 0;
    }


    /**
     *
     * @param as a base-2^64 value
     * @param uint64Count number of uint64 used in as
     * @param b a 64-bit value
     * @param result a base-2^64 value, which store the (as + b)
     * @return (as + b)'s carry
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
     *
     * @param as a base-2^64 value
     * @param bs a base-2^64 value
     * @param uint64Count number of uint64 used in as and bs
     * @param result a base-2^64 value, which store the (as + bs)
     * @return (as + bs)'s carry
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
     *
     * @param as a base-2^64 value
     * @param asUint64Count number of uint64 used in as
     * @param bs a base-2^64 value
     * @param bsUint64Count number of uint64 used in bs
     * @param carry given carry, 0 or 1
     * @param uint64Count number of uint64 used in result
     * @param result a base-2^64 value, which store the (as + bs)
     * @return (as + bs)'s carry
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
     *
     * @param operand
     * @param exponent
     * @return operand^exponent
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
        long product = 0;
        long intermediate = 1;

        // Initially: power = operand and intermediate = 1, product irrelevant.
        while (true) {
            if ((exponent & 1) > 0) {
                product = power * intermediate;
                // swap
                intermediate = product;
//                long tmp = product;
//                product = intermediate;
//                intermediate = tmp;
            }
            exponent >>>= 1;
            if (exponent == 0) break;

            product = power * power;
            //swap
            power = product;
//            long tmp = product;
//            product = power;
//            power = tmp;
        }
        return intermediate;
    }


}
