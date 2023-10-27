package edu.alibaba.mpc4j.crypto.fhe.zq;

import java.math.BigInteger;

/**
 * This Class for some common arithmetic computation.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/common.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/9
 */
public class Common {
    /**
     * a long (uint64) value is 8-byte
     */
    public static final int BYTES_PER_UINT64 = Long.BYTES;
    /**
     * a hex value is 4-bit
     */
    public static final int BITS_PER_NIBBLE = 4;
    /**
     * a long (uint64) value is 64-bit
     */
    public static final int BITS_PER_UINT64 = Long.SIZE;
    /**
     * a byte value is 2-hex
     */
    public static final int NIBBLES_PER_BYTE = 2;
    /**
     * a long (uint64) value is 16-hex
     */
    public static final int NIBBLES_PER_UINT64 = BYTES_PER_UINT64 * NIBBLES_PER_BYTE;

    /**
     * Coverts a hex to a char (upper hex).
     *
     * @param nibble a hex.
     * @return a char (upper hex).
     */
    public static char nibbleToUpperHex(int nibble) {
        // nibble can only be [0, 16)
        assert (nibble >= 0 && nibble < 16);
        if (nibble < 10) {
            return (char) ((char) nibble + '0');
        }
        return (char) ((char) nibble + 'A' - 10);
    }

    /**
     * Determines whether the given character is a hex char.
     *
     * @param hex a char
     * @return whether the given char is a hex char.
     */
    public static boolean isHexChar(char hex) {
        // hex can be {'0',...,'9','A',...,'F','a',...,'f'
        if (hex >= '0' && hex <= '9') {
            return true;
        }
        if (hex >= 'A' && hex <= 'F') {
            return true;
        }
        return hex >= 'a' && hex <= 'f';
    }

    /**
     * Converts a hex char to corresponding decimal value.
     *
     * @param hex a hex char.
     * @return decimal corresponding to the hex char.
     */
    public static int hexToNibble(char hex) {
        if (hex >= '0' && hex <= '9') {
            return hex - '0';
        }
        if (hex >= 'A' && hex <= 'F') {
            return hex - 'A' + 10;
        }
        if (hex >= 'a' && hex <= 'f') {
            return hex - 'a' + 10;
        }
        assert isHexChar(hex);

        return -1;
    }

    /**
     * Gets bit_count of the hex string.
     *
     * @param hexString the hex string.
     * @param charCount the number of chars.
     * @return bit_count of the hex string.
     */
    public static int getHexStringBitCount(String hexString, int charCount) {
        // when hexString is null, we allow charCount <= 0
        assert !(hexString == null && charCount > 0);
        // when hexString is not null, we need charCount >= 0
        assert charCount >= 0;

        for (int i = 0; i < charCount; i++) {
            char hex = hexString.charAt(i);
            int nibble = hexToNibble(hex);
            // find the first non-zero hex char
            if (nibble != 0) {
                // bit_count for the first non-zero hex char
                int nibbleBits = UintCore.getSignificantBitCount(nibble);
                // bit_count for the remaining nibbles
                int remainingNibbles = (charCount - i - 1) * BITS_PER_NIBBLE;

                return nibbleBits + remainingNibbles;
            }
        }
        return 0;
    }

    /**
     * Gets bit_count of the hex string.
     *
     * @param hexString  the hex string.
     * @param startIndex the start index.
     * @param charCount  the number of chars.
     * @return bit_count of the hex string.
     */
    public static int getHexStringBitCount(String hexString, int startIndex, int charCount) {
        // when hexString is null, we allow charCount <= 0
        assert !(hexString == null && charCount > 0);
        // when hexString is not null, we need charCount >= 0
        assert charCount >= 0;

        for (int i = 0; i < charCount; i++) {
            char hex = hexString.charAt(i + startIndex);
            int nibble = hexToNibble(hex);
            // find the first non-zero hex char
            if (nibble != 0) {
                // bit_count for the first non-zero hex char
                int nibbleBits = UintCore.getSignificantBitCount(nibble);
                // bit_count for the remaining nibbles
                int remainingNibbles = (charCount - i - 1) * BITS_PER_NIBBLE;

                return nibbleBits + remainingNibbles;
            }
        }
        return 0;
    }

    /**
     * Gets the hamming weight of the byte value.
     *
     * @param value the byte value.
     * @return the hamming weight of the byte value.
     */
    public static int hammingWeight(byte value) {
        int t = value;
        t -= (t >> 1) & 0x55;
        t = (t & 0x33) + ((t >> 2) & 0x33);
        return (t + (t >> 4)) & 0x0F;
    }

    /**
     * Returns if the two double values are close in floating-point view, i.e., |v1 - v2| < max(v1, v2) * Math.ulp(1.0).
     *
     * @param v1 the double value v1.
     * @param v2 the double value v2.
     * @return true if the two double values are close.
     */
    public static boolean areClose(double v1, double v2) {
        double scaleFactor = Math.max(Math.max(Math.abs(v1), Math.abs(v2)), 1.0);
        return Math.abs(v1 - v2) < scaleFactor * Math.ulp(1.0);
    }

    /**
     * Returns if (uint64) in1 > (uint64) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint64) in1 > (uint64) in2.
     */
    public static boolean unsignedGt(long in1, long in2) {
        return Long.compareUnsigned(in1, in2) > 0;
    }

    /**
     * Returns if (uint64) in1 >= (uint64) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint64) in1 >= (uint64) in2.
     */
    public static boolean unsignedGeq(long in1, long in2) {
        return Long.compareUnsigned(in1, in2) >= 0;
    }

    /**
     * Returns if (uint32) in1 > (uint32) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint32) in1 > (uint32) in2.
     */
    public static boolean unsignedGt(int in1, int in2) {
        return Integer.compareUnsigned(in1, in2) > 0;
    }

    /**
     * Returns if (uint32) in1 >= (uint32) in2.
     *
     * @param in1 in1.
     * @param in2 in2.
     * @return true if (uint32) in1 >= (uint32) in2.
     */
    public static boolean unsignedGeq(int in1, int in2) {
        return Integer.compareUnsigned(in1, in2) >= 0;
    }


    public static long reverseBits(long operand, int bitCount) {
        assert bitCount >= 0;
        assert bitCount <= 64;

        if (bitCount == 0) {
            return 0;
        }
        // take low bitCount bits
        return reverseBits(operand) >>> (64 - bitCount);
    }

    public static int reverseBits(int operand, int bitCount) {
        assert bitCount >= 0;
        assert bitCount <= 32;

        if (bitCount == 0) {
            return 0;
        }
        // take low bitCount bits
        return reverseBits(operand) >>> (32 - bitCount);
    }

    /**
     * Returns the value obtained by reversing the order of the bits in the two's complement binary representation of
     * the specified int value.
     *
     * @param operand the value to be reversed.
     * @return the value obtained by reversing order of the bits in the specified int value.
     */
    public static int reverseBits(int operand) {
        return Integer.reverse(operand);
    }

    /**
     * Returns the value obtained by reversing the order of the bits in the two's complement binary representation of
     * the specified long value.
     *
     * @param operand the value to be reversed.
     * @return the value obtained by reversing order of the bits in the specified long value.
     */
    public static long reverseBits(long operand) {
        return Long.reverse(operand);
    }

    /**
     * Gets the most significant bit (msb) index of the value. For example:
     * <li>the msb of 1 is the 0-th bit.</li>
     * <li>the msb of 2 is the 1-th bit.</li>
     *
     * @param value the value.
     * @return the most significant bit (msb) index of the value.
     */
    public static int getMsbIndex(long value) {
        return 63 - Long.numberOfLeadingZeros(value);
    }

    /**
     * @param a
     * @param b
     * @param unsigned
     * @param numbers
     * @return
     */
    public static int mulSafe(int a, int b, boolean unsigned, int... numbers) {

        int prod = mulSafe(a, b, unsigned);
        for (int n : numbers) {
            prod = mulSafe(prod, n, unsigned);
        }
        return prod;
    }

    /**
     * @param a
     * @param b
     * @param unsigned
     * @param numbers
     * @return
     */
    public static long mulSafe(long a, long b, boolean unsigned, long... numbers) {

        long prod = mulSafe(a, b, unsigned);
        for (long n : numbers) {
            prod = mulSafe(prod, n, unsigned);
        }
        return prod;
    }

//    public static boolean productFitsIn(boolean unsigned, int in1, int... numbers) {
//
//        try{
//            mulSafe(in1, 1, unsigned, numbers);
//        }catch (Exception e) {
//            return false;
//        }
//        return true;
//    }

    public static boolean productFitsIn(boolean unsigned, int... numbers) {

        try {
            mulSafe(1, 1, unsigned, numbers);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

//    public static boolean productFitsIn(boolean unsigned, long in1, long... numbers) {
//
//        try{
//            mulSafe(in1, 1, unsigned, numbers);
//        }catch (Exception e) {
//            return false;
//        }
//        return true;
//    }

    public static boolean productFitsIn(boolean unsigned, long... numbers) {

        try {
            mulSafe(1, 1, unsigned, numbers);
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    /**
     * @param a
     * @param b
     * @param unsigned
     * @return a*b
     */
    public static int mulSafe(int a, int b, boolean unsigned) {

        // 把 long 视为 uint64
        if (unsigned) {
            // 分类讨论，以下情况一定会溢出
            // neg * neg, 64-bit * 64-bit > 64bits
            if (a < 0 && b < 0) {
                throw new ArithmeticException("unsigned overflow");
            }
            // neg * pos, pos > 1 时一定溢出，最小的neg 是 2^63, * 2 一定溢出了
            if ((a < 0 && b > 1) || (a > 1 && b < 0)) {
                throw new ArithmeticException("unsigned overflow");
            }
            // pos * pos , 情况就很复杂了，假设 a * b = c , a.bit_length() + b.bit_length() >= c
            // 暂时没有想到很好的判断方法，所以引入 BigInteger 来帮助，只在这一个点使用，对性能的影响还算能接收
            // 后面思考出了更好的方法，再替换
            // 只要有一个等于1, 肯定是不会溢出的
            if (a > 1 && b > 1) {
                // a 最小也是2, (2^32 - 1) / 2 可以用 int 放下
                long tmp = new BigInteger("FFFFFFFF", 16).divide(BigInteger.valueOf(a)).longValue();
                if (b > tmp) {
                    throw new ArithmeticException("unsigned overflow");
                }
            }
        } else { // 按 int64 来判断，简单多了
            // a * b > 0 的时候，溢出指的是结果 大于 2^63 - 1
            if ((a > 0) && (b > 0) && (b > Integer.MAX_VALUE / a)) {
                throw new ArithmeticException("signed overflow");
            } else if ((a < 0) && (b < 0) && ((-b) > Integer.MAX_VALUE / (-a))) {
                throw new ArithmeticException("signed overflow");
            } else if ((a < 0) && (b > 0) && (b > Integer.MAX_VALUE / (-a))) {
                // a * b < 0 的时候，溢出指的是 结果 小于 -2^63
                throw new ArithmeticException("unsigned overflow");
            } else if ((a > 0) && (b < 0) && (b < (Integer.MIN_VALUE / a))) {
                throw new ArithmeticException("unsigned overflow");
            }
        }
        return a * b;
    }


    /**
     * @param a
     * @param b
     * @param unsigned
     * @return a*b
     */
    public static long mulSafe(long a, long b, boolean unsigned) {

        // 把 long 视为 uint64
        if (unsigned) {
            // 分类讨论，以下情况一定会溢出
            // neg * neg, 64-bit * 64-bit > 64bits
            if (a < 0 && b < 0) {
                throw new ArithmeticException("unsigned overflow");
            }
            // neg * pos, pos > 1 时一定溢出，最小的neg 是 2^63, * 2 一定溢出了
            if ((a < 0 && b > 1) || (a > 1 && b < 0)) {
                throw new ArithmeticException("unsigned overflow");
            }
            // pos * pos , 情况就很复杂了，假设 a * b = c , a.bit_length() + b.bit_length() >= c
            // 暂时没有想到很好的判断方法，所以引入 BigInteger 来帮助，只在这一个点使用，对性能的影响还算能接收
            // 后面思考出了更好的方法，再替换
            // 只要有一个等于1, 肯定是不会溢出的
            if (a > 1 && b > 1) {
                // a 最小也是2, (2^64 - 1) / 2 可以用 long 放下
                long tmp = Long.divideUnsigned(0xFFFFFFFFFFFFFFFFL, a);
                if (b > tmp) {
                    throw new ArithmeticException("unsigned overflow");
                }
            }
        } else { // 按 int64 来判断，简单多了
            // a * b > 0 的时候，溢出指的是结果 大于 2^63 - 1
            if ((a > 0) && (b > 0) && (b > Long.MAX_VALUE / a)) {
                throw new ArithmeticException("signed overflow");
            } else if ((a < 0) && (b < 0) && ((-b) > Long.MAX_VALUE / (-a))) {
                throw new ArithmeticException("signed overflow");
            } else if ((a < 0) && (b > 0) && (b > Long.MAX_VALUE / (-a))) {
                // a * b < 0 的时候，溢出指的是 结果 小于 -2^63
                throw new ArithmeticException("unsigned overflow");
            } else if ((a > 0) && (b < 0) && (b < (Long.MIN_VALUE / a))) {
                throw new ArithmeticException("unsigned overflow");
            }
        }
        return a * b;
    }


    /**
     * unsigned decides the max and min of long. if unsigned is true, we treat long as uint64
     * otherwise, treat long as int64
     *
     * @param a
     * @param b
     * @param unsigned
     * @return
     */
    public static int subSafe(int a, int b, boolean unsigned) {

        if (unsigned) {
            // the core is judge (in1 + in2)'s bit string  larger than 0xFFFFFFFF...
            // the logic here is same as the borrow  computation in subUint64
            // 0 is the smallest, borrow = 1
            if (a == 0 && b != 0) {
                throw new ArithmeticException("unsigned underflow");
            }

            if (a > 0 && b > 0 && a < b) {
                throw new ArithmeticException("unsigned underflow");
            }
            if (a < 0 && b < 0) {
                if (a < b) {
                    throw new ArithmeticException("unsigned underflow");
                }
            }
            if (a > 0 && b < 0) {
                throw new ArithmeticException("unsigned underflow");
            }
        } else {
            if (a < 0 && (b > Integer.MAX_VALUE + a)) {
                throw new ArithmeticException("signed overflow");
            } else if (a > 0 && (b < Integer.MIN_VALUE + a)) {
                throw new ArithmeticException("signed underflow");
            }
        }
        return a - b;
    }

    /**
     * unsigned decides the max and min of long. if unsigned is true, we treat long as uint64
     * otherwise, treat long as int64
     *
     * @param a
     * @param b
     * @param unsigned
     * @return
     */
    public static long subSafe(long a, long b, boolean unsigned) {

        if (unsigned) {
            // the core is judge (in1 + in2)'s bit string  larger than 0xFFFFFFFF...
            // the logic here is same as the borrow  computation in subUint64
            // 0 is the smallest, borrow = 1
            if (a == 0 && b != 0) {
                throw new ArithmeticException("unsigned underflow");
            }

            if (a > 0 && b > 0 && a < b) {
                throw new ArithmeticException("unsigned underflow");
            }
            if (a < 0 && b < 0) {
                if (a < b) {
                    throw new ArithmeticException("unsigned underflow");
                }
            }
            if (a > 0 && b < 0) {
                throw new ArithmeticException("unsigned underflow");
            }
        } else {
            if (a < 0 && (b > Long.MAX_VALUE + a)) {
                throw new ArithmeticException("signed overflow");
            } else if (a > 0 && (b < Long.MIN_VALUE + a)) {
                throw new ArithmeticException("signed underflow");
            }
        }
        return a - b;
    }


    /**
     * unsigned decides the max and min of long. if unsigned is true, we treat long as uint64
     * otherwise, treat long as int64
     *
     * @param a
     * @param b
     * @param unsigned
     * @return in1 + in2
     */
    public static long addSafe(long a, long b, boolean unsigned) {
        // treat a and b as uint64_t
        if (unsigned) {
            // the core is judge (in1 + in2)'s bit string  larger than 0xFFFFFFFF...
            // the logic here is same as the carry's computation in addUint64
            if (a < 0 && b < 0) {
                throw new ArithmeticException("unsigned overflow");
            }
            if ((a < 0 && b > 0) || (a > 0 && b < 0)) {
                if (a + b >= 0) {
                    throw new ArithmeticException("unsigned overflow");
                }
            }
        } else { // treat a and b as int64

            if (a > 0 && (b > Long.MAX_VALUE - a)) {
                throw new ArithmeticException("signed overflow");
            } else if (a < 0 && (b < Long.MIN_VALUE - a)) {
                throw new ArithmeticException("signed underflow");
            }
        }
        // do not overflow, can be added
        return a + b;
    }

    /**
     * add safe with variable numbers parameters
     *
     * @param a
     * @param b
     * @param unsigned
     * @param numbers
     * @return
     */
    public static long addSafe(long a, long b, boolean unsigned, long... numbers) {
        long sum = addSafe(a, b, unsigned);
        for (long n : numbers) {
            sum = addSafe(sum, n, unsigned);
        }
        return sum;
    }


    /**
     * unsigned decides the max and min of long. if unsigned is true, we treat long as uint64
     * otherwise, treat long as int64
     *
     * @param a
     * @param b
     * @param unsigned
     * @return in1 + in2
     */
    public static int addSafe(int a, int b, boolean unsigned) {
        // treat a and b as uint64_t
        if (unsigned) {
            // the core is judge (in1 + in2)'s bit string  larger than 0xFFFFFFFF...
            // the logic here is same as the carry's computation in addUint64
            if (a < 0 && b < 0) {
                throw new ArithmeticException("unsigned overflow");
            }
            if ((a < 0 && b > 0) || (a > 0 && b < 0)) {
                if (a + b >= 0) {
                    throw new ArithmeticException("unsigned overflow");
                }
            }
        } else { // treat a and b as int64

            if (a > 0 && (b > Integer.MAX_VALUE - a)) {
                throw new ArithmeticException("signed overflow");
            } else if (a < 0 && (b < Integer.MIN_VALUE - a)) {
                throw new ArithmeticException("signed underflow");
            }
        }
        // do not overflow, can be added
        return a + b;
    }

    /**
     * add safe with variable numbers parameters
     *
     * @param a
     * @param b
     * @param unsigned
     * @param numbers
     * @return
     */
    public static int addSafe(int a, int b, boolean unsigned, int... numbers) {
        int sum = addSafe(a, b, unsigned);
        for (int n : numbers) {
            sum = addSafe(sum, n, unsigned);
        }
        return sum;
    }

}
