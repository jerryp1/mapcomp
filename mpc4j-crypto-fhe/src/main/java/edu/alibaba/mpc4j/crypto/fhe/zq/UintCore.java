package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/8/4
 */
public class UintCore {

    /**
     * @param value       a base-2^64 value
     * @param uint64Count number of uint64 used in value
     * @return
     */
    public static String uintToDecimalString(long[] value, int uint64Count) {

        assert !(uint64Count > 0 && value == null);

        if (uint64Count == 0) {
            return "0";
        }

        long[] remainder = new long[uint64Count];
        long[] quotient = new long[uint64Count];
        long[] base = new long[uint64Count];
        int remainderIndex = 0;
        int quotientIndex = 0;
        int baseIndex = 0;
        // base = [10, 10, ..., 10]
        setUint(10, uint64Count, base);
        // remainder = value
        setUint(value, uint64Count, remainder);

        StringBuilder output = new StringBuilder();
        // 例如把 1234 这个十进制的数转换为 String -->
        // 1234 / 10 --> q = 123, r = 4 ---> '4'
        // 123 / 10 ----> q = 12 , r = 3 ---> '43'
        // ......
        // '4321' ---> reverse ---> '1234'
        while (!isZeroUint(remainder, uint64Count)) {
            // remainder / 10
            UintArithmetic.divideUintInplace(remainder, base, uint64Count, quotient);
            // 保留余数
            char digit = (char) ((char) remainder[0] + '0');
            output.append(digit);
            // 更新余数，作为下一轮的 被除数
            System.arraycopy(quotient, 0, remainder, 0, uint64Count);
        }
        // 反转 output
        output.reverse();
        // 输出 String
        String result = output.toString();
        if (result.isEmpty()) {
            return "0";
        }
        return result;
    }

    /**
     * convert value[startIndex, startIndex + uint64Count) to a hex String
     *
     * @param value
     * @param startIndex
     * @param uint64Count
     * @return
     */
    public static String uintToHexString(long[] value, int startIndex, int uint64Count) {

        assert !(uint64Count > 0 && value == null);

        // Start with a string with a zero for each nibble in the array.
        // nibble 值的是一个 hex char 对应的 decimal value, 占据 4-bit, 0-15
        int numNibbles = Common.mulSafe(uint64Count, Common.NIBBLES_PER_UINT64, false);
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < numNibbles; i++) {
            output.append('0');
        }

        // Iterate through each uint64 in array and set string with correct nibbles in hex.
        int nibbleIndex = numNibbles;
        int leftMostNonZeroPos = numNibbles;

        for (int i = 0; i < uint64Count; i++) {
            long part = value[startIndex + i];
            // Iterate through each nibble in the current uint64.
            // 可以认为一个 part 包含了 24(64-bit/4)个 nibble, 现在一个一个抠出来
            for (int j = 0; j < Common.NIBBLES_PER_UINT64; j++) {
                // 依次取 最低位的 4-bit, 就是当前的 nibble
                int nibble = (int) (part & (long) 0x0F);
                int pos = --nibbleIndex;
                if (nibble != 0) {
                    // If nibble is not zero, then update string and save this pos to determine
                    // number of leading zeros.
                    output.setCharAt(pos, Common.nibbleToUpperHex(nibble));
                    leftMostNonZeroPos = pos;
                }
                // 右移动 4-bit, 处理下一个 nibble
                part >>>= 4;
            }
        }
        // Trim string to remove leading zeros.
        // 只保留 [leftMostNonZeroPos,...)
        String result = output.substring(leftMostNonZeroPos);

        if (result.isEmpty()) {
            return "0";
        }
        return result;
    }

    /**
     * convert a uint, a long array, to a hex string
     *
     * @param value
     * @param uint64Count
     * @return
     */
    public static String uintToHexString(long[] value, int uint64Count) {

        assert !(uint64Count > 0 && value == null);

        // Start with a string with a zero for each nibble in the array.
        // nibble 值的是一个 hex char 对应的 decimal value, 占据 4-bit, 0-15
        int numNibbles = Common.mulSafe(uint64Count, Common.NIBBLES_PER_UINT64, false);
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < numNibbles; i++) {
            output.append('0');
        }

        // Iterate through each uint64 in array and set string with correct nibbles in hex.
        int nibbleIndex = numNibbles;
        int leftMostNonZeroPos = numNibbles;

        for (int i = 0; i < uint64Count; i++) {
            long part = value[i];
            // Iterate through each nibble in the current uint64.
            // 可以认为一个 part 包含了 24(64-bit/4)个 nibble, 现在一个一个抠出来
            for (int j = 0; j < Common.NIBBLES_PER_UINT64; j++) {
                // 依次取 最低位的 4-bit, 就是当前的 nibble
                int nibble = (int) (part & (long) 0x0F);
                int pos = --nibbleIndex;
                if (nibble != 0) {
                    // If nibble is not zero, then update string and save this pos to determine
                    // number of leading zeros.
                    output.setCharAt(pos, Common.nibbleToUpperHex(nibble));
                    leftMostNonZeroPos = pos;
                }
                // 右移动 4-bit, 处理下一个 nibble
                part >>>= 4;
            }
        }
        // Trim string to remove leading zeros.
        // 只保留 [leftMostNonZeroPos,...)
        String result = output.substring(leftMostNonZeroPos);

        if (result.isEmpty()) {
            return "0";
        }
        return result;
    }


    /**
     * convert a hex string to uint, a long array.
     *
     * @param hexString
     * @param charCount
     * @param uint64Count
     * @param result
     */
    public static void hexStringToUint(String hexString, int charCount, int uint64Count, long[] result) {
        // 如果 hexString 为空，charCount 同时又大于0，显然是错误
        assert !(hexString == null && charCount > 0);
        assert !(uint64Count > 0 && result == null);
        // todo: need mulSafe?
        assert !Common.unsignedGt(Common.getHexStringBitCount(hexString, charCount), Common.mulSafe(uint64Count, Common.BITS_PER_UINT64, true));
        // 从最低位遍历，也就是 最后一个 hex char
        int hexStringIndex = charCount;

        for (int uint64Index = 0; uint64Index < uint64Count; uint64Index++) {
            long value = 0;
            // 把一个 long 的 bits 填满, 每次处理一个 Nibble
            for (int bitIndex = 0; bitIndex < Common.BITS_PER_UINT64; bitIndex += Common.BITS_PER_NIBBLE) {
                // 处理完 charCount 个 char 就结束
                if (hexStringIndex == 0) {
                    break;
                }
                char hex = hexString.charAt(--hexStringIndex);
                int nibble = Common.hexToNibble(hex);
                // 表示当前字符不是 hex char
                if (nibble == -1) {
                    throw new IllegalArgumentException("current char: " + hex + "is not a hex char");
                }
                // 当前 nibble 放在 value 对应的 4-bit 位置上
                value |= ((long) nibble << bitIndex);
            }
            result[uint64Index] = value;
        }
    }

    public static void hexStringToUint(String hexString, int startIndex, int charCount, int uint64Count, int resultStartIndex, long[] result) {
        // 如果 hexString 为空，charCount 同时又大于0，显然是错误
        assert !(hexString == null && charCount > 0);
        assert !(uint64Count > 0 && result == null);
        // todo: need mulSafe?
        assert !Common.unsignedGt(Common.getHexStringBitCount(hexString, charCount), Common.mulSafe(uint64Count, Common.BITS_PER_UINT64, true));
        // 从最低位遍历，也就是 最后一个 hex char
        int hexStringIndex = charCount + startIndex;

        for (int uint64Index = 0; uint64Index < uint64Count; uint64Index++) {
            long value = 0;
            // 把一个 long 的 bits 填满, 每次处理一个 Nibble
            for (int bitIndex = 0; bitIndex < Common.BITS_PER_UINT64; bitIndex += Common.BITS_PER_NIBBLE) {
                // 处理完 charCount 个 char 就结束, 只要到了起点就结束了
                if (hexStringIndex == startIndex) {
                    break;
                }
                char hex = hexString.charAt(--hexStringIndex);
                int nibble = Common.hexToNibble(hex);
                // 表示当前字符不是 hex char
                if (nibble == -1) {
                    throw new IllegalArgumentException("current char: " + hex + "is not a hex char");
                }
                // 当前 nibble 放在 value 对应的 4-bit 位置上
                value |= ((long) nibble << bitIndex);
            }
            result[resultStartIndex + uint64Index] = value;
        }

    }


    /**
     * If the value is a power of two, return the power; otherwise, return -1.
     *
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
     *
     * @param value
     * @param uint64Count
     * @return values == 0
     */
    public static boolean isZeroUint(long[] value, int uint64Count) {

        assert uint64Count > 0;
        return Arrays.stream(value, 0, uint64Count).allMatch(n -> n == 0);
    }

    /**
     * determine whether the value[startIndex, startIndex + uint64Count) is all 0
     *
     * @param value
     * @param startIndex
     * @param uint64Count
     * @return value[startIndex, startIndex + uint64Count) == 0
     */
    public static boolean isZeroUint(long[] value, int startIndex, int uint64Count) {
        assert uint64Count > 0;

        for (int i = startIndex; i < startIndex + uint64Count; i++) {
            if (value[i] != 0) {
                return false;
            }
        }
        return true;
    }


    /**
     * note that values is a base-2^64 number
     *
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
     * @param operand1
     * @param operand1Uint64Count
     * @param operand2
     * @param operand2Uint64Count
     * @return operand1 == operand2
     */
    public static boolean isEqualUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count) {

        return compareUint(operand1, operand1Uint64Count, operand2, operand2Uint64Count) == 0;
    }

    /**
     * @param operand1
     * @param operand2
     * @param uint64Count
     * @return operand1 == operand2
     */
    public static boolean isEqualUint(long[] operand1, long[] operand2, int uint64Count) {

        return compareUint(operand1, operand2, uint64Count) == 0;
    }


    /**
     * @param operand1
     * @param operand1Uint64Count
     * @param operand2
     * @param operand2Uint64Count
     * @return operand1 > operand2
     */
    public static boolean isGreaterThanUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count) {

        return compareUint(operand1, operand1Uint64Count, operand2, operand2Uint64Count) > 0;
    }

    /**
     * @param operand1
     * @param operand2
     * @param uint64Count
     * @return operand1 > operand2
     */
    public static boolean isGreaterThanUint(long[] operand1, long[] operand2, int uint64Count) {

        return compareUint(operand1, operand2, uint64Count) > 0;
    }

    /**
     * @param operand1
     * @param operand2
     * @param uint64Count
     * @return operand1 >= operand2
     */
    public static boolean isGreaterThanOrEqualUint(long[] operand1, long[] operand2, int uint64Count) {

        return compareUint(operand1, operand2, uint64Count) >= 0;
    }

    /**
     * @param operand1
     * @param operand1Uint64Count
     * @param operand2
     * @param operand2Uint64Count
     * @return operand1 >= operand2
     */
    public static boolean isGreaterThanOrEqualUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count) {

        return compareUint(operand1, operand1Uint64Count, operand2, operand2Uint64Count) >= 0;
    }

    /**
     * @param operand1
     * @param operand2
     * @param uint64Count
     * @return operand1 < operand2
     */
    public static boolean isLessThanUint(long[] operand1, long[] operand2, int uint64Count) {
        return compareUint(operand1, operand2, uint64Count) < 0;
    }

    /**
     * @param operand1
     * @param operand1Uint64Count
     * @param operand2
     * @param operand2Uint64Count
     * @return operand1 < operand2
     */
    public static boolean isLessThanUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count) {

        return compareUint(operand1, operand1Uint64Count, operand2, operand2Uint64Count) < 0;
    }

    /**
     * @param operand1
     * @param operand2
     * @param uint64Count
     * @return operand1 <= operand2
     */
    public static boolean isLessThanOrEqualUint(long[] operand1, long[] operand2, int uint64Count) {
        return compareUint(operand1, operand2, uint64Count) <= 0;
    }

    /**
     * @param operand1
     * @param operand1Uint64Count
     * @param operand2
     * @param operand2Uint64Count
     * @return operand1 <= operand2
     */
    public static boolean isLessThanOrEqualUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count) {
        return compareUint(operand1, operand1Uint64Count, operand2, operand2Uint64Count) <= 0;
    }


    /**
     * note that operand1 and operand2 is a base-2^64 number
     *
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
     *
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
        if ((a > 0 && b > 0) || (a < 0 && b < 0)) {
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
     *
     * @param operand1
     * @param operand1Uint64Count
     * @param operand2
     * @param operand2Uint64Count
     * @return if operand1 > operand2 , return 1, else if operand1 < operand2 return -1, otherwise return 0
     */
    public static int compareUint(long[] operand1, int operand1Uint64Count, long[] operand2, int operand2Uint64Count) {

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
            result = operand1[operand1Index] != 0 ? 1 : 0;
        }
        operand2Uint64Count -= minUint64Count;
        for (; (result == 0) && operand2Uint64Count-- > 0; operand2Index--) {
//            result = -(operand2[operand2Index] > 0 ? 1: 0);
            result = -(operand1[operand1Index] != 0 ? 1 : 0);
        }

        for (; result == 0 && minUint64Count-- > 0; operand1Index--, operand2Index--) {
//            result = (operand1[operand1Index] > operand2[operand2Index] ? 1 : 0) - ((operand1[operand1Index] < operand2[operand2Index] ? 1 : 0));
            result = compareUint64(operand1[operand1Index], operand2[operand2Index]);
        }
        return result;
    }


    /**
     * such as : ([1, 0], 2) , will return 1, because the significant Uint64Count is just 1
     *
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
        for (; uint64Count > 0 && values[index] == 0; uint64Count--) {
            index--;
        }
        return uint64Count;
    }


    /**
     * @param value       a base-2^64 value
     * @param uint64Count the used count in value
     * @return the number of non-zero element in value[0, uint64Count)
     */
    public static int getNonZeroUint64CountUint(long[] value, int uint64Count) {

        assert uint64Count > 0;

        int nonZeroCount = uint64Count;

        int index = uint64Count - 1;

        for (; uint64Count > 0; uint64Count--) {
            if (value[index] == 0) {
                nonZeroCount--;
            }
            index--;
        }

        return nonZeroCount;
    }


    /**
     * Compute the most significant bit-count in values[0, uint64Count).
     * For example: [0, 0, 1] ---> 63 + 63 + 1 = 127 bits; [1, 0, 0, ] --->  1 + 0 + 0 = 1 bits
     *
     * @param values      a base-2^64 value
     * @param uint64Count number of uint64 count used in values
     * @return the most significant bit-count in values[0, uint64Count)
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
//        for (int i = 0; i < uint64Count; i++) {
//            values[i] = 0;
//        }

        Arrays.fill(values, 0, uint64Count, 0);
    }

    /**
     * set result[0] = value, result[i>0] = 0
     *
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
     *
     * @param values
     * @param uint64Count
     * @param result
     */
    public static void setUint(long[] values, int uint64Count, long[] result) {
        assert uint64Count >= 0;

        if (result == values || uint64Count == 0) {
            return;
        }
        // todo: 这里的写法有问题，起点不一定是0的, SEAL的参数直接是一个数组的起点
        System.arraycopy(values, 0, result, 0, uint64Count);
    }

    /**
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
        } else {
            int minUint64Count = Math.min(valueUint64Count, resultUint64Count);
            System.arraycopy(values, 0, result, 0, minUint64Count);
            Arrays.fill(result, minUint64Count, resultUint64Count, 0);
        }
    }

    /**
     * set the specific bitIndex of long[] to 1, long[] has  64 * uint64Count bits
     *
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
     * @param values
     * @param uint64Count
     * @param bitIndex
     * @return whether the value at specific bitIndex of given values is 1
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

    /**
     * @param input          a base-2^64 value
     * @param uint64Count    length of the input in base-2^64
     * @param newUint64Count length of the output in base-2^64
     * @param force          if true, then deep-copy, otherwise shallow-copy
     * @return a base-2^64 value, which equals input
     */
    public static long[] duplicateUintIfNeeded(long[] input, int uint64Count, int newUint64Count, boolean force) {

        if (!force && uint64Count >= newUint64Count) {
            return input;
        }
        long[] newUint = new long[newUint64Count];
        setUint(input, uint64Count, newUint64Count, newUint);
        return newUint;
    }


}
