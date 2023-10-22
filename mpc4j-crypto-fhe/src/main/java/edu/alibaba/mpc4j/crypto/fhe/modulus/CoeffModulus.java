package edu.alibaba.mpc4j.crypto.fhe.modulus;

import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;
import edu.alibaba.mpc4j.crypto.fhe.utils.GlobalVariables;
import edu.alibaba.mpc4j.crypto.fhe.utils.HeStdParms;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;

import java.util.*;

/**
 * This class contains static methods for creating a coefficient modulus easily.
 * Note that while these functions take a sec_level_type argument, all security
 * guarantees are lost if the output is used with encryption parameters with
 * a mismatching value for the poly_modulus_degree.
 *
 * The default value sec_level_type::tc128 provides a very high level of security
 * and is the default security level enforced by Microsoft SEAL when constructing
 * a SEALContext object. Normal users should not have to specify the security
 * level explicitly anywhere.
 *
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/modulus.h#L424
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/27
 */
public class CoeffModulus {

    public enum SecurityLevelType {
        // No security level specified.
        NONE,
        // 128-bit security level according to HomomorphicEncryption.org standard.
        TC128,
        // 192-bit security level according to HomomorphicEncryption.org standard.
        TC192,
        // 256-bit security level according to HomomorphicEncryption.org standard.
        TC256,
    }


    public static int maxBitCount(int polyModulusDegree, SecurityLevelType securityLevel) {

        switch (securityLevel) {

            case TC128:
                return HeStdParms.heStdParms128Tc(polyModulusDegree);
            case TC192:
                return HeStdParms.heStdParms192Tc(polyModulusDegree);
            case TC256:
                return HeStdParms.heStdParms256Tc(polyModulusDegree);
            case NONE:
                return Integer.MAX_VALUE;
            default:
                return 0;
        }
    }

    /**
     * SecurityLevelType default is TC128
     *
     * @param polyModulusDegree N
     * @return max bit-count of coeffModulus in given polyModulusDegree
     */
    public static int maxBitCount(int polyModulusDegree) {

        return HeStdParms.heStdParms128Tc(polyModulusDegree);
    }

    /**
     * @param polyModulusDegree N
     * @return
     */
    public static Modulus[] BfvDefault(int polyModulusDegree) {

        if (maxBitCount(polyModulusDegree) == 0) {
            throw new IllegalArgumentException("non-standard poly_modulus_degree");
        }

        return GlobalVariables.DEFAULT_COEFF_MUDULUS_128.get(polyModulusDegree);

    }

    public static Modulus[] BfvDefault(int polyModulusDegree, SecurityLevelType securityLevel) {

        if (maxBitCount(polyModulusDegree) == 0) {
            throw new IllegalArgumentException("non-standard poly_modulus_degree");
        }
        if (securityLevel == SecurityLevelType.NONE) {
            throw new IllegalArgumentException("invalid security level");
        }
        switch (securityLevel) {
            case TC128:
                return GlobalVariables.DEFAULT_COEFF_MUDULUS_128.get(polyModulusDegree);
            case TC192:
                return GlobalVariables.DEFAULT_COEFF_MUDULUS_192.get(polyModulusDegree);
            case TC256:
                return GlobalVariables.DEFAULT_COEFF_MUDULUS_256.get(polyModulusDegree);
            default:
                throw new IllegalArgumentException("invalid security level");

        }
    }


    public static Modulus create(int polyModulusDegree, int bitSize) {

        if (polyModulusDegree > Constants.POLY_MOD_DEGREE_MAX || polyModulusDegree < Constants.POLY_MOD_DEGREE_MIN
                || UintCore.getPowerOfTwo(polyModulusDegree) < 0
        ) {
            throw new IllegalArgumentException("polyModulusDegree is invalid");
        }

        if (bitSize < Constants.USER_MOD_BIT_COUNT_MIN
                || bitSize > Constants.USER_MOD_BIT_COUNT_MAX) {
            throw new IllegalArgumentException("bitSize is invalid");
        }

        // todo: why mul safe?
        long factor = Common.mulSafe(2L, (long) polyModulusDegree, true);
        // 直接 return
        return Numth.getPrime(factor, bitSize);
    }


    /**
     * 为什么 seal 的实现如此麻烦？不能直接遍历，然后生成？为什么非得搞 map?
     * Returns a custom coefficient modulus suitable for use with the specified
     * poly_modulus_degree. The return value will be an array consisting of
     * Modulus elements representing distinct prime numbers such that:
     * 1) have bit-lengths as given in the bit_sizes parameter (at most 60 bits) and
     * 2) are congruent to 1 modulo 2*poly_modulus_degree.
     *
     * @param polyModulusDegree The value of the polyModulusDegree
     *                          encryption parameter
     * @param bitSizes          The bit-lengths of the primes to be generated
     * @return an array consisting of Modulus elements
     */
    public static Modulus[] create(int polyModulusDegree, int[] bitSizes) {

        if (polyModulusDegree > Constants.POLY_MOD_DEGREE_MAX || polyModulusDegree < Constants.POLY_MOD_DEGREE_MIN
                || UintCore.getPowerOfTwo(polyModulusDegree) < 0
        ) {
            throw new IllegalArgumentException("polyModulusDegree is invalid");
        }

        if (bitSizes.length > Constants.COEFF_MOD_COUNT_MAX) {
            throw new IllegalArgumentException("bitSizes is invalid");
        }

        if (Arrays.stream(bitSizes).min().getAsInt() < Constants.USER_MOD_BIT_COUNT_MIN
                || Arrays.stream(bitSizes).max().getAsInt() > Constants.USER_MOD_BIT_COUNT_MAX) {
            throw new IllegalArgumentException("bitSizes is invalid");
        }

        // because bitSizes may have some same elements
        // size --> count
        Map<Integer, Integer> countTables = new HashMap<>();
        for (int size : bitSizes) {
            if (!countTables.containsKey(size)) {
                countTables.put(size, 1);
                continue;
            }
            countTables.put(size, countTables.get(size) + 1);
        }
        // todo: why mul safe?
        long factor = Common.mulSafe(2L, (long) polyModulusDegree, true);

        Map<Integer, ArrayList<Modulus>> primeTable = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : countTables.entrySet()) {
            primeTable.put(entry.getKey(), new ArrayList(Arrays.asList(Numth.getPrimes(factor, entry.getKey(), entry.getValue()))));
        }
        // result
        Modulus[] result = new Modulus[bitSizes.length];
        int i = 0;
        for (int size : bitSizes) {
            result[i] = primeTable.get(size).remove(primeTable.get(size).size() - 1);
            i++;
        }
        return result;
    }

    public static Modulus[] create(int polyModulusDegree, Modulus plainModulus, int[] bitSizes) {

        if (polyModulusDegree > Constants.POLY_MOD_DEGREE_MAX || polyModulusDegree < Constants.POLY_MOD_DEGREE_MIN
                || UintCore.getPowerOfTwo(polyModulusDegree) < 0
        ) {
            throw new IllegalArgumentException("poly_modulus_degree is invalid");
        }

        if (bitSizes.length > Constants.COEFF_MOD_COUNT_MAX) {
            throw new IllegalArgumentException("bit_sizes is invalid");
        }

        if (Arrays.stream(bitSizes).min().getAsInt() < Constants.USER_MOD_BIT_COUNT_MIN
                || Arrays.stream(bitSizes).max().getAsInt() > Constants.USER_MOD_BIT_COUNT_MAX) {
            throw new IllegalArgumentException("bit_sizes is invalid");
        }
        // because bitSizes may have some same size
        // size --> count
        Map<Integer, Integer> countTables = new HashMap<>();
        for (int size : bitSizes) {
            if (!countTables.containsKey(size)) {
                countTables.put(size, 1);
                continue;
            }
            countTables.put(size, countTables.get(size) + 1);
        }
        // why mul safe?
        long factor = Common.mulSafe(2L, (long) polyModulusDegree, true);
        // what the meaning ?
        // 2N * (p/gcd(p, 2N)) = 2N?
        factor = Common.mulSafe(factor, plainModulus.getValue() / Numth.gcd(plainModulus.getValue(), factor), true);

        Map<Integer, ArrayList<Modulus>> primeTable = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : countTables.entrySet()) {
            primeTable.put(entry.getKey(), new ArrayList(Arrays.asList(Numth.getPrimes(factor, entry.getKey(), entry.getValue()))));
        }
        // result
        Modulus[] result = new Modulus[bitSizes.length];
        int i = 0;
        for (int size : bitSizes) {
            result[i] = primeTable.get(size).remove(primeTable.get(size).size() - 1);
            i++;
        }
        return result;
    }

}
