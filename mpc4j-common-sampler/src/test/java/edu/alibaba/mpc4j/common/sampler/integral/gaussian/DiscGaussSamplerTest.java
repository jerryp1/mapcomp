package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.sampler.integral.gaussian.DiscGaussSamplerFactory.DiscGaussSamplerType;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Tests for Discrete Gaussian sampler.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
@RunWith(Parameterized.class)
public class DiscGaussSamplerTest {
    /**
     * number of trials
     */
    private static final int N_TRIALS = 100000;
    /**
     * α
     */
    private static final int[] SIGMA_ARRAY = new int[]{1, 2, 4};
    /**
     * c
     */
    private static final int[] C_ARRAY = new int[]{0, 5, -5};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();

        // UNIFORM_ONLINE
        configurationParams.add(new Object[]{
            DiscGaussSamplerType.UNIFORM_ONLINE.name(), DiscGaussSamplerType.UNIFORM_ONLINE,
        });
        // UNIFORM_TABLE
        configurationParams.add(new Object[]{
            DiscGaussSamplerType.UNIFORM_TABLE.name(), DiscGaussSamplerType.UNIFORM_TABLE,
        });
        // CKS20_TAU
        configurationParams.add(new Object[]{
            DiscGaussSamplerType.CKS20_TAU.name(), DiscGaussSamplerType.CKS20_TAU,
        });
        // CKS20
        configurationParams.add(new Object[]{
            DiscGaussSamplerType.CKS20.name(), DiscGaussSamplerType.CKS20,
        });

        return configurationParams;
    }

    /**
     * the discrete gaussian sampler type
     */
    private final DiscGaussSamplerType type;

    public DiscGaussSamplerTest(String name, DiscGaussSamplerType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        DiscGaussSampler sampler = DiscGaussSamplerFactory.createInstance(type, 0, 1);
        Assert.assertEquals(type, sampler.getType());
    }

    @Test
    public void testSample() {
        DiscGaussSampler sampler = DiscGaussSamplerFactory.createInstance(type, 0, 1);
        int[] round1Samples = IntStream.range(0, N_TRIALS)
            .map(index -> sampler.sample())
            .toArray();
        int[] round2Samples = IntStream.range(0, N_TRIALS)
            .map(index -> sampler.sample())
            .toArray();
        // different sample results
        boolean allEqual = true;
        for (int i = 0; i < N_TRIALS; i++) {
            if (round1Samples[i] != round2Samples[i]) {
                allEqual = false;
                break;
            }
        }
        Assert.assertFalse(allEqual);
    }

    @Test
    public void testC() {
        for (int c : C_ARRAY) {
            testC(c);
        }
    }

    private void testC(int c) {
        DiscGaussSampler sampler = DiscGaussSamplerFactory.createInstance(type, c, 1);
        Assert.assertEquals(c, sampler.getC());
        int[] samples = IntStream.range(0, N_TRIALS)
            .map(index -> sampler.sample())
            .toArray();
        double mean = Arrays.stream(samples).average().orElse(0);
        Assert.assertEquals(sampler.getMean(), mean, 1.0);
    }

    @Test
    public void testSigma() {
        for (double sigma : SIGMA_ARRAY) {
            testSigma(sigma);
        }
    }

    public void testSigma(double sigma) {
        DiscGaussSampler sampler = DiscGaussSamplerFactory.createInstance(type, 0, sigma);
        Assert.assertEquals(sigma, sampler.getSigma(), DoubleUtils.PRECISION);
        int[] samples = IntStream.range(0, N_TRIALS)
            .map(index -> sampler.sample())
            .toArray();
        double variance = Arrays.stream(samples)
            .mapToDouble(sample -> Math.pow(sample, 2))
            .sum() / N_TRIALS;
        Assert.assertEquals(sampler.getVariance(), variance, sampler.getVariance() * 0.3);
    }

    @Test
    public void testReseed() {
        DiscGaussSampler sampler = DiscGaussSamplerFactory.createInstance(type, new Random(), 0, 1);
        try {
            sampler.reseed(0L);
            int[] round1Samples = IntStream.range(0, N_TRIALS)
                .map(index -> sampler.sample())
                .toArray();
            sampler.reseed(0L);
            int[] round2Samples = IntStream.range(0, N_TRIALS)
                .map(index -> sampler.sample())
                .toArray();
            Assert.assertArrayEquals(round1Samples, round2Samples);
        } catch (UnsupportedOperationException ignored) {

        }
    }

}
