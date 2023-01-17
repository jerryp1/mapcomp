package edu.alibaba.mpc4j.dp.service.fo.config;

import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.dp.service.fo.rappor.RapporFoLdpUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RAPPOR Frequency Oracle config utilities test.
 *
 * @author Weiran Liu
 * @date 2023/1/17
 */
public class RapporFoLdpConfigUtilsTest {
    /**
     * default max item
     */
    private static final int DEFAULT_MAX_ITEM = 100;

    @Test
    public void testEqualHashPosition() {
        testHash(2, 2);
        testHash(3, 3);
        testHash(4, 4);
        testHash(40, 40);
    }

    @Test
    public void testUnequalHashPosition() {
        testHash(2, 3);
        testHash(2, 4);
        testHash(2, 1 << 20);
        testHash(10, 1 << 20);
        testHash(20, 1 << 20);
        testHash(40, 1 << 20);
    }

    private void testHash(int k, int m) {
        Random random = new Random();
        IntHash intHash = IntHashFactory.fastestInstance();
        int[] hashSeeds = IntStream.range(0, k).map(hashIndex -> random.nextInt()).toArray();
        IntStream.range(0, DEFAULT_MAX_ITEM)
            .mapToObj(String::valueOf)
            .forEach(item -> {
                int[] hashValues = RapporFoLdpUtils.hash(intHash, item, m, hashSeeds);
                Assert.assertEquals(k, hashValues.length);
                Set<Integer> hashValueSet = Arrays.stream(hashValues)
                    // each hash value should be in range [0, m)
                    .peek(hashValue -> Assert.assertTrue(hashValue >= 0 && hashValue < m))
                    .boxed()
                    .collect(Collectors.toSet());
                // all values should be distinct
                Assert.assertEquals(k, hashValueSet.size());
            });
    }
}
