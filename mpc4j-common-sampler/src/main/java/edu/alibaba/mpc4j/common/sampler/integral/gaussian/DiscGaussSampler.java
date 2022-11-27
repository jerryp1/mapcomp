package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import edu.alibaba.mpc4j.common.sampler.integral.IntegralSampler;

/**
 * The interface of Discrete Gaussian Sampling. The definition is shown in Definition 1 of the following paper:
 * <p>
 * Canonne, Clément L., Gautam Kamath, and Thomas Steinke. The discrete gaussian for differential privacy. Advances in
 * Neural Information Processing Systems 33 (2020): 15676-15688.
 * </p>
 * <p>
 * Let µ, σ ∈ R with σ > 0. The discrete Gaussian distribution with location µ and scale σ is denoted N_Z(µ, σ^2). It
 * is a probability distribution supported on the integers.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
public interface DiscGaussSampler extends IntegralSampler {
    /**
     * Get the type of Discrete Gaussian sampler.
     *
     * @return the type of Discrete Gaussian sampler.
     */
    DiscGaussSamplerFactory.DiscGaussSamplerType getType();

    /**
     * Get the mean of the distribution c.
     *
     * @return the mean of the distribution c.
     */
    int getC();

    /**
     * Get the width parameter σ.
     *
     * @return the width parameter σ.
     */
    double getSigma();

    /**
     * Get the mean of the distribution. In Discrete Gaussian sampling, it is often denoted as c.
     *
     * @return the mean of the distribution.
     */
    @Override
    default double getMean() {
        return getC();
    }

    /**
     * Get the variance of the distribution. In Discrete Gaussian sampling, it is often denoted as σ.
     *
     * @return the variance of the distribution.
     */
    @Override
    default double getVariance() {
        return getSigma() * getSigma();
    }
}
