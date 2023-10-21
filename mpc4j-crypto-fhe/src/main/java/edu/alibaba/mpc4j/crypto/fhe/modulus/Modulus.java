package edu.alibaba.mpc4j.crypto.fhe.modulus;

import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.zq.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;


/**
 * Represent an integer modulus of up to 61 bits. An instance of the Modulus
 * class represents a non-negative integer modulus up to 61 bits. In particular,
 * the encryption parameter plain_modulus, and the primes in coeff_modulus, are
 * represented by instances of Modulus. The purpose of this class is to
 * perform and store the pre-computation required by Barrett reduction.
 * <p>
 * The implementation is from `class Modulus` in https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/modulus.h.
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/3
 */
public class Modulus implements Cloneable {


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
     * constRatio[0, 1] stores the (2^128/value)'s quotient,  constRatio[2] stores (2^128/value)'s remainder
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
     * Set another value for the current Modulus object, which will completely change the current Modulus object's contents.
     *
     * @param value a given modulus's value
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
            // 这里就是为 barret reduce 进行预计算: 2^128 / value, 商保存在 constRatio[0, 1]
            // 余数保存在 constRatio[2]
            constRatio = new long[3];
            // 2^128 / value ---> quotient is stored in constRatio[0, 1] and remainder is stored in constRatio[2]
            long[] numerator = new long[]{0, 0, 1};
            UintArithmetic.divideUint192Inplace(numerator, value, constRatio);
            constRatio[2] = numerator[0];
            // Set the primality flag
            isPrime = Numth.isPrime(value);
        }
    }

    public long[] getConstRatio() {
        return constRatio;
    }

    /**
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

        if (value == 0) {
            throw new IllegalArgumentException("cannot reduce modulo a zero modulus");
        }

        return UintArithmeticSmallMod.barrettReduce64(input, this);
    }

    /**
     * @return whether value is zero
     */
    public boolean isZero() {
        return value == 0;
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

        StringBuilder sb = new StringBuilder();
        sb.append("Modulus{" + "value=").append(value).append(", bitCount=").append(bitCount).append(", uint64Count=").append(uint64Count).append(", constRatio=[");
        for (int i = 0; i < constRatio.length; i++) {
            sb.append(constRatio[i]);
            if (i < constRatio.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("] ");
        sb.append(", isPrime=").append(isPrime).append('}');

        return sb.toString();
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
