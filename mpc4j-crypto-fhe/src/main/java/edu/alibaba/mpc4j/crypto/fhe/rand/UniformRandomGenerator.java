package edu.alibaba.mpc4j.crypto.fhe.rand;

import edu.alibaba.mpc4j.crypto.fhe.zq.Common;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/randomgen.h#L313
 * </p>
 *
 * @author Qixian Zhou, Weiran Liu
 * @date 2023/9/2
 */
public class UniformRandomGenerator {
    /**
     * the random state
     */
    public SecureRandom secureRandom;
    /**
     * seed
     */
    private long[] seed;

    /**
     * Creates a uniform random generator.
     */
    public UniformRandomGenerator() {
        secureRandom = new SecureRandom();
    }

    /**
     * Creates a uniform random generator.
     *
     * @param seed the seed.
     */
    public UniformRandomGenerator(long[] seed) {
        assert seed.length == UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT;
        try {
            // only SHA1PRNG support random generation with seed
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
            // we cannot directly use a for loop to set seed, otherwise the generated randomness is not fixed.
            byte[] byteSeed = Common.uint64ArrayToByteArray(seed, seed.length);
            secureRandom.setSeed(byteSeed);
            this.seed = new long[UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT];
            System.arraycopy(seed, 0, this.seed, 0, UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets a copy of the seed.
     *
     * @return a copy of the seed.
     */
    public long[] seed() {
        return Arrays.copyOf(seed, seed.length);
    }

    /**
     * Generates a random integer value in range [0, Integer.MAX_VALUE).
     *
     * @return a random integer value in range [0, Integer.MAX_VALUE).
     */
    public int generate() {
        return secureRandom.nextInt(Integer.MAX_VALUE);
    }

    /**
     * Generates randomness into the destination.
     *
     * @param destination the destination.
     */
    public void generate(byte[] destination) {
        secureRandom.nextBytes(destination);
    }

    /**
     * Generates randomness into the destination.
     *
     * @param destination the destination.
     */
    public void generate(long[] destination) {
        for (int i = 0; i < destination.length; i++) {
            destination[i] = secureRandom.nextLong();
        }
    }

    /**
     * Generates randomness with the assigned length into the destination.
     *
     * @param byteCount the randomness length in byte.
     * @param destination the destination.
     * @param startIndex the start index in the destination.
     */
    public void generate(int byteCount, long[] destination, int startIndex) {
        assert byteCount % Common.BYTES_PER_UINT64 == 0;
        int longCount = byteCount / Common.BYTES_PER_UINT64;
        for (int i = startIndex; i < startIndex + longCount; i++) {
            destination[i] = secureRandom.nextLong();
        }
    }


}
