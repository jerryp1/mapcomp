package edu.alibaba.mpc4j.dp.stream.tool.bobhash;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * BobLongHash测试类。
 *
 * @author Weiran Liu
 * @date 2022/11/15
 */
public class TestBobLongHash {

    @Test(expected = AssertionError.class)
    public void testNegativePrimeIndex() {
        new BobLongHash(-1);
    }

    @Test(expected = AssertionError.class)
    public void testLargePrimeIndex() {
        new BobLongHash(BobHashUtils.PRIME_BIT_TABLE_SIZE);
    }

    @Test(expected = AssertionError.class)
    public void testEmptyData() {
        new BobLongHash().hash(new byte[0]);
    }

    @Test
    public void testHash() {
        int maxLength = 1 << CommonConstants.STATS_BYTE_LENGTH;
        Set<Long> hashSet = new HashSet<>(maxLength);
        BobLongHash bobLongHash = new BobLongHash();
        for (int length = 1; length <= maxLength; length++) {
            byte[] data = new byte[length];
            hashSet.add(bobLongHash.hash(data));
        }
        Assert.assertEquals(maxLength, hashSet.size());
    }

    @Test
    public void testConsistency() {
        SecureRandom secureRandom = new SecureRandom();
        int maxLength = 1 << CommonConstants.STATS_BYTE_LENGTH;
        BobLongHash bobLongHash = new BobLongHash();
        for (int length = 1; length <= maxLength; length++) {
            byte[] data = new byte[length];
            secureRandom.nextBytes(data);
            long hash1 = bobLongHash.hash(data);
            long hash2 = bobLongHash.hash(data);
            Assert.assertEquals(hash1, hash2);
        }
    }

    @Test
    public void testParallel() {
        int blockByteLength =CommonConstants.STATS_BYTE_LENGTH;
        int maxNum = 1 << blockByteLength;
        byte[] data = new byte[blockByteLength];
        BobLongHash bobLongHash = new BobLongHash();
        Set<Long> hashSet = IntStream.range(0, maxNum).parallel()
            .mapToLong(index -> bobLongHash.hash(data)).boxed()
            .collect(Collectors.toSet());
        Assert.assertEquals(1, hashSet.size());
    }
}
