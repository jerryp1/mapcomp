package edu.alibaba.mpc4j.crypto.fhe.modulus;

import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * @author Qixian Zhou
 * @date 2023/8/3
 */
public class Modulus implements Cloneable {


    // modulus value, up to 61 bits, must be positive number
    private long value;
    // bit-count of value
    private int bitCount;
    // uint64 count of value
    private int uint64Count;
    //Returns the Barrett ratio computed for the value of the current Modulus.
    //The first two components of the Barrett ratio are the floor of 2^128/value,
    //and the third component is the remainder.
    private long[] constRatio;
    // value is a prime
    private boolean isPrime;

    public Modulus(long value) {

        setValue(value);
    }

    public Modulus() {}


    /**
     * deep copy a Modulus object
     *
     * @param other another Modulus object
     */
    public Modulus(Modulus other) {
        this.value = other.value;
        this.bitCount = other.bitCount;
        this.uint64Count = other.uint64Count;
        this.isPrime = other.isPrime;
        this.constRatio = new long[3];
        System.arraycopy(other.constRatio, 0, constRatio, 0, 3);
    }

    /**
     * Set another value for the current Modulus object, which will completely change the current Modulus object
     *
     * @param value a long value
     */
    public void setValue(long value) {

        if (value == 0) {
            // zero settings
            bitCount = 0;
            uint64Count = 1;
            value = 0;
            constRatio = new long[] {0, 0, 0};
            isPrime = false;
        }else if (value >>> Constants.MOD_BIT_COUNT_MAX != 0 || (value == 1)) {
            throw new IllegalArgumentException("value can be at most 61-bit and cannot be 1");
        }else {
            this.value = value;
            bitCount = UintCore.getSignificantBitCount(value);
            uint64Count = 1;
            constRatio = new long[3];
            long[] numerator = new long[]{0, 0, 1}; // 2^128, 129 bits
            // first parameter numerator will be changed, so new a array instead of pass the TWO_POWER_128
            UintArithmetic.divideUint192Inplace(numerator, value, constRatio);
            constRatio[2] = numerator[0];

            isPrime = Numth.isPrime(value);
        }
    }

    public long[] getConstRatio() {
        return constRatio;
    }

    /**
     *
     * @param values an array of type long
     * @return An array of Modulus objects
     */
    public static Modulus[] createModulus(long[] values) {
        return Arrays.stream(values).mapToObj(Modulus::new).toArray(Modulus[]::new);
    }

    /**
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
        return Arrays.stream(as).parallel().map(this::reduce).toArray();
    }

    /**
     * convert a \in [0, p) to a \in [-p/2, p/2)
     *
     * @param a
     * @return a\in [-p/2, p/2)
     */
    public long center(long a) {
        assert a < value;
        // a >= p/2 ---> a - p , when a = p/2 , res = p/2 - p = -p/2
        // So the range is [-p/2, p/2)
        return a >= (value >> 1) ? a - value : a;
    }

    public long[] center(long[] as) {
        return Arrays.stream(as).parallel().map(this::center).toArray();
    }


    public long add(long a, long b) {
        assert a < value && b < value;
        return UintArithmeticSmallMod.addUintMod(a, b, this);
    }

    public long[] add(long[] as, long[] bs) {
        assert as.length == bs.length;
        IntStream indexIntStream = IntStream.range(0, as.length).parallel();
        return indexIntStream
                .mapToLong(i -> add(as[i], bs[i]))
                .toArray();
    }

    public long[] sub(long[] as, long[] bs) {
        assert as.length == bs.length;
        IntStream indexIntStream = IntStream.range(0, as.length).parallel();
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
        IntStream indexIntStream = IntStream.range(0, as.length).parallel();

        return indexIntStream
                .mapToLong(i -> mul(as[i], bs[i]))
                .toArray();

    }

    public long negate(long a) {
        assert a < value;
        return UintArithmeticSmallMod.negateUintMod(a, this);
    }

    public long[] negate(long[] as) {
        LongStream longStream = Arrays.stream(as).parallel();
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

    /**
     * convert x in [0, 2p) to [0, p)
     *
     * @param x in [0, 2p)
     * @param p modulus
     * @return x in [0, p)
     */
    public static long reduce1(long x, long p) {

        assert x < (p << 1);

        return x >= p ? x - p : x;
    }

    /**
     * convert a in [0, p) to Shoup Representation for better mul performance. Same as the MultiplyUintModOperation
     * here is more lightweight.
     *
     * @param a \in [0, p)
     * @return [(a < < 64) / p]'s low 64-bit
     */
    public long shoup(long a) {
        assert a < value;
        long[] wideQuotient = new long[2];
        // operand <<= 64
        long[] wideCoeff = new long[]{0, a};

        UintArithmetic.divideUint128Inplace(wideCoeff, value, wideQuotient);

        return wideQuotient[0];
    }

    /**
     * Same as multiplyUintModLazy, here is more lightweight
     *
     * @param a      in [0, p)
     * @param b      in [0, p)
     * @param bShoup shoup representation of b
     * @return a * b - q * p in [0, 2p), q is the high 64 bits of a * bShoup
     */
    public long mulShoupLazy(long a, long b, long bShoup) {
        assert b < value;
        assert shoup(b) == bShoup;

        long q = UintArithmetic.multiplyUint64Hw64(a, bShoup);
        // res \in [0, 2p)
        return a * b - q * value;
    }

    /**
     * Compute (a*b) mod p based mulShoupLazy and reduce1
     *
     * @param a      in [0, p)
     * @param b      in [0, p)
     * @param bShoup shoup representation of b
     * @return a * b mod p in [0, p)
     */
    public long mulShoup(long a, long b, long bShoup) {
        return reduce1(mulShoupLazy(a, b, bShoup), value);
    }


    public boolean isZero() {
        return value == 0;
    }

    /**
     * @param size   array length
     * @param random random object
     * @return a long array with random uniform elements in [0, p)
     */
    public long[] randomUniformArray(int size, Random random) {
        long[] res = new long[size];
        for (int i = 0; i < size; i++) {
            res[i] = Math.abs(random.nextLong()) % value;
        }
        return res;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Modulus)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        Modulus that = (Modulus) obj;
        return new EqualsBuilder()
                .append(this.value, that.value)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(value).toHashCode();
    }

    @Override
    public String toString() {
        return "Modulus{" +
                "value=" + value +
                ", bitCount=" + bitCount +
                ", uint64Count=" + uint64Count +
                ", constRatio=" + Arrays.toString(constRatio) +
                ", isPrime=" + isPrime +
                '}';
    }

    public int getBitCount() {
        return bitCount;
    }

    public int getUint64Count() {
        return uint64Count;
    }

    public long getValue() {
        return value;
    }

    public boolean isPrime() {
        return isPrime;
    }

    @Override
    public Modulus clone() {
        try {
            Modulus clone = (Modulus) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            clone.constRatio = new long[3];
            System.arraycopy(this.constRatio, 0, clone.constRatio, 0, 3);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
