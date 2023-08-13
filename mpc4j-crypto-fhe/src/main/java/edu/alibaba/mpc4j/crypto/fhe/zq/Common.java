package edu.alibaba.mpc4j.crypto.fhe.zq;

import java.math.BigInteger;

/**
 * This Class for some common arithmetic computation, just like safeArithmetic(safeAdd, safeMul..)
 * @author Qixian Zhou
 * @date 2023/8/9
 */
public class Common {


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
     * treat int as uint32
     * @param operand
     * @return
     */
    public static int reverseBits(int operand) {
       // 0b10101010101010101010101010101010      0b1010101010101010101010101010101
       operand = ((operand & 0xaaaaaaaa) >>> 1) |  ((operand & 0x55555555) << 1);
       operand = ((operand & 0xcccccccc) >>> 2) |  ((operand & 0x33333333) << 2);
       operand = ((operand & 0xf0f0f0f0) >>> 4) |  ((operand & 0x0f0f0f0f) << 4);
       operand = ((operand & 0xff00ff00) >>> 8) |  ((operand & 0x00ff00ff) << 8);

       return (operand >>> 16) | (operand << 16);
    }

    public static long reverseBits(long operand) {
        // (int) operand equals (int) (operand & 0xFFFFFFFF)
        int a =  reverseBits((int)(operand >>> 32));
        // avoid negate lead error, just like when a = -65536 = 0x0000FFFF, cast to long, we expect is 0x 0000 0000 0000 FFFF
        // if we directly long b = (long) a; we will get 0x FFFF FFFF FFFF 0000 , this is error
        long au = Long.parseUnsignedLong(Integer.toHexString(a), 16);
        int b = reverseBits((int) (operand));
        long bu = Long.parseUnsignedLong(Integer.toHexString(b), 16);
        return au | bu << 32;
    }




    /**
     *
     * @param value
     * @return the msb bit index of value, such as 1->0, 2->1
     */
    public static int getMsbIndex(long value) {
        return 63 - Long.numberOfLeadingZeros(value);
    }


    /**
     *
     * @param a
     * @param b
     * @param unsigned
     * @param numbers
     * @return
     */
    public static int mulSafe(int a, int b, boolean unsigned, int... numbers) {

        int prod = mulSafe(a, b, unsigned);
        for (int n: numbers) {
            prod = mulSafe(prod, n, unsigned);
        }
        return prod;
    }

    /**
     *
     * @param a
     * @param b
     * @param unsigned
     * @param numbers
     * @return
     */
    public static long mulSafe(long a, long b, boolean unsigned, long... numbers) {

        long prod = mulSafe(a, b, unsigned);
        for (long n: numbers) {
            prod = mulSafe(prod, n, unsigned);
        }
        return prod;
    }


    /**
     *
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
        }else { // 按 int64 来判断，简单多了
            // a * b > 0 的时候，溢出指的是结果 大于 2^63 - 1
            if ((a > 0) && (b > 0) && (b > Integer.MAX_VALUE / a)) {
                throw new ArithmeticException("signed overflow");
            }else if (  (a < 0) && (b < 0) && ( (-b) > Integer.MAX_VALUE / (-a))) {
                throw new ArithmeticException("signed overflow");
            } else if (  (a < 0) && (b > 0) && (b > Integer.MAX_VALUE / (-a))    ) {
                // a * b < 0 的时候，溢出指的是 结果 小于 -2^63
                throw new ArithmeticException("unsigned overflow");
            }else if ( (a > 0) && (b < 0) && (b < (Integer.MIN_VALUE / a))) {
                throw new ArithmeticException("unsigned overflow");
            }
        }
        return a * b;
    }



    /**
     *
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
                long tmp = new BigInteger("FFFFFFFFFFFFFFFF", 16).divide(BigInteger.valueOf(a)).longValue();
                if (b > tmp) {
                    throw new ArithmeticException("unsigned overflow");
                }
            }
        }else { // 按 int64 来判断，简单多了
            // a * b > 0 的时候，溢出指的是结果 大于 2^63 - 1
            if ((a > 0) && (b > 0) && (b > Long.MAX_VALUE / a)) {
                throw new ArithmeticException("signed overflow");
            }else if (  (a < 0) && (b < 0) && ( (-b) > Long.MAX_VALUE / (-a))) {
                throw new ArithmeticException("signed overflow");
            } else if (  (a < 0) && (b > 0) && (b > Long.MAX_VALUE / (-a))    ) {
            // a * b < 0 的时候，溢出指的是 结果 小于 -2^63
                throw new ArithmeticException("unsigned overflow");
            }else if ( (a > 0) && (b < 0) && (b < (Long.MIN_VALUE / a))) {
                throw new ArithmeticException("unsigned overflow");
            }
        }
        return a * b;
    }


    /**
     * unsigned decides the max and min of long. if unsigned is true, we treat long as uint64
     * otherwise, treat long as int64
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
            if (a < 0 && b < 0 ) {
                if (a < b) {
                    throw new ArithmeticException("unsigned underflow");
                }
            }
            if (a > 0 && b < 0) {
                throw new ArithmeticException("unsigned underflow");
            }
        }else {
            if (a < 0 && (b > Integer.MAX_VALUE + a)) {
                throw new ArithmeticException("signed overflow");
            }else if (a > 0 && ( b < Integer.MIN_VALUE + a)) {
                throw new ArithmeticException("signed underflow");
            }
        }
        return a - b;
    }

    /**
     * unsigned decides the max and min of long. if unsigned is true, we treat long as uint64
     * otherwise, treat long as int64
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
        }else {
            if (a < 0 && (b > Long.MAX_VALUE + a)) {
                throw new ArithmeticException("signed overflow");
            }else if (a > 0 && ( b < Long.MIN_VALUE + a)) {
                throw new ArithmeticException("signed underflow");
            }
        }
        return a - b;
    }



    /**
     * unsigned decides the max and min of long. if unsigned is true, we treat long as uint64
     * otherwise, treat long as int64
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
        }else { // treat a and b as int64

            if (a > 0 && (b > Long.MAX_VALUE - a)) {
                throw new ArithmeticException("signed overflow");
            }else if (a < 0 && (b < Long.MIN_VALUE - a)) {
                throw new ArithmeticException("signed underflow");
            }
        }
        // do not overflow, can be added
        return a + b;
    }

    /**
     * add safe with variable numbers parameters
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
        }else { // treat a and b as int64

            if (a > 0 && (b > Integer.MAX_VALUE - a)) {
                throw new ArithmeticException("signed overflow");
            }else if (a < 0 && (b < Integer.MIN_VALUE - a)) {
                throw new ArithmeticException("signed underflow");
            }
        }
        // do not overflow, can be added
        return a + b;
    }

    /**
     * add safe with variable numbers parameters
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
