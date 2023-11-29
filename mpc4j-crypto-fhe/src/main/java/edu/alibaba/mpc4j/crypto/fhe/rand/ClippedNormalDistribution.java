package edu.alibaba.mpc4j.crypto.fhe.rand;

/**
 * Clipped normal distribution.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/clipnormal.h
 * </p>
 *
 * @author Qixian Zhou, Weiran Liu
 * @date 2023/9/20
 */
public class ClippedNormalDistribution {
    /**
     * mean μ
     */
    private final double mean;
    /**
     * standard deviation σ
     */
    private final double standardDeviation;
    /**
     * max deviation for clipping
     */
    private final double maxDeviation;

    public ClippedNormalDistribution(double mean, double standardDeviation, double maxDeviation) {
        if (standardDeviation < 0) {
            throw new IllegalArgumentException("standardDeviation must be >= 0");
        }
        if (maxDeviation < 0) {
            throw new IllegalArgumentException("maxDeviation must be >= 0");
        }
        this.mean = mean;
        this.standardDeviation = standardDeviation;
        this.maxDeviation = maxDeviation;
    }

    /**
     * Samples a randomness in clipped normal distribution.
     *
     * @param engine uniform random generator engine.
     * @return a randomness in clipped normal distribution.
     */
    public double sample(UniformRandomGenerator engine) {
        while (true) {
            double value = engine.secureRandom.nextGaussian() * standardDeviation + mean;
            double deviation = Math.abs(value - mean);
            if (deviation <= maxDeviation) {
                return value;
            }
        }
    }
}
