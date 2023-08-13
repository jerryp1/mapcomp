package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/8/4
 */
public class UintCore {


    /**
     * If the value is a power of two, return the power; otherwise, return -1.
     * @param value
     * @return value = 2^n, return n.
     */
    public static int getPowerOfTwo(long value) {
        if (value == 0 || (value & (value - 1)) != 0) {
            return -1;
        }
        return Common.getMsbIndex(value);
    }


    /**
     * note that values is a base-2^64 number
     * @param values
     * @param uint64Count
     * @return values == 0
     */
    public static boolean isZeroUint(long[] values, int uint64Count) {

        assert uint64Count > 0;
        return Arrays.stream(values, 0, uint64Count).allMatch(n -> n == 0);
    }


    /**
     * note that values is a base-2^64 number
     * @param values
     * @param uint64Count
     * @param scalar
     * @return values == scalar
     */
    public static boolean isEqualUint(long[] values, int uint64Count, long scalar) {

        assert uint64Count > 0;
        if (values[0] != scalar) {
            return false;
        }
        // if values[0] == scalar, then only if values[1..] all equal 0, then values = scalar, return true
        return Arrays.stream(values, 1, uint64Count).allMatch(n -> n == 0);
    }



    /**
     *
     * @param operand1
     * @param operand1Uint64Count
     * @param operand2
     * @param operand2Uint64Count
     * @return operand1 == operand2
     */
    public static boolean isEqualUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count) {

        return compareUint(operand1, operand1Uint64Count,  operand2, operand2Uint64Count) == 0;
    }

    /**
     *
     * @param operand1
     * @param operand2
     * @param uint64Count
     * @return operand1 == operand2
     */
    public static boolean isEqualUint(long[] operand1, long[] operand2, int uint64Count) {

        return compareUint(operand1, operand2, uint64Count) == 0;
    }


    /**
     *
     * @param operand1
     * @param operand1Uint64Count
     * @param operand2
     * @param operand2Uint64Count
     * @return operand1 > operand2
     */
    public static boolean isGreaterThanUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count) {

        return compareUint(operand1, operand1Uint64Count,  operand2, operand2Uint64Count) > 0;
    }

    /**
     *
     * @param operand1
     * @param operand2
     * @param uint64Count
     * @return operand1 > operand2
     */
    public static boolean isGreaterThanUint(long[] operand1, long[] operand2, int uint64Count) {

        return compareUint(operand1, operand2, uint64Count) > 0;
    }

    /**
     *
     * @param operand1
     * @param operand2
     * @param uint64Count
     * @return operand1 >= operand2
     */
    public static boolean isGreaterThanOrEqualUint(long[] operand1, long[] operand2, int uint64Count) {

        return compareUint(operand1, operand2, uint64Count) >= 0;
    }

    /**
     *
     * @param operand1
     * @param operand1Uint64Count
     * @param operand2
     * @param operand2Uint64Count
     * @return operand1 >= operand2
     */
    public static boolean isGreaterThanOrEqualUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count) {

        return compareUint(operand1, operand1Uint64Count,  operand2, operand2Uint64Count) >= 0;
    }

    /**
     *
     * @param operand1
     * @param operand2
     * @param uint64Count
     * @return operand1 < operand2
     */
    public static boolean isLessThanUint(long[] operand1, long[] operand2, int uint64Count) {
        return compareUint(operand1, operand2, uint64Count) < 0;
    }

    /**
     *
     * @param operand1
     * @param operand1Uint64Count
     * @param operand2
     * @param operand2Uint64Count
     * @return operand1 < operand2
     */
    public static boolean isLessThanUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count) {

        return compareUint(operand1, operand1Uint64Count,  operand2, operand2Uint64Count) < 0;
    }

    /**
     *
     * @param operand1
     * @param operand2
     * @param uint64Count
     * @return operand1 <= operand2
     */
    public static boolean isLessThanOrEqualUint(long[] operand1, long[] operand2, int uint64Count) {
        return compareUint(operand1, operand2, uint64Count) <= 0;
    }

    /**
     *
     * @param operand1
     * @param operand1Uint64Count
     * @param operand2
     * @param operand2Uint64Count
     * @return operand1 <= operand2
     */
    public static boolean isLessThanOrEqualUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count) {
        return compareUint(operand1, operand1Uint64Count,  operand2, operand2Uint64Count) <= 0;
    }





    /**
     * note that operand1 and operand2 is a base-2^64 number
     * @param operand1
     * @param operand2
     * @param uint64Count
     * @return if operand1 > operand2 , return 1, else if operand1 < operand2 return -1, otherwise return 0
     */
    public static int compareUint(long[] operand1, long[] operand2, int uint64Count) {

        assert uint64Count > 0;

        int result = 0;
        int index = uint64Count - 1;
        // once result = 1, break loop
        for (; result == 0 && uint64Count-- > 0; index--) {
//            result = (operand1[index] > operand2[index] ? 1 : 0) - ((operand1[index] < operand2[index] ? 1 : 0));
              result = compareUint64(operand1[index], operand2[index]);
        }
        return result;
    }

    /**
     * note that we treat long as uint64, if a is neg, b is pos, the a > b
     * @param a
     * @param b
     * @return if a > b then return 1, else if  a < b , return -1, otherwise return 0
     */
    public static int compareUint64(long a, long b) {

        if (a == b) {
            return 0;
        }
        // 0 < any  and a != b
        if (a == 0) {
            return -1;
        }
        // and a != b
        if (b == 0) {
            return 1;
        }
        if (( a > 0 && b > 0) || ( a < 0 && b < 0) ) {
            // a = -1 b = -2 , a > b , return 1
            // a == b do not occur here
            return a > b ? 1 : -1;
        }
        // a > 0 , b must be < 0, so a < b
        if (a > 0) {
            return -1;
        }
        // when reach here,only one possible: a < 0 && b > 0
        return 1;
    }

    /**
     * note that operand1 and operand2 is a base-2^64 number
     * @param operand1
     * @param operand1Uint64Count
     * @param operand2
     * @param operand2Uint64Count
     * @return if operand1 > operand2 , return 1, else if operand1 < operand2 return -1, otherwise return 0
     */
    public static int compareUint(long[] operand1, int operand1Uint64Count,  long[] operand2, int operand2Uint64Count) {

        assert operand1Uint64Count > 0 && operand2Uint64Count > 0;

        int result = 0;
        int operand1Index = operand1Uint64Count - 1;
        int operand2Index = operand2Uint64Count - 1;
        int minUint64Count = Math.min(operand1Uint64Count, operand2Uint64Count);
        // 0 or > 0
        operand1Uint64Count -= minUint64Count;
        for (; (result == 0) && operand1Uint64Count-- > 0; operand1Index--) {
//            result = operand1[operand1Index] > 0 ? 1: 0;
            // treat long as uint64, so once != 0, operand1 > operand2
            result = operand1[operand1Index] != 0 ? 1: 0;
        }
        operand2Uint64Count -= minUint64Count;
        for (; (result == 0) && operand2Uint64Count-- > 0; operand2Index--) {
//            result = -(operand2[operand2Index] > 0 ? 1: 0);
            result = -(operand1[operand1Index] != 0 ? 1: 0);
        }

        for(; result == 0 && minUint64Count-- > 0; operand1Index--, operand2Index--) {
//            result = (operand1[operand1Index] > operand2[operand2Index] ? 1 : 0) - ((operand1[operand1Index] < operand2[operand2Index] ? 1 : 0));
              result = compareUint64(operand1[operand1Index], operand2[operand2Index]);
        }
        return result;
    }



    /**
     *  such as : ([1, 0], 2) , will return 1, because the significant Uint64Count is just 1
     * @param values
     * @param uint64Count
     * @return real uint64Count of given values
     */
    public static int getSignificantUint64CountUint(long[] values, int uint64Count) {
        assert uint64Count > 0;
        assert values.length >= uint64Count;

        int index = uint64Count - 1;
        // ([1, 0], 2) ---->  1
        //  只要不满足中间条件，会直接退出 for 循环
        // ensure uint64Count > 0 is the first condition
        for (; uint64Count > 0 && values[index] == 0;  uint64Count--){
            index--;
        }
        return uint64Count;
    }



    /**
     * the real bits in values, for example: [0, 0, 1]  63 + 63 + 1 = 127
     * [1, 0, 0, ]  1 + 0 + 0 = 1 bits
     * @param values long values
     * @param uint64Count  how many 63-bits
     * @return the real bits in the range values[..uint63Count]
     */
    public static int getSignificantBitCountUint(long[] values, int uint64Count) {

        assert uint64Count <= values.length;

        int index = uint64Count - 1;
        for (; values[index] == 0 && uint64Count > 1; uint64Count--) {
            index--;
        }

        return (uint64Count - 1) * 64 + getSignificantBitCount(values[index]);
    }


    public static int getSignificantBitCount(long value) {
        return 64 - Long.numberOfLeadingZeros(value);
    }


    public static int divideRoundUp(int value, int divisor) {
        assert value >= 0;
        assert divisor > 0;

        return (value + divisor - 1) / divisor;
    }

    public static void setZeroUint(int uint64Count, long[] values) {
        for (int i = 0; i < uint64Count; i++) {
            values[i] = 0;
        }
    }

    /**
     * set result[0] = value, result[i>0] = 0
     * @param value
     * @param uint64Count
     * @param result
     */
    public static void setUint(long value, int uint64Count, long[] result) {
        assert uint64Count > 0;
        assert result.length >= uint64Count;

        result[0] = value;
        for (int i = 1; i < uint64Count; i++) {
            result[i] = 0;
        }
    }

    /**
     * result[0..uint64Count] = value[0..uint64Count]
     * @param values
     * @param uint64Count
     * @param result
     */
    public static void setUint(long[] values, int uint64Count, long[] result) {
        assert uint64Count >= 0;

        if (result == values || uint64Count == 0) {
            return;
        }
        System.arraycopy(values, 0, result, 0, uint64Count);
    }

    /**
     *
     * @param values
     * @param valueUint64Count
     * @param resultUint64Count
     * @param result
     */
    public static void setUint(long[] values, int valueUint64Count, int resultUint64Count, long[] result) {

        assert values.length >= valueUint64Count;
        assert result.length >= resultUint64Count;

        if (values == result || valueUint64Count == 0) {
            Arrays.fill(result, valueUint64Count, resultUint64Count, 0);
        }else {
            int minUint64Count = Math.min(valueUint64Count, resultUint64Count);
            System.arraycopy(values, 0, result, 0, minUint64Count);
            Arrays.fill(result, minUint64Count, resultUint64Count, 0);
        }
    }

    /**
     * set the specific bitIndex of long[] to 1, long[] has  64 * uint64Count bits
     * @param values
     * @param uint64Count
     * @param bitIndex
     */
    public static void setBitUint(long[] values, int uint64Count, int bitIndex) {

        assert uint64Count > 0;
        // [0, 64 * uint64Count)
        assert bitIndex >= 0;
        assert bitIndex < Constants.UINT64_BITS * uint64Count;

        int uint64Index = bitIndex / Constants.UINT64_BITS;
        int subBitIndex = bitIndex % Constants.UINT64_BITS;
        // set
        values[uint64Index] |= (1L << subBitIndex);
    }

    /**
     *
     * @param values
     * @param uint64Count
     * @param bitIndex
     * @return  whether the value at specific bitIndex of given values is 1
     */
    public static boolean isBitSetUint(long[] values, int uint64Count, int bitIndex) {

        assert uint64Count > 0;
        // [0, 64 * uint64Count)
        assert bitIndex >= 0;
        assert bitIndex < Constants.UINT64_BITS * uint64Count;

        int uint64Index = bitIndex / Constants.UINT64_BITS;
        int subBitIndex = bitIndex % Constants.UINT64_BITS;
        // right shift and judge
        return ((values[uint64Index] >>> subBitIndex) & 1) != 0;
    }

}
