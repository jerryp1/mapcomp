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
 * BobIntHash测试类。
 *
 * @author Weiran Liu
 * @date 2022/11/15
 */
public class TestBobIntHash {

    @Test(expected = AssertionError.class)
    public void testNegativePrimeIndex() {
        new BobIntHash(-1);
    }

    @Test(expected = AssertionError.class)
    public void testLargePrimeIndex() {
        new BobIntHash(BobHashUtils.PRIME_BIT_TABLE_SIZE);
    }

    @Test(expected = AssertionError.class)
    public void testEmptyData() {
        new BobIntHash().hash(new byte[0]);
    }

    @Test
    public void testHash() {
        int maxLength = 1 << CommonConstants.STATS_BYTE_LENGTH;
        Set<Integer> hashSet = new HashSet<>(maxLength);
        BobIntHash bobIntHash = new BobIntHash();
        for (int length = 1; length <= maxLength; length++) {
            byte[] data = new byte[length];
            hashSet.add(bobIntHash.hash(data));
        }
        Assert.assertEquals(maxLength, hashSet.size());
    }

    @Test
    public void testConsistency() {
        SecureRandom secureRandom = new SecureRandom();
        int maxLength = 1 << CommonConstants.STATS_BYTE_LENGTH;
        BobIntHash bobIntHash = new BobIntHash();
        for (int length = 1; length <= maxLength; length++) {
            byte[] data = new byte[length];
            secureRandom.nextBytes(data);
            int hash1 = bobIntHash.hash(data);
            int hash2 = bobIntHash.hash(data);
            Assert.assertEquals(hash1, hash2);
        }
    }

    @Test
    public void testParallel() {
        int blockByteLength = CommonConstants.STATS_BYTE_LENGTH;
        int maxNum = 1 << blockByteLength;
        byte[] data = new byte[blockByteLength];
        BobIntHash bobIntHash = new BobIntHash();
        Set<Integer> hashSet = IntStream.range(0, maxNum).parallel()
            .map(index -> bobIntHash.hash(data)).boxed()
            .collect(Collectors.toSet());
        Assert.assertEquals(1, hashSet.size());
    }
}
