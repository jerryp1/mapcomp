package edu.alibaba.mpc4j.crypto.fhe.rand;

import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * UniformRandomGenerator test.
 *
 * @author Weiran Liu
 * @date 2023/11/29
 */
public class UniformRandomGeneratorTest {

    @Test
    public void testRandomness() {
        UniformRandomGeneratorFactory factory = UniformRandomGeneratorFactory.defaultFactory();
        Assert.assertTrue(factory.useRandomSeed());
        UniformRandomGenerator randomGenerator = factory.create();
        boolean lowerHalf = false;
        boolean upperHalf = false;
        boolean even = false;
        boolean odd = false;
        // generate 20 random values and see if there is at least one lower_half, one upper_half, one even and one odd
        for (int i = 0; i < 20; i++) {
            int value = randomGenerator.generate();
            if (value < Integer.MAX_VALUE / 2) {
                lowerHalf = true;
            } else {
                upperHalf = true;
            }
            if ((value % 2) == 0) {
                even = true;
            } else {
                odd = true;
            }
        }
        Assert.assertTrue(lowerHalf);
        Assert.assertTrue(upperHalf);
        Assert.assertTrue(even);
        Assert.assertTrue(odd);
    }

    @Test
    public void testSeedFactory() {
        UniformRandomGeneratorFactory factory = UniformRandomGeneratorFactory.defaultFactory();
        Assert.assertTrue(factory.useRandomSeed());
        Assert.assertNull(factory.defaultSeed());

        factory = new UniformRandomGeneratorFactory(new long[] {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L});
        Assert.assertFalse(factory.useRandomSeed());
        Assert.assertArrayEquals(new long[] {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L}, factory.defaultSeed());
    }

    @Test
    public void testSeedGenerator() {
        int size = 20;
        // create from default factory
        UniformRandomGenerator generator1 = UniformRandomGeneratorFactory.defaultFactory().create();
        int[] values1 = IntStream.range(0, size).map(index -> generator1.generate()).toArray();
        UniformRandomGenerator generator2 = UniformRandomGeneratorFactory.defaultFactory()
            .create(new long[Common.BYTES_PER_UINT64]);
        int[] values2 = IntStream.range(0, size).map(index -> generator2.generate()).toArray();
        UniformRandomGenerator generator3 = UniformRandomGeneratorFactory.defaultFactory()
            .create(new long[Common.BYTES_PER_UINT64]);
        int[] values3 = IntStream.range(0, size).map(index -> generator3.generate()).toArray();
        for (int i = 0; i < size; i++) {
            Assert.assertNotEquals(values1[i], values2[i]);
            Assert.assertEquals(values2[i], values3[i]);
        }
        // create from seed factory
        UniformRandomGenerator generator4 = UniformRandomGeneratorFactory.defaultFactory().create();
        values1 = IntStream.range(0, size).map(index -> generator4.generate()).toArray();
        UniformRandomGenerator generator5 = new UniformRandomGeneratorFactory(new long[Common.BYTES_PER_UINT64]).create();
        values2 = IntStream.range(0, size).map(index -> generator5.generate()).toArray();
        UniformRandomGenerator generator6 = new UniformRandomGeneratorFactory(new long[Common.BYTES_PER_UINT64]).create();
        values3 = IntStream.range(0, size).map(index -> generator6.generate()).toArray();
        for (int i = 0; i < size; i++) {
            Assert.assertNotEquals(values1[i], values2[i]);
            Assert.assertEquals(values2[i], values3[i]);
        }
    }
}
