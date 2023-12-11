package edu.alibaba.mpc4j.crypto.fhe.rand;

import java.util.Arrays;

/**
 * Uniform random generator factory.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/randomgen.h#L411
 * </p>
 *
 * @author Qixian Zhou, Weiran Liu
 * @date 2023/9/19
 */
public class UniformRandomGeneratorFactory {
    /**
     * send length
     */
    public static final int PRNG_SEED_UINT64_COUNT = 8;
    /**
     * use random seed
     */
    private final boolean useRandomSeed;
    /**
     * default seed
     */
    private long[] defaultSeed;

    public UniformRandomGeneratorFactory() {
        useRandomSeed = true;
    }

    public UniformRandomGeneratorFactory(long[] defaultSeed) {
        assert defaultSeed.length == UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT;
        this.defaultSeed = new long[defaultSeed.length];
        System.arraycopy(defaultSeed, 0, this.defaultSeed, 0, defaultSeed.length);
        useRandomSeed = false;
    }

    public static UniformRandomGeneratorFactory defaultFactory() {
        return new UniformRandomGeneratorFactory();
    }

    /**
     * Returns if the factory use a random seed.
     *
     * @return true if the factory use a random seed.
     */
    public boolean useRandomSeed() {
        return useRandomSeed;
    }

    /**
     * Gets a copy of the default seed.
     *
     * @return a copy of the default seed.
     */
    public long[] defaultSeed() {
        if (defaultSeed == null) {
            return null;
        }
        return Arrays.copyOf(defaultSeed, defaultSeed.length);
    }

    /**
     * Creates a uniform random generator.
     *
     * @return a uniform random generator.
     */
    public UniformRandomGenerator create() {
        return useRandomSeed ? new UniformRandomGenerator() : new UniformRandomGenerator(defaultSeed);
    }

    public UniformRandomGenerator create(long[] seed) {
        return new UniformRandomGenerator(seed);
    }
}
