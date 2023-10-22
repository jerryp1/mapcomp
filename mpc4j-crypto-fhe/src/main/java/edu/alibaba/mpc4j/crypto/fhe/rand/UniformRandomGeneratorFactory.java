package edu.alibaba.mpc4j.crypto.fhe.rand;

import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;

import java.security.SecureRandom;

/**
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/randomgen.h#L411
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/9/19
 */
public class UniformRandomGeneratorFactory {

    public static final int PRNG_SEED_UINT64_COUNT = 8;

    public static final int PRNG_SEED_BYTE_COUNT = PRNG_SEED_UINT64_COUNT * Constants.BYTES_PER_UINT64;

    private long[] defaultSeed;

    private boolean useRandomSeed;

    public UniformRandomGeneratorFactory() {
        useRandomSeed = true;
    }

    public UniformRandomGeneratorFactory(long[] defaultSeed) {
        this.defaultSeed = new long[defaultSeed.length];
        System.arraycopy(defaultSeed, 0, this.defaultSeed, 0, defaultSeed.length);
        useRandomSeed = true;
    }

    public static UniformRandomGeneratorFactory defaultFactory() {
        return new UniformRandomGeneratorFactory();
    }


    public UniformRandomGenerator create() {

        return useRandomSeed ? createImpl() : createImpl(defaultSeed);
    }

    public UniformRandomGenerator create(long[] seed) {
        return createImpl(seed);
    }

    public UniformRandomGenerator createImpl(long[] seed) {
        return new UniformRandomGenerator(seed);
    }

    public UniformRandomGenerator createImpl() {
        return new UniformRandomGenerator();
    }


}
