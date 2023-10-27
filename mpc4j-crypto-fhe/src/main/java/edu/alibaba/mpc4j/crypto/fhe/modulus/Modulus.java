package edu.alibaba.mpc4j.crypto.fhe.modulus;

import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Arrays;

/**
 * Represent an integer modulus of up to 61 bits. An instance of the Modulus class represents a non-negative integer
 * modulus up to 61 bits. In particular, the encryption parameter plain_modulus, and the primes in coeff_modulus, are
 * represented by instances of Modulus. The purpose of this class is to perform and store the pre-computation required
 * by Barrett reduction.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/modulus.h.
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/3
 */
public class Modulus implements Cloneable {
    /**
     * Creates modulus for a long array.
     *
     * @param values a long array.
     * @return a modulus array.
     */
    public static Modulus[] createModulus(long[] values) {
        return Arrays.stream(values).mapToObj(Modulus::new).toArray(Modulus[]::new);
    }

    /**
     * modulus value, up to 61 bits, must be positive number
     */
    private long value;
    /**
     * bit-count of value
     */
    private int bitCount;
    /**
     * uint64 count of value
     */
    private int uint64Count;
    /**
     * the Barrett ratio computed for the value of the current Modulus. The first two components of the Barrett ratio
     * are the floor of 2^128 / value, and the third component is the remainder.
     */
    private long[] constRatio;
    /**
     * Whether value is a prime number
     */
    private boolean isPrime;

    /**
     * Creates a Modulus instance. The value of the Modulus is set to the given value.
     *
     * @param value a given value.
     */
    public Modulus(long value) {
        setValue(value);
    }

    /**
     * Creates a Modulus instance. The value of the Modulus is set to zero by default.
     */
    public Modulus() {
        setValue(0);
    }

    /**
     * Creates a new Modulus by copying a given one.
     *
     * @param other the Modulus to move from.
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
     * Set another modulus value, which will completely change the current modulus contents.
     *
     * @param value a given value.
     */
    public void setValue(long value) {
        if (value == 0) {
            // zero settings
            bitCount = 0;
            uint64Count = 1;
            this.value = 0;
            constRatio = new long[]{0, 0, 0};
            isPrime = false;
        } else if (value >>> Constants.MOD_BIT_COUNT_MAX != 0 || (value == 1)) {
            throw new IllegalArgumentException("value can be at most 61-bit and cannot be 1");
        } else {
            // All normal, compute const_ratio and set everything
            this.value = value;
            bitCount = UintCore.getSignificantBitCount(value);
            uint64Count = 1;
            constRatio = new long[3];
            // Compute Barrett ratios for 64-bit words (barrett_reduce_128)
            long[] numerator = new long[]{0, 0, 1};
            UintArithmetic.divideUint192Inplace(numerator, value, constRatio);
            constRatio[2] = numerator[0];
            // Set the primality flag
            isPrime = Numth.isPrime(value);
        }
    }

    /**
     * Returns the Barrett ratio computed for the value of the current Modulus. The first two components of the Barrett
     * ratio are the floor of 2^128 / value, and the third component is the remainder.
     *
     * @return the Barrett ratio computed for the value of the current Modulus.
     */
    public long[] getConstRatio() {
        return constRatio;
    }

    /**
     * Reduces a given unsigned integer modulo this modulus.
     *
     * @param input the unsigned integer to reduce.
     * @return input mod value.
     */
    public long reduce(long input) {
        if (value == 0) {
            throw new IllegalArgumentException("cannot reduce modulo a zero modulus");
        }
        return UintArithmeticSmallMod.barrettReduce64(input, this);
    }

    /**
     * Returns whether the value of the current Modulus is zero.
     *
     * @return whether the value of the current Modulus is zero.
     */
    public boolean isZero() {
        return value == 0;
    }

    /**
     * Returns the significant bit count of the value of the current Modulus.
     *
     * @return the significant bit count of the value of the current Modulus.
     */
    public int getBitCount() {
        return bitCount;
    }

    /**
     * Returns the size (in 64-bit words) of the value of the current Modulus.
     *
     * @return the size (in 64-bit words) of the value of the current Modulus.
     */
    public int getUint64Count() {
        return uint64Count;
    }

    /**
     * Returns the value of the current Modulus.
     *
     * @return the value of the current Modulus.
     */
    public long getValue() {
        return value;
    }

    /**
     * Returns whether the value of the current Modulus is a prime number.
     *
     * @return whether the value of the current Modulus is a prime number.
     */
    public boolean isPrime() {
        return isPrime;
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
        return new ToStringBuilder(this)
            .append("value", value)
            .append("bitCount", bitCount)
            .append("uint64Count", uint64Count)
            .append("constRatio", constRatio)
            .append("isPrime", isPrime)
            .build();
    }

    @Override
    public Modulus clone() {
        try {
            Modulus clone = (Modulus) super.clone();
            clone.constRatio = new long[3];
            System.arraycopy(this.constRatio, 0, clone.constRatio, 0, 3);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
