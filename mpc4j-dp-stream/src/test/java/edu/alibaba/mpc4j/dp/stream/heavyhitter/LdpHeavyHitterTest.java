package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHeavyHitterFactory.LdpHeavyHitterType;
import edu.alibaba.mpc4j.dp.stream.structure.NaiveStreamCounter;
import edu.alibaba.mpc4j.dp.stream.structure.TestStreamCounter;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.util.RamUsageEstimator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Heavy Hitter with Local Differential Privacy test.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
@RunWith(Parameterized.class)
public class LdpHeavyHitterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdpHeavyHitterTest.class);
    /**
     * File path for stream_counter_example_data.txt
     */
    static final String EXAMPLE_DATA_PATH = Objects.requireNonNull(
        TestStreamCounter.class.getClassLoader().getResource("stream_counter_example_data.txt")
    ).getPath();
    /**
     * Key set for stream_counter_example_data.txt
     */
    static final Set<String> EXAMPLE_DATA_DOMAIN = IntStream.rangeClosed(480, 520)
        .mapToObj(String::valueOf).collect(Collectors.toSet());
    /**
     * Key num for stream_counter_example_data.txt
     */
    private static final int EXAMPLE_D = EXAMPLE_DATA_DOMAIN.size();
    /**
     * correct count map for stream_counter_example_data.txt
     */
    private static final Map<String, Integer> CORRECT_EXAMPLE_COUNT_MAP;
    /**
     * correct count ordered list for stream_counter_example_data.txt
     */
    private static final List<Map.Entry<String, Integer>> CORRECT_EXAMPLE_COUNT_ORDERED_LIST;

    static {
        NaiveStreamCounter streamCounter = new NaiveStreamCounter();
        try {
            Files.lines(Paths.get(EXAMPLE_DATA_PATH)).forEach(streamCounter::insert);
            CORRECT_EXAMPLE_COUNT_MAP = EXAMPLE_DATA_DOMAIN.stream()
                .collect(Collectors.toMap(item -> item, streamCounter::query));
            CORRECT_EXAMPLE_COUNT_ORDERED_LIST = new ArrayList<>(CORRECT_EXAMPLE_COUNT_MAP.entrySet());
            // descending sort
            CORRECT_EXAMPLE_COUNT_ORDERED_LIST.sort(Comparator.comparingInt(Map.Entry::getValue));
            Collections.reverse(CORRECT_EXAMPLE_COUNT_ORDERED_LIST);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }
    /**
     * File path for stream_counter_example_data.txt
     */
    static final String CONNECT_DATA_PATH = Objects.requireNonNull(
        TestStreamCounter.class.getClassLoader().getResource("connect.dat")
    ).getPath();
    /**
     * Key set for stream_counter_example_data.txt
     */
    static final Set<String> CONNECT_DATA_DOMAIN = IntStream.rangeClosed(1, 129)
        .mapToObj(String::valueOf).collect(Collectors.toSet());
    /**
     * Key num for stream_counter_example_data.txt
     */
    private static final int CONNECT_D = CONNECT_DATA_DOMAIN.size();

    /**
     * large ε
     */
    static final double LARGE_EPSILON = 128;
    /**
     * default ε
     */
    private static final double DEFAULT_EPSILON = 16;
    /**
     * default k
     */
    static final int DEFAULT_K = 20;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // NAIVE
        configurations.add(new Object[]{
            LdpHeavyHitterType.NAIVE.name(), LdpHeavyHitterType.NAIVE,
        });
        // basic heavy guardian
        configurations.add(new Object[]{
            LdpHeavyHitterType.BASIC_HEAVY_GUARDIAN.name(), LdpHeavyHitterType.BASIC_HEAVY_GUARDIAN,
        });

        return configurations;
    }

    /**
     * the type
     */
    private final LdpHeavyHitterType type;

    public LdpHeavyHitterTest(String name, LdpHeavyHitterType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createInstance(
            type, EXAMPLE_DATA_DOMAIN, EXAMPLE_D, DEFAULT_EPSILON
        );
        Assert.assertEquals(type, ldpHeavyHitter.getType());
    }

    @Test
    public void testLargeEpsilonFullK() throws IOException {
        LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createInstance(
            type, EXAMPLE_DATA_DOMAIN, EXAMPLE_D, LARGE_EPSILON
        );
        Files.lines(Paths.get(EXAMPLE_DATA_PATH)).forEach(ldpHeavyHitter::insert);
        Map<String, Double> countMap = ldpHeavyHitter.responseDomain();
        double totalNum = 0;
        for (String item : EXAMPLE_DATA_DOMAIN) {
            totalNum += countMap.get(item);
            Assert.assertEquals(CORRECT_EXAMPLE_COUNT_MAP.get(item), countMap.get(item), DoubleUtils.PRECISION);
        }
        Assert.assertEquals(ldpHeavyHitter.getNum(), totalNum, DoubleUtils.PRECISION);
    }

    @Test
    public void testFullK() throws IOException {
        LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createInstance(
            type, EXAMPLE_DATA_DOMAIN, EXAMPLE_D, LARGE_EPSILON
        );
        Files.lines(Paths.get(EXAMPLE_DATA_PATH)).forEach(ldpHeavyHitter::insert);
        List<Map.Entry<String, Double>> countOrderedList = ldpHeavyHitter.responseOrderedDomain();
        Assert.assertEquals(CORRECT_EXAMPLE_COUNT_ORDERED_LIST.size(), countOrderedList.size());
        int domainSize = CORRECT_EXAMPLE_COUNT_ORDERED_LIST.size();
        for (int index = 0; index < domainSize; index++) {
            Assert.assertEquals(
                CORRECT_EXAMPLE_COUNT_ORDERED_LIST.get(index).getKey(), countOrderedList.get(index).getKey()
            );
        }
    }

    @Test
    public void testMemory() throws IOException {
        LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createInstance(
            type, CONNECT_DATA_DOMAIN, DEFAULT_K, DEFAULT_EPSILON
        );
        Files.lines(Paths.get(CONNECT_DATA_PATH)).forEach(ldpHeavyHitter::insert);
        String memory = RamUsageEstimator.humanSizeOf(ldpHeavyHitter);
        LOGGER.info("{}: k = {}, d = {}, memory = {}", type.name(), DEFAULT_K, CONNECT_D, memory);
    }
}
