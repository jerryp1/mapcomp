package edu.alibaba.mpc4j.crypto.fhe.zq;

import com.google.common.math.LongMath;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import gnu.trove.list.array.TIntArrayList;

import java.math.BigInteger;
import java.util.Random;

/**
 * Number Theory methods.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/numth.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/9
 */
public class Numth {
    /**
     * Converts the value to its non-adjacent form (NAF).
     * <p>
     * The NAF of a number is a unique signed-digit representation, in which non-zero values cannot be adjacent.
     * For example:
     * <ul>
     * <li>( 0  1  1  1)_2 =     4 + 2 + 1 = 7</li>
     * <li>( 1  0 -1  1)_2 = 8     - 2 + 1 = 7</li>
     * <li>( 1 −1  1  1)_2 = 8 − 4 + 2 + 1 = 7</li>
     * <li>( 1  0  0 −1)_2 = 8         − 1 = 7</li>
     * </ul>
     * All are valid signed-digit representations of 7, but only the final representation, ( 1  0  0 −1)_2,
     * is in non-adjacent form.
     * <p>
     * The main benefit of NAF is that the Hamming weight of the value will be minimal. For regular binary
     * representations of values, 1/2 bits will be non-zero, on average, but with NAF this drops to only 1/3.
     * </p>
     * The properties of NAF make it useful in various algorithms, especially some in cryptography; e.g., for reducing
     * the number of multiplications needed for performing an exponentiation.
     *
     * @param value the value.
     * @return the NAF form of the value.
     */
    public static TIntArrayList naf(int value) {
        /*
         * There are several algorithms for obtaining the NAF representation of a value given in binary. One such is the
         * following method using repeated division; it works by choosing non-zero coefficients such that the resulting
         * quotient is divisible by 2 and hence the next coefficient a zero.
         * For more details, see https://en.wikipedia.org/wiki/Non-adjacent_form, Converting to NAF.
         */
        TIntArrayList res = new TIntArrayList();
        boolean sign = value < 0;
        value = Math.abs(value);
        // i ← 0, while E > 0 do
        for (int i = 0; value != 0; i++) {
            // if E is odd, then z_i ← 2 − (E mod 4); else, z_i = 0
            int zi = (value & 0x01) != 0 ? 2 - (value & 0x03) : 0;
            // E ← E - z_i; E ← E/2
            value = (value - zi) >>> 1;
            if (zi != 0) {
                // we only add non-zero NAF form with its base (1 << i)
                res.add((sign ? -zi : zi) * (1 << i));
            }
            // i ← i + 1
        }
        return res;
    }

    /**
     * Returns {@code true} if {@code n} is a prime number: an integer greater than one that cannot be factored into a
     * product of smaller positive integers.
     * <p>
     * Returns {@code false} if {@code n} is zero, one, or a composite number (one which can be factored into smaller
     * positive integers).
     *
     * <p>To test larger numbers, use {@link BigInteger#isProbablePrime}.
     *
     * @param n the value n.
     * @return true if n is a prime number.
     */
    public static boolean isPrime(long n) {
        return LongMath.isPrime(n);
    }

    /**
     * Try to find the smallest (as integer) primitive degree-th root of
     * unity modulo small prime modulus, where degree must be a power of two.
     *
     * @param degree
     * @param modulus
     * @param result
     * @return
     */
    public static boolean tryMinimalPrimitiveRoot(long degree, Modulus modulus, long[] result) {
        assert result.length == 1;


        if (!tryPrimitiveRoot(degree, modulus, result)) {
            return false;
        }
        long generatorSq = UintArithmeticSmallMod.multiplyUintMod(result[0], result[0], modulus);
        long currentGenerator = result[0];
        // destination is going to always contain the smallest generator found
        for (int i = 0; i < degree; i += 2) {
            if (currentGenerator < result[0]) {
                result[0] = currentGenerator;
            }
            currentGenerator = UintArithmeticSmallMod.multiplyUintMod(currentGenerator, generatorSq, modulus);
        }

        return true;
    }


    /**
     * @param degree  power of 2
     * @param modulus modulus
     * @param result  x^degree = 1 mod modulus, solve x, just the root is degree-th root of unity in integers modulo modulus
     * @return
     */
    public static boolean tryPrimitiveRoot(long degree, Modulus modulus, long[] result) {

        assert UintCore.getPowerOfTwo(degree) > 0;
        assert result.length == 1;
        // |Z_p^*| = p - 1
        // implies that modulus is a prime number ？
        // We need to divide modulus-1 by degree to get the size of the quotient group
        long sizeEntireGroup = modulus.getValue() - 1;

        // Compute size of quotient group
        // (p-1) / n
        long sizeQuotientGroup = sizeEntireGroup / degree;
        // size_entire_group must be divisible by degree, or otherwise the primitive root does not
        // exist in integers modulo modulus, (p-1) - (p-1/n) * n must be 0
        if (sizeEntireGroup - sizeQuotientGroup * degree != 0) {
            return false;
        }

        Random random = new Random();
        int attemptCounter = 0;
        int attemptCounterMax = 100;
        // 随机生成 g, 并计算 g^{p-1/n} ，并验证 g^{p-1/n} 是否为 模 p 下的 n 次单位根
        do {
            attemptCounter++;
            // Set destination to be a random number modulo modulus， random g
            result[0] = UintArithmeticSmallMod.barrettReduce64(random.nextLong(), modulus);
            // Raise the random number to power the size of the quotient
            // to get rid of irrelevant part, g^{p-1/n}
            result[0] = UintArithmeticSmallMod.exponentUintMod(result[0], sizeQuotientGroup, modulus);
        } while (!isPrimitiveRoot(result[0], degree, modulus) && (attemptCounter < attemptCounterMax));

        return isPrimitiveRoot(result[0], degree, modulus);
    }

    /**
     * if root is degree-th root of unity, then root^n = 1 mod p, then (root^{n/2}) = -1 mod p
     *
     * @param root
     * @param degree
     * @param modulus
     * @return Whether the root is degree-th root of unity in integers modulo modulus
     */
    public static boolean isPrimitiveRoot(long root, long degree, Modulus modulus) {

        assert root < modulus.getValue();
        // ensure degree is power of 2
        assert UintCore.getPowerOfTwo(degree) > 0;

        if (root == 0) {
            return false;
        }

        // We check if root is a degree-th root of unity in integers modulo modulus,
        // where degree is a power of two. It suffices to check that root^(degree/2)
        // is -1 modulo modulus.
        return UintArithmeticSmallMod.exponentUintMod(root, degree >>> 1, modulus) == modulus.getValue() - 1;
    }


    /**
     * Generate a vector of primes with "bit_size" bits that are congruent to 1 modulo "factor"
     *
     * @param factor
     * @param bitSize bit-size of prime value
     * @return A Modulus object with prime value
     */
    public static Modulus getPrime(long factor, int bitSize) {
//        return getPrimes(factor, bitSize, 1)[0];

        // [2, 61]
        assert bitSize <= Constants.MOD_BIT_COUNT_MAX && bitSize >= Constants.MOD_BIT_COUNT_MIN;

        // Start with (2^bit_size - 1) / (factor * factor)  +  1
        long value = ((1L << bitSize) - 1) / factor * factor + 1;
        // min value of bitSize-bit integer
        long lowerBound = 1L << (bitSize - 1);
        int i = 0;
        while (value > lowerBound) {
            Modulus mod = new Modulus(value);
            if (mod.isPrime()) {
                return mod;
            }
            value -= factor;
        }

        throw new IllegalArgumentException("failed to find enough qualifying primes, please check factor and bitSize");
    }


    /**
     * Generate a vector of primes with "bit_size" bits that are congruent to 1 modulo "factor"
     *
     * @param factor  factor
     * @param bitSize bit-size of prime value
     * @param count   number of Modulus
     * @return Modulus[] with length count, and every modulus's value is a prime number, and are congruent to 1 modulo "factor"
     */
    public static Modulus[] getPrimes(long factor, int bitSize, int count) {

        assert count > 0;
        // [2, 61]
        assert bitSize <= Constants.MOD_BIT_COUNT_MAX && bitSize >= Constants.MOD_BIT_COUNT_MIN;

        // Start with (2^bit_size - 1) / (factor * factor)  +  1
        long value = ((1L << bitSize) - 1) / factor * factor + 1;
        // min value of bitSize-bit integer
        long lowerBound = 1L << (bitSize - 1);
        int i = 0;
        Modulus[] modArray = new Modulus[count];
        while (count > 0 && value > lowerBound) {
            Modulus mod = new Modulus(value);
            if (mod.isPrime()) {
                modArray[i] = mod;
                i++;
                count--;
            }
            value -= factor;
        }
        if (count > 0) {
            throw new IllegalArgumentException("failed to find enough qualifying primes, please check factor and bitSize");
        }
        return modArray;
    }


    /**
     * @param x a number
     * @param y a number
     * @return gcd(x, y)
     */
    public static long gcd(long x, long y) {
        assert x != 0;
//        assert y != 0;
        if (x < y) {
            return gcd(y, x);
        } else if (y == 0) {
            return x;
        } else {
            long f = x % y;
            if (f == 0) {
                return y;
            } else {
                return gcd(y, f);
            }
        }
    }

    /**
     * @param x a value
     * @param y a value
     * @return if gcd(x, y) = 1, return true, else return false
     */
    public static boolean areCoPrime(long x, long y) {
        return !(gcd(x, y) > 1);
    }

    /**
     * Extended GCD
     *
     * @param x
     * @param y
     * @return (gcd ( x, y), a, b), satisfying gcd(x, y) = ax + by
     */
    public static long[] xgcd(long x, long y) {

        assert x != 0;
        assert y != 0;

        long prevA = 1;
        long a = 0;

        long prevB = 0;
        long b = 1;

        while (y != 0) {
            // need safe_cast?
            long q = x / y;
            long temp = x % y;
            x = y;
            y = temp;
            temp = a;
            a = Common.subSafe(prevA, Common.mulSafe(q, a, false), false);
            prevA = temp;

            temp = b;
            b = Common.subSafe(prevB, Common.mulSafe(q, b, false), false);
            prevB = temp;
        }
        return new long[]{x, prevA, prevB};
    }

    /**
     * Compute a^{-1} mod b using Extended Gcd, basic idea is that
     * gdc(a, b) = ax + by, if gcd(a, b) = 1, then 1 = ax + by ,  both sides mod b:
     * 1 mod b = ax mod b  , so a^{-1} mod b = x mod b
     *
     * @param value   input
     * @param modulus modulus
     * @param result  Array length is 1, store the value's inverse
     * @return value * value^{-1} = 1 mod modulus
     */
    public static boolean tryInvertUintMod(long value, long modulus, long[] result) {

        assert modulus > 1;
        assert result.length == 1;

        if (value == 0) {
            return false;
        }
        // 1 = ax + by , y is the modulus , both sides mod y
        // so 1 = ax , x^{-1} = a
        long[] gcdTuple = xgcd(value, modulus);
        if (gcdTuple[0] != 1) {
            return false;
        } else if (gcdTuple[1] < 0) {
            result[0] = gcdTuple[1] + modulus;
            return true;
        } else {
            result[0] = gcdTuple[1];
            return true;
        }

    }

}
