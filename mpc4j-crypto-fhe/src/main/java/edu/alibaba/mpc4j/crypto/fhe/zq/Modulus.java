package edu.alibaba.mpc4j.crypto.fhe.zq;

import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * @author Qixian Zhou
 * @date 2023/8/3
 */
public class Modulus {


    // modulus value, up to 61 bits, must be positive number
    public long value;
    // bit-count of value
    public int bitCount;
    //
    public int uint64Count;
    // store the quotient of 2^128 / value base-2^63
    // the length is always 3
    public long[] barrettQuotient;
    // remainder of 2^128 / value
    public long barrettRemainder;

    // value is a prime
    public boolean isPrime;

    // when handle array, whether open parallel, default false
    public boolean parallel;

    public boolean supportsOpt;





    public Modulus(long value) {

        if (value < 2 || value >> Constants.MOD_BIT_COUNT_MAX != 0) {
            throw new IllegalArgumentException("Invalid modulus value: " + value + " should be between 2 and (1 << 61) - 1.");
        }
        this.value = value;
        bitCount = UintCore.getSignificantBitCount(value);
        uint64Count = 1;
        barrettQuotient = new long[3];
        long[] numerator = new long[] {0, 0, 1}; // 2^128, 129 bits
        // first parameter numerator will be changed, so new a array instead of pass the TWO_POWER_128
        UintArithmetic.divideUint192Inplace(numerator, value, barrettQuotient);
        barrettRemainder = numerator[0];

        isPrime = Primes.isPrime(value);
        parallel = false;
    }

    public boolean getParallel() {
        return parallel;
    }
    public void  setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    public void setValue(long value) {

        if (value < 2 || value >> Constants.MOD_BIT_COUNT_MAX != 0) {
            throw new IllegalArgumentException("Invalid modulus value: " + value + " should be between 2 and (1 << 61) - 1.");
        }
        this.value = value;
        bitCount = LongArithmetic.getUint63ValueBitCount(value);
        uint64Count = 1;
        barrettQuotient = new long[3];
        long[] numerator = new long[] {0, 0, 1}; // 2^128, 129 bits
        // first parameter numerator will be changed, so new a array instead of pass the TWO_POWER_128
        UintArithmetic.divideUint192Inplace(numerator, value, barrettQuotient);
        barrettRemainder = numerator[0];

        isPrime = Primes.isPrime(value);
    }

    /**
     *
     * @param input input
     * @return input mod value using barrett reduce
     */
    public long reduce(long input) {

        if (input == 0 || input == value) {
            return 0;
        }
        if (input == 1) {
            return 1;
        }

        return UintArithmeticSmallMod.barrettReduce64(input, this);
    }


    public long[] reduce(long[] as) {
        LongStream longStream = Arrays.stream(as);
        longStream = parallel ? longStream.parallel() : longStream;
        return longStream.map(this::reduce).toArray();
    }

    /**
     * convert a \in [0, p) to a \in [-p/2, p/2)
     * @param a
     * @return  a\in [-p/2, p/2)
     */
    public long center(long a) {
        assert a < value;
        // a >= p/2 ---> a - p , when a = p/2 , res = p/2 - p = -p/2
        // So the range is [-p/2, p/2)
        return a >= ( value >> 1) ? a - value : a;
    }

    public long[] center(long[] as) {
        LongStream longStream = Arrays.stream(as);
        longStream = parallel ? longStream.parallel() : longStream;
        return longStream.map(this::center).toArray();
    }


    public long add(long a, long b) {
        assert a < value && b < value;
        return UintArithmeticSmallMod.addUintMod(a, b, this);
    }

    public long[] add(long[] as, long[] bs) {
        assert as.length == bs.length;
        IntStream indexIntStream = IntStream.range(0, as.length);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        return indexIntStream
                .mapToLong(i -> add(as[i], bs[i]))
                .toArray();
    }
    public long[] sub(long[] as, long[] bs) {
        assert as.length == bs.length;
        IntStream indexIntStream = IntStream.range(0, as.length);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        return indexIntStream
                .mapToLong(i -> sub(as[i], bs[i]))
                .toArray();
    }




    public long sub(long a, long b) {
        assert a < value && b < value;
        return UintArithmeticSmallMod.subUintMod(a, b, this);
    }

    public long mul(long a, long b) {
        assert a < value && b < value;
        return UintArithmeticSmallMod.multiplyUintMod(a, b, this);
    }

    public long[] mul(long[] as, long[] bs) {
        assert as.length == bs.length;
        IntStream indexIntStream = IntStream.range(0, as.length);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        if (supportsOpt) {
            return indexIntStream
                    .mapToLong(i -> mul(as[i], bs[i]))
                    .toArray();
        }else {
            return indexIntStream
                    .mapToLong(i -> mul(as[i], bs[i]))
                    .toArray();
        }
    }

    public long negate(long a) {
        assert a < value;
        return UintArithmeticSmallMod.negateUintMod(a, this);
    }

    public long[] negate(long[] as) {
        LongStream longStream = Arrays.stream(as);
        longStream = parallel ? longStream.parallel() : longStream;
        return longStream.map(this::negate).toArray();
    }



    public long exponent(long a, long n) {
        assert a < value && n < value;
        return UintArithmeticSmallMod.exponentUintMod(a, n, this);
    }

    public long inv(long a) {
        assert a < value;
        long[] res = new long[1];
        if (!UintArithmeticSmallMod.tryInvertUintMod(a, this, res)) {
            throw new ArithmeticException("a's invert under mod modulus do not exists");
        }
        return res[0];
    }






    public int getBitCount() {
        return bitCount;
    }

    public int getUint64Count() {
        return uint64Count;
    }

    public long getBarrettRemainder() {
        return barrettRemainder;
    }

    public long getValue() {
        return value;
    }

    public long[] getBarrettQuotient() {
        return barrettQuotient;
    }

    public boolean isPrime() {
        return isPrime;
    }
}
