package edu.alibaba.mpc4j.crypto.fhe.zq;


import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.math.BigInteger;
import java.util.Arrays;

/**
 *  Using long present a 63-bit uint, then under base-2^63 implement Arithmetic
 *
 * @author Qixian Zhou
 * @date 2023/7/31
 */
public class LongArithmetic {

    // we use 63 bits to represent
    private static final int UINT63_BITS = 63;
    // 129 bits represent 2^128
    public static final long[] TWO_POWER_128 = new long[] {0, 0, 4};


    /**
     * a 189-bit numerator divide a 63-bit denominator, return quotient and remainder
     * @param numerator numerator
     * @param denominator denominator
     * @return an array result , length is 4, result[0..2] is quotient, result[3] is the remainder
     */
    public static long[] divideUint189(long[] numerator, long denominator) {

        assert numerator.length == 3;
        assert denominator > 0;
        assert Arrays.stream(numerator).allMatch(n -> n >= 0);

        int uint63Count = 3;
        // last index store the remainder
        long[] quotient = new long[3];

        int numeratorBits = getSignificantBitCountUint63(numerator, uint63Count);
        int denominatorBits = getUint63ValueBitCount(denominator);
        // 如果分子bit 长度 < 分母，商肯定为0,
        if (numeratorBits < denominatorBits) {
            return quotient;
        }

        uint63Count = divideRoundUp(numeratorBits, UINT63_BITS);
        // only one 63 bits, directly compute
        if (uint63Count == 1) {
            quotient[0] = numerator[0] / denominator;
            // change numerator, this change will pass out of this function
            numerator[0] = numerator[0] - quotient[0] * denominator;
            // copy remainder


            return quotient;
        }
        // shift denominator up to numerator same bits
        long[] shiftedDenominator = new long[uint63Count];
        shiftedDenominator[0] = denominator;
        long[] difference = new long[uint63Count];

        int denominatorShift = numeratorBits - denominatorBits;
        shiftedDenominator = leftShiftUint189(shiftedDenominator, denominatorShift);
        denominatorBits += denominatorShift;

        int remainingShifts = denominatorShift;
        long borrow;
        int whileCount = 0;
        while (numeratorBits == denominatorBits) {
            // difference will be changed
            borrow = subUint(numerator, shiftedDenominator, uint63Count, difference);

            if (borrow > 0) {
                if (remainingShifts == 0) break;
                addUint(difference, numerator, uint63Count, difference);
                quotient = leftShiftUint189(quotient, 1);
                remainingShifts--;
            }

            quotient[0] |= 1;
            numeratorBits = getSignificantBitCountUint63(difference, uint63Count);
            int numeratorShift = denominatorBits - numeratorBits;
            if (numeratorShift > remainingShifts) {
                numeratorShift = remainingShifts;
            }
            if (numeratorBits > 0) {
                numerator = leftShiftUint189(difference, numeratorShift);
                numeratorBits += numeratorShift;
            }else {
                setZeroUint(uint63Count, numerator);
            }

            quotient = leftShiftUint189(quotient, numeratorShift);

            remainingShifts -= numeratorShift;

            whileCount++;
        }
        if (numeratorBits > 0) {
            numerator = rightShiftUint189(numerator, denominatorShift);
        }
        // consider the remainder, because the denominator is up to 63bits
        // so the remainder is up to 63 bits
        long[] result = new long[4];
        System.arraycopy(quotient, 0, result, 0, quotient.length);
        result[3] = numerator[0];
        return result;
    }

    /**
     *
     * @param numerator
     * @param denominator
     * @param quotient as result
     * @return remainder
     */
    public static long divideUint189(long[] numerator, long denominator, long[] quotient) {

        assert numerator.length == 3;
        assert quotient.length == 3;
        assert denominator > 0;
        assert Arrays.stream(numerator).allMatch(n -> n >= 0);

        int uint63Count = 3;
        quotient[0] = 0;
        quotient[1] = 0;
        quotient[2] = 0;

        int numeratorBits = getSignificantBitCountUint63(numerator, uint63Count);
        int denominatorBits = getUint63ValueBitCount(denominator);
        // 如果分子bit 长度 < 分母，商肯定为0,
        if (numeratorBits < denominatorBits) {
            // return remainder
            return numerator[0];
        }

        uint63Count = divideRoundUp(numeratorBits, UINT63_BITS);
        // only one 63 bits, directly compute
        if (uint63Count == 1) {
            quotient[0] = numerator[0] / denominator;
            // change numerator, this change will pass out of this function
            numerator[0] = numerator[0] - quotient[0] * denominator;
            // copy remainder
            return numerator[0];
        }
        // shift denominator up to numerator same bits
        long[] shiftedDenominator = new long[uint63Count];
        shiftedDenominator[0] = denominator;
        long[] difference = new long[uint63Count];

        int denominatorShift = numeratorBits - denominatorBits;
        shiftedDenominator = leftShiftUint189(shiftedDenominator, denominatorShift);
        denominatorBits += denominatorShift;

        int remainingShifts = denominatorShift;
        long borrow;
        int whileCount = 0;
        while (numeratorBits == denominatorBits) {

            // difference will be changed
            borrow = subUint(numerator, shiftedDenominator, uint63Count, difference);
//            System.out.printf("while count: %d, borrow: %d %n", whileCount, borrow);

            if (borrow > 0) {
                if (remainingShifts == 0) break;
                addUint(difference, numerator, uint63Count, difference);
//                System.out.printf("borrow > 0, while count: %d, before left shift, quotient: %d, %d, %d %n", whileCount, quotient[0], quotient[1], quotient[2]);
                leftShiftUint189(quotient, 1, quotient);
//                System.out.printf("borrow > 0, while count: %d, after left shift, quotient: %d, %d, %d %n", whileCount, quotient[0], quotient[1], quotient[2]);
                remainingShifts--;
            }
//            System.out.printf("while count: %d, before quotient[0] |= 1, quotient: %d, %d, %d %n", whileCount, quotient[0], quotient[1], quotient[2]);
            quotient[0] |= 1;
//            System.out.printf("while count: %d, after quotient[0] |= 1, quotient: %d, %d, %d %n", whileCount, quotient[0], quotient[1], quotient[2]);

            numeratorBits = getSignificantBitCountUint63(difference, uint63Count);
            int numeratorShift = denominatorBits - numeratorBits;
            if (numeratorShift > remainingShifts) {
                numeratorShift = remainingShifts;
            }
            if (numeratorBits > 0) {
//                System.out.printf("while count: %d, before inplace left(%d), numerator: %d, %d, %d%n",whileCount,numeratorShift, numerator[0], numerator[1], numerator[2] );
//                System.out.printf("difference: %d, %d, %d%n", difference[0], difference[1], difference[2]);
                leftShiftUint189(difference, numeratorShift, numerator);
//                numerator = leftShiftUint189(difference, numeratorShift);
//                System.out.printf("while count: %d, after inplace left(%d), numerator: %d, %d, %d%n",whileCount, numeratorShift, numerator[0], numerator[1], numerator[2] );

//                numerator = leftShiftUint189(difference, numeratorShift);
                numeratorBits += numeratorShift;
            }else {
                setZeroUint(uint63Count, numerator);
            }
//            System.out.printf("while count: %d, before left shift, quotient: %d, %d, %d %n", whileCount, quotient[0], quotient[1], quotient[2]);
            leftShiftUint189(quotient, numeratorShift, quotient);
//            System.out.printf("while count: %d, after shift, quotient: %d, %d, %d %n", whileCount, quotient[0], quotient[1], quotient[2]);

            remainingShifts -= numeratorShift;

            whileCount++;
        }
        if (numeratorBits > 0) {
            numerator = rightShiftUint189(numerator, denominatorShift);
        }

        return numerator[0];
    }


    public static void setZeroUint(int uint63Count, long[] values) {
        for (int i = 0; i < uint63Count; i++) {
            values[i] = 0;
        }
    }

    public static void leftShiftUint189(long[] operand, int shiftAmount, long[] result) {
        // 左移 结果 不能超过 189
        assert shiftAmount >= 0;
        assert Arrays.stream(operand).allMatch(n -> n >= 0);
        assert operand.length == result.length;
        // 一定要注意这里的处理逻辑和 有返回值的重载实现不一样
        if (shiftAmount == 0) {
            System.arraycopy(operand, 0, result, 0, operand.length);
            return;
        }

        if (shiftAmount >= UINT63_BITS << 1) {
            result[2] = operand[0];
        } else if (shiftAmount >= UINT63_BITS) {
            result[2] = operand[1];
            result[1] = operand[0];
        } else {
            result[2] = operand[2];
            result[1] = operand[1];
            result[0] = operand[0];
        }
        int bitShiftAmount = shiftAmount % UINT63_BITS;
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT63_BITS - bitShiftAmount;

            result[2] = ((result[2] << bitShiftAmount) | (result[1] >> negBitShiftAmount)) & Long.MAX_VALUE;
            result[1] = ((result[1] << bitShiftAmount) | (result[0] >> negBitShiftAmount)) & Long.MAX_VALUE;
            result[0] = (result[0] << bitShiftAmount) & Long.MAX_VALUE; // 这里是可能溢出的， 这里溢出了，结果有可能出错
        }

    }

    /**
     * left shift a value under base-2^63 , result satisfy:
     *
     *  (operand[0] + 2^63 * operand[1] + 2^126 * operand[2]) >> shiftAmount
     *
     *
     * @param operand long array with length = 3, split a 189-bit uint into three long value
     * @param shiftAmount shift bits
     * @return long[] after shiftAmount
     */
    public static long[] leftShiftUint189(long[] operand, int shiftAmount) {
        // 左移 结果 不能超过 189
        assert shiftAmount >= 0 && shiftAmount <= 3 * UINT63_BITS;
//        assert getSignificantBitCountUint63(operand, operand.length) + shiftAmount <= 189: "the shift bit count is: " + (getSignificantBitCountUint63(operand, operand.length) + shiftAmount) + " > 189";


        assert Arrays.stream(operand).allMatch(n -> n >= 0);
        // must be deep copy
        if (shiftAmount == 0) {
            return Arrays.copyOf(operand, operand.length);
        }

        assert operand.length == 3;
        long[] result = new long[3];

        if (shiftAmount >= UINT63_BITS << 1 ) {
            result[2] = operand[0];
        } else if (shiftAmount >= UINT63_BITS) {
            result[2] = operand[1];
            result[1] = operand[0];
        } else {
            result[2] = operand[2];
            result[1] = operand[1];
            result[0] = operand[0];
        }
        int bitShiftAmount = shiftAmount % UINT63_BITS;
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT63_BITS - bitShiftAmount;

//            result[2] = (result[2] << bitShiftAmount) | (result[1] >> negBitShiftAmount);
//            result[1] = (result[1] << bitShiftAmount) | (result[0] >> negBitShiftAmount);
//            result[0] = result[0] << bitShiftAmount; // 这里是可能溢出的， 这里溢出了，结果有可能出错
            // & Long.MAX_VALUE = 2^63 - 1 to avoid overflow, essentially we implement Arithmetic in Z_{2^{63} - 1}
            result[2] = ((result[2] << bitShiftAmount) | (result[1] >> negBitShiftAmount)) & Long.MAX_VALUE;
            result[1] = ((result[1] << bitShiftAmount) | (result[0] >> negBitShiftAmount)) & Long.MAX_VALUE;
            result[0] = (result[0] << bitShiftAmount) & Long.MAX_VALUE; // 这里是可能溢出的， 这里溢出了，结果有可能出错

//            // 修正负数, 只有产生负数的时候，才会导致 结果出错，即 得到的 long[] 无法正确还原为一个 192 bits 的数
//            // 其余情况不会出错，所以这里修正负数！
//            if (result[1] < 0) {
////                long diff = result[1] - Long.MIN_VALUE;
////                result[1] = diff;
//                result[1] &= Long.MAX_VALUE;
//                result[2] |= 1;
//            }
//            if (result[0] < 0) {
////                long diff = result[0] - Long.MIN_VALUE;
////                result[0] = diff;
//                result[0] &= Long.MAX_VALUE;
//                result[1] |= 1;
//            }
        }

        return result;
    }

    /**
     *
     * @param operand
     * @param shiftAmount
     * @return
     */
    public static long[] rightShiftUint189(long[] operand, int shiftAmount) {

        assert operand.length == 3;
        assert shiftAmount >= 0 && shiftAmount < 3 * UINT63_BITS;
        assert Arrays.stream(operand).allMatch(n -> n >= 0);
        // must be deep copy
        if (shiftAmount == 0) {
            return Arrays.copyOf(operand, operand.length);
        }

        long[] result = new long[3];
        // 右移 超过 126 bits
        if (shiftAmount >= UINT63_BITS << 1 ) {
            result[0] = operand[2];
        } else if (shiftAmount >= UINT63_BITS) { // 右移在 [63, 126) 之间
            result[0] = operand[1];
            result[1] = operand[2];
        } else { // [0， 63） no word shift
            result[2] = operand[2];
            result[1] = operand[1];
            result[0] = operand[0];
        }
        int bitShiftAmount = shiftAmount % UINT63_BITS;
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT63_BITS - bitShiftAmount;
            result[0] = ((result[0] >> bitShiftAmount) | (result[1] << negBitShiftAmount)) & Long.MAX_VALUE;
            result[1] = ((result[1] >> bitShiftAmount) | (result[2] << negBitShiftAmount)) & Long.MAX_VALUE;
            result[2] = (result[2] >> bitShiftAmount) & Long.MAX_VALUE; // 这里是可能溢出的， 这里溢出了，结果有可能出错
        }
        return result;
    }

    public static void rightShiftUint189(long[] operand, int shiftAmount, long[] result) {

        assert operand.length == 3;
        assert shiftAmount >= 0 && shiftAmount < 3 * UINT63_BITS;
        assert Arrays.stream(operand).allMatch(n -> n >= 0);
        assert operand.length == result.length;

        if (shiftAmount == 0) {
            System.arraycopy(operand, 0, result, 0, operand.length);
            return;
        }

        // 右移 超过 126 bits
        if (shiftAmount >= UINT63_BITS << 1 ) {
            result[0] = operand[2];
        } else if (shiftAmount >= UINT63_BITS) { // 右移在 [63, 126) 之间
            result[0] = operand[1];
            result[1] = operand[2];
        } else { // [0， 63） no word shift
            result[2] = operand[2];
            result[1] = operand[1];
            result[0] = operand[0];
        }
        int bitShiftAmount = shiftAmount % UINT63_BITS;
        if (bitShiftAmount > 0) {
            int negBitShiftAmount = UINT63_BITS - bitShiftAmount;
            result[0] = ((result[0] >> bitShiftAmount) | (result[1] << negBitShiftAmount)) & Long.MAX_VALUE;
            result[1] = ((result[1] >> bitShiftAmount) | (result[2] << negBitShiftAmount)) & Long.MAX_VALUE;
            result[2] = (result[2] >> bitShiftAmount) & Long.MAX_VALUE; // 这里是可能溢出的， 这里溢出了，结果有可能出错
        }
    }

    /**
     * the real bits in values, for example: [0, 0, 1]  63 + 63 + 1 = 127
     * [1, 0, 0, ]  1 + 0 + 0 = 1 bits
     * @param values long values
     * @param uint63Count  how many 63-bits
     * @return the real bits in the range values[..uint63Count]
     */
    public static int getSignificantBitCountUint63(long[] values, int uint63Count) {

        assert uint63Count <= values.length;
        assert Arrays.stream(values).allMatch(n -> n >= 0);

        int index = uint63Count - 1;
        for (; values[index] == 0 && uint63Count > 1; uint63Count--) {
            index--;
        }

        return (uint63Count - 1) * UINT63_BITS + getUint63ValueBitCount(values[index]);
    }


    public static int getUint63ValueBitCount(long value) {
        return 64 - Long.numberOfLeadingZeros(value);
    }


    public static BigInteger convertLongArrayToBigInteger(long[] values) {

        assert getSignificantBitCountUint63(values, values.length) <= 189;

        BigInteger res = BigInteger.valueOf(0);
        BigInteger pow = BigInteger.ONE;
        BigInteger base = BigInteger.ZERO.setBit(63);
        for (int i = 0; i < values.length; i++) {
            res = res.add(BigInteger.valueOf(values[i]).multiply(pow));
            pow = pow.multiply(base);
        }
        return res;
    }
    // convert base-2^63 to digit, just like base-10, for example: 123 -> 3 2 1
    public static long[] convertBigIntegerToLongArray(BigInteger value) {
        assert value.bitLength() <= 189;
        BigInteger base = BigInteger.valueOf(2).pow(63);
        long[] result = new long[3];
        result[0] = value.mod(base).longValue();
        value = value.divide(base);
        result[1] = value.mod(base).longValue();
        value = value.divide(base);
        result[2] = value.mod(base).longValue();
        return result;
    }

    /**
     *
     * @param value value
     * @param divisor divisor
     * @return ceil(ceil/divisior)
     */
    public static int divideRoundUp(int value, int divisor) {

        assert value >= 0;
        assert divisor > 0;

        return (value + divisor - 1) / divisor;
    }


    public static long[] subUint(long[] operand1, long[] operand2, int uint63Count) {
        assert operand1.length == operand2.length;
        assert uint63Count > 0;
        long[] result = new long[uint63Count + 1];

        long[] tmp = subUint63(operand1[0], operand2[0]);
        result[0] = tmp[0];
        long borrow = tmp[1];
        int i = 1;
        while (--uint63Count > 0) {
            tmp = subUint63(operand1[i], operand2[i], borrow);
            result[i] = tmp[0];
            borrow = tmp[1];
            i++;
        }
        //
        result[result.length - 1] = borrow;
        return result;
    }

    public static long subUint(long[] operand1, long[] operand2, int uint63Count, long[] result) {
        assert operand1.length == operand2.length;
        assert uint63Count > 0;
        assert result.length == uint63Count;

        long[] tmp = subUint63(operand1[0], operand2[0]);
        result[0] = tmp[0];
        long borrow = tmp[1];
        int i = 1;
        while (--uint63Count > 0) {
            tmp = subUint63(operand1[i], operand2[i], borrow);
            result[i] = tmp[0];
            borrow = tmp[1];
            i++;
        }
        return borrow;
    }



    public static long[] subUint(long[] operand1, int operand1Uint63Count, long[] operand2, int operand2Uint63Count,long borrow, int resultUint63Count) {
        assert resultUint63Count > 0;
        long[] result = new long[resultUint63Count + 1];
        for (int i = 0; i < resultUint63Count; i++) {
            long[] tmp = subUint63(
                    i < operand1Uint63Count ? operand1[i] : 0,
                    i < operand2Uint63Count ? operand2[i] : 0,
                    borrow
            );
            borrow = tmp[1];
            result[i] = tmp[0];
        }
        return result;
    }


    /**
     * result[0] is the a - b, result[1] is the borrow
     * @param a a
     * @param b b
     * @return  a - b mod 2^{63} - 1, and whether produce borrow
     */
    public static long[] subUint63(long a, long b) {
        long[] result = new long[2];

        result[0] = (a - b) & Long.MAX_VALUE;
        result[1] = b > a ? 1 : 0;
        return result;
    }

    /**
     *  a - (b + borrow)
     * @param a a
     * @param b b
     * @param borrow 1 or 0
     * @return
     */
    public static long[] subUint63(long a, long b, long borrow) {

        assert borrow == 1 || borrow == 0;

        long[] result = new long[2];
        long diff = (a - b);
        // 只要有一个成立，就说明有进位
        result[1] = ((diff < 0) || (diff < borrow)) ? 1: 0;
        //
        result[0] = (diff - borrow ) & Long.MAX_VALUE;
        return result;
    }




    public static long[] addUint63(long a, long b) {
        long[] result = new long[2];

        result[0] = (a + b) & Long.MAX_VALUE;
        result[1] = result[0] < a ? 1 : 0;
        return result;
    }

    public static long[] addUint63(long a, long b, long carry) {
        assert carry == 0 || carry == 1;
        long[] result = new long[2];

        a = (a + b) & Long.MAX_VALUE;
        result[0] = (a + carry) & Long.MAX_VALUE;
        // operand1 < operand2 说明 溢出了，那么肯定有进位， 如果这个不满足，再看第二个条件
        // 能小于的，那么 carry 只能为 1, 对 operand1 取反，如果小于 carry , 说明取反等于 0， 那取反前就是 u64::max, u64::max + 1 自然也存在进位
        // if a = 0, ~a will be a negative number , ~a < carry will be true, but this is incorrect
        // so here need ~a & Long.MAX_VALUE
        result[1] = ( a < b || (~a & Long.MAX_VALUE) < carry ) ? 1 : 0;
        return result;
    }

    public static long[] addUint(long[] operand1, long[] operand2, int uint63Count) {

        assert uint63Count > 0;
        assert operand1.length == operand2.length;
        // last index store carry
        long[] result = new long[uint63Count + 1];
        long[] tmp = addUint63(operand1[0], operand2[0]);
        result[0] = tmp[0];
        long carry = tmp[1];
        int i = 1;
        while (--uint63Count > 0) {
            tmp = addUint63(operand1[i], operand2[i], carry);
            result[i] = tmp[0];
            carry = tmp[1];
            i++;
        }
        result[result.length - 1] = carry;
        return result;
    }

    public static long addUint(long[] operand1, long[] operand2, int uint63Count, long[] result) {

        assert uint63Count > 0;
        assert operand1.length == operand2.length;
        assert result.length == uint63Count;
        long[] tmp = addUint63(operand1[0], operand2[0]);
        result[0] = tmp[0];
        long carry = tmp[1];
        int i = 1;
        while (--uint63Count > 0) {
            tmp = addUint63(operand1[i], operand2[i], carry);
            result[i] = tmp[0];
            carry = tmp[1];
            i++;
        }

        return carry;
    }



    public static long[] addUint(long[] operand1, int operand1Uint63Count, long[] operand2, int operand2Uint63Count, long carry, int uint63Count) {

        assert operand1Uint63Count > 0 && operand2Uint63Count > 0;
        assert uint63Count > 0;

        long[] result = new long[uint63Count + 1];
        long[] tmp;

        for (int i = 0; i < uint63Count; i++) {
            tmp = addUint63(
                    i < operand1Uint63Count ? operand1[i] : 0,
                    i < operand2Uint63Count ? operand2[i] : 0,
                    carry
            );
            result[i] = tmp[0];
            carry = tmp[1];
        }
        result[result.length - 1] = carry;
        return result;
    }





}