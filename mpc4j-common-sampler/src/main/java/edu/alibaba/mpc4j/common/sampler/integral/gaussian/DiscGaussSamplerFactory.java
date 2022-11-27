package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import java.security.SecureRandom;
import java.util.Random;

/**
 * The factory for Discrete Gaussian Sampling.
 *
 * @author Weiran Liu
 * @date 2022/11/25
 */
public class DiscGaussSamplerFactory {
    /**
     * default τ
     */
    static final int DEFAULT_TAU = 6;

    private DiscGaussSamplerFactory() {
        // empty
    }

    public enum DiscGaussSamplerType {
        /**
         * Uniform Discrete Gaussian Sampler, where $\exp(-(x-c)²/(2σ²))$ is precomputed and stored in a table.
         */
        UNIFORM_TABLE,
        /**
         * Uniform Discrete Gaussian Sampler, where $\exp(-(x-c)²/(2σ²))$ is computed using logarithmically
         * many calls to Bernoulli distributions.
         */
        UNIFORM_LOG_TABLE,
        /**
         * Uniform Discrete Gaussian Sampler, where $\exp(-(x-c)²/(2σ²))$ is computed in each invocation.
         */
        UNIFORM_ONLINE,
        /**
         * σ_2 Discrete Gaussian Sampler, where σ = k · σ_2, and σ_2 = \sqrt{1 / (2 \log 2)}, and
         * $\exp(-(x - c)²/(2σ²))$ is computed using logarithmically many calls to Bernoulli distributions
         * (but no calls to $\exp$).
         */
        SIGMA2_LOG_TABLE,
        /**
         * σ_2 Discrete Gaussian Sampler with a cut-off parameter τ.
         */
        SIGMA2_LOG_TABLE_TAU,
        /**
         * Discrete Gaussian Sampler with Alias method.
         */
        ALIAS,
        /**
         * Applies the convolution technique to alias sampling.
         */
        CONVOLUTION,
        /**
         * The Discrete Gaussian sampling proposed by Canonne, Kamath and Steinke.
         */
        CKS20,
        /**
         * The Discrete Gaussian sampling with a cut-off parameter τ, proposed by Canonne, Kamath and Steinke.
         */
        CKS20_TAU,
    }

    /**
     * Create an instance of Discrete Gaussian sampler.
     *
     * @param type  the type.
     * @param c     the mean of the distribution c.
     * @param sigma the width parameter σ.
     * @return an instance of Discrete Gaussian sampler.
     */
    public static DiscGaussSampler createInstance(DiscGaussSamplerType type, int c, double sigma) {
        return createInstance(type, new SecureRandom(), c, sigma);
    }

    /**
     * Create an instance of Discrete Gaussian sampler.
     *
     * @param type   the type.
     * @param random the random state.
     * @param c      the mean of the distribution c.
     * @param sigma  the width parameter σ.
     * @return an instance of Discrete Gaussian sampler.
     */
    public static DiscGaussSampler createInstance(DiscGaussSamplerType type, Random random, int c, double sigma) {
        switch (type) {
            case CKS20:
                return new Cks20DiscGaussSampler(random, c, sigma);
            case SIGMA2_LOG_TABLE:
                return new Sigma2LogTableDiscGaussSampler(random, c, sigma);
            default:
                return createTauInstance(type, random, c, sigma, DEFAULT_TAU);
        }
    }

    /**
     * Create an instance of Discrete Gaussian sampler with a cut-off parameter τ.
     *
     * @param type  the type.
     * @param c     the mean of the distribution c.
     * @param sigma the width parameter σ.
     * @param tau   the cut-off parameter τ.
     * @return an instance of Discrete Gaussian sampler with a cut-off parameter τ.
     */
    public static TauDiscGaussSampler createTauInstance(DiscGaussSamplerType type, int c, double sigma, int tau) {
        return createTauInstance(type, new SecureRandom(), c, sigma, tau);
    }

    /**
     * Create an instance of Discrete Gaussian sampler with a cut-off parameter τ.
     *
     * @param type   the type.
     * @param random the random state.
     * @param c      the mean of the distribution c.
     * @param sigma  the width parameter σ.
     * @param tau    the cut-off parameter τ.
     * @return an instance of Discrete Gaussian sampler with a cut-off parameter τ.
     */
    public static TauDiscGaussSampler createTauInstance(DiscGaussSamplerType type, Random random, int c, double sigma, int tau) {
        switch (type) {
            case CKS20_TAU:
                return new Cks20TauDiscGaussSampler(random, c, sigma, tau);
            case UNIFORM_TABLE:
                return new UniTableTauDiscGaussSampler(random, c, sigma, tau);
            case UNIFORM_ONLINE:
                return new UniOnlineTauDiscGaussSampler(random, c, sigma, tau);
            case UNIFORM_LOG_TABLE:
                return new UniLogTableTauDiscGaussSampler(random, c, sigma, tau);
            case SIGMA2_LOG_TABLE_TAU:
                return new Sigma2LogTableTauDiscGaussSampler(random, c, sigma, tau);
            case ALIAS:
            case CONVOLUTION:
            default:
                throw new IllegalArgumentException("Invalid " + DiscGaussSamplerType.class.getSimpleName() + ": " + type.name());
        }
    }
}
