package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHeavyHitterFactory.LdpHeavyHitterType;
import edu.alibaba.mpc4j.dp.stream.structure.HeavyGuardian;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HeavyGuardian Heavy Hitter with Local Differential Privacy test.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
@RunWith(Parameterized.class)
public class HgLdpHeavyHitterTest {
    /**
     * HeavyGuardian seed
     */
    private static final long HEAVY_GUARDIAN_SEED = 1234567890L;
    /**
     * correct HeavyGuardian count ordered list for stream_counter_example_data.txt
     */
    private static final List<Map.Entry<String, Integer>> CORRECT_HG_EXAMPLE_COUNT_ORDERED_LIST;

    static {
        try {
            Random random = new Random(HEAVY_GUARDIAN_SEED);
            HeavyGuardian heavyGuardian = new HeavyGuardian(1, LdpHeavyHitterTest.DEFAULT_K, 0, random);
            Files.lines(Paths.get(LdpHeavyHitterTest.EXAMPLE_DATA_PATH)).forEach(heavyGuardian::insert);
            Map<String, Integer> correctCountMap = LdpHeavyHitterTest.EXAMPLE_DATA_DOMAIN.stream()
                .collect(Collectors.toMap(item -> item, heavyGuardian::query));
            CORRECT_HG_EXAMPLE_COUNT_ORDERED_LIST = new ArrayList<>(correctCountMap.entrySet());
            // descending sort
            CORRECT_HG_EXAMPLE_COUNT_ORDERED_LIST.sort(Comparator.comparingInt(Map.Entry::getValue));
            Collections.reverse(CORRECT_HG_EXAMPLE_COUNT_ORDERED_LIST);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }

    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // basic heavy guardian
        configurations.add(new Object[] {
            LdpHeavyHitterType.BASIC_HEAVY_GUARDIAN.name(), LdpHeavyHitterType.BASIC_HEAVY_GUARDIAN,
        });

        return configurations;
    }

    /**
     * the type
     */
    private final LdpHeavyHitterType type;

    public HgLdpHeavyHitterTest(String name, LdpHeavyHitterType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testLargeEpsilon() throws IOException {
        Random random = new Random(HEAVY_GUARDIAN_SEED);
        LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createHeavyGuardianInstance(
            type, LdpHeavyHitterTest.EXAMPLE_DATA_DOMAIN, LdpHeavyHitterTest.DEFAULT_K,
            LdpHeavyHitterTest.LARGE_EPSILON, random
        );
        Files.lines(Paths.get(LdpHeavyHitterTest.EXAMPLE_DATA_PATH)).forEach(ldpHeavyHitter::insert);
        Map<String, Double> heavyHitterMap = ldpHeavyHitter.responseHeavyHitters();
        Assert.assertEquals(LdpHeavyHitterTest.DEFAULT_K, heavyHitterMap.size());
        List<Map.Entry<String, Double>> orderedHeavyHitterList = ldpHeavyHitter.responseOrderedHeavyHitters();
        Assert.assertEquals(LdpHeavyHitterTest.DEFAULT_K, orderedHeavyHitterList.size());

        for (int index = 0; index < LdpHeavyHitterTest.DEFAULT_K; index++) {
            Assert.assertEquals(
                CORRECT_HG_EXAMPLE_COUNT_ORDERED_LIST.get(index).getKey(), orderedHeavyHitterList.get(index).getKey()
            );
        }
    }
}
