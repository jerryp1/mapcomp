package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.integral.gaussian.DiscGaussSamplerFactory.DiscGaussSamplerType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Tests for Discrete Gaussian sampler with a cut-off parameter τ.
 *
 * @author Weiran Liu
 * @date 2022/11/25
 */
@RunWith(Parameterized.class)
public class TauDiscGaussSamplerTest {
    /**
     * number of trials
     */
    private static final int N_TRIALS = 1 << 18;
    /**
     * tolerance
     */
    private static final double TOLERANCE = 0.1;
    /**
     * bound
     */
    private static final int BOUND = 2;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // SIGMA2_LOG_TABLE_TAU
        configurations.add(new Object[]{
            DiscGaussSamplerType.SIGMA2_LOG_TABLE_TAU.name(), DiscGaussSamplerType.SIGMA2_LOG_TABLE_TAU,
        });
        // UNIFORM_LOG_TABLE
        configurations.add(new Object[]{
            DiscGaussSamplerType.UNIFORM_LOG_TABLE.name(), DiscGaussSamplerType.UNIFORM_LOG_TABLE,
        });
        // UNIFORM_ONLINE
        configurations.add(new Object[]{
            DiscGaussSamplerType.UNIFORM_ONLINE.name(), DiscGaussSamplerType.UNIFORM_ONLINE,
        });
        // UNIFORM_TABLE
        configurations.add(new Object[]{
            DiscGaussSamplerType.UNIFORM_TABLE.name(), DiscGaussSamplerType.UNIFORM_TABLE,
        });
        // CKS20_TAU
        configurations.add(new Object[]{
            DiscGaussSamplerType.CKS20_TAU.name(), DiscGaussSamplerType.CKS20_TAU,
        });

        return configurations;
    }

    /**
     * the discrete gaussian sampler type
     */
    private final DiscGaussSamplerType type;

    public TauDiscGaussSamplerTest(String name, DiscGaussSamplerType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testRatios() {
        // test proportional probabilities
        testRatios(3.0, 6);
        testRatios(2.0, 6);
        testRatios(4.0, 3);
        testRatios(15.4, 3);
        if (type.equals(DiscGaussSamplerType.CONVOLUTION)) {
            // we need some large sigma test cases here, because everything else would just be retesting alias
            testRatios(150, 6);
            testRatios(1500, 6);
            testRatios(1 << 27, 6);
        }
    }

    private void testRatios(double sigma, int tau) {
        TauDiscGaussSampler sampler = DiscGaussSamplerFactory.createTauInstance(type, 0, sigma, tau);

        // counts number of samples in [-Bound, BOUND]
        double[] counts = new double[2 * BOUND + 1];
        for (int i = 0; i < N_TRIALS; i++) {
            int r = sampler.sample();
            if (Math.abs(r) <= BOUND) {
                counts[r + BOUND] += 1;
            }
        }
        // calculate ratios for each count pairs
        for (int i = -BOUND; i <= BOUND; i++) {
            double left = counts[BOUND + 1] / counts[BOUND + i];
            double right = rho(0, sigma) / rho(i, sigma);
            Assert.assertTrue(Math.abs(Math.log(left / right)) < TOLERANCE * 4);
        }
    }

    private static double rho(double x, double sigma) {
        return Math.exp(-(x * x) / (2 * sigma * sigma));
    }

    @Test
    public void testUniformBoundaries() {
        // test [⌊c⌋ - ⌈στ⌉, ..., ⌊c⌋ + ⌈στ⌉] boundaries
        // Testing boundaries for convolution does not work in the same way
        // since the boundary constraints are imposed on the base sampler, but the convolution can exceed them
        if (!type.equals(DiscGaussSamplerType.CONVOLUTION)) {
            testUniformBoundaries(0, 3.0, 2);
            testUniformBoundaries(0, 10.0, 2);
            testUniformBoundaries(1, 3.3, 1);
            testUniformBoundaries(2, 2.0, 2);
        }
    }

    private void testUniformBoundaries(int c, double sigma, int tau) {
        TauDiscGaussSampler sampler = DiscGaussSamplerFactory.createTauInstance(type, c, sigma, tau);
        // [c - τ * σ, c + τ * σ]
        int lowerBound = (sampler.getC()) - (int) Math.ceil(sampler.getActualSigma() * sampler.getTau());
        int upperBound = (sampler.getC()) + (int) Math.ceil(sampler.getActualSigma() * sampler.getTau());

        for (int i = 0; i < N_TRIALS; i++) {
            int r = sampler.sample();
            if (r < lowerBound || r > upperBound) {
                System.out.println();
            }
            Assert.assertTrue(r >= lowerBound && r <= upperBound);
        }
    }

    @Test
    public void testMean() {
        testMean(0, 3.0, 6);
        testMean(0, 10.0, 6);
        testMean(1, 3.3, 6);
        testMean(2, 2.0, 6);

        testMean(0, 3.0, 3);
        testMean(0, 10.0, 3);
        testMean(1, 3.3, 3);
        testMean(2, 2.0, 3);
    }

    private void testMean(int c, double sigma, int tau) {
        TauDiscGaussSampler sampler = DiscGaussSamplerFactory.createTauInstance(type, c, sigma, tau);
        double mean = 0.0;
        for (int i = 0; i < N_TRIALS; i++) {
            mean += sampler.sample();
        }
        mean /= N_TRIALS;
        Assert.assertTrue(Math.abs(mean - c) <= TOLERANCE);
    }
}
