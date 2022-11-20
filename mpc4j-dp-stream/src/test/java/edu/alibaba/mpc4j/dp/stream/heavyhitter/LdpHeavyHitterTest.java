package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHeavyHitterFactory.LdpHeavyHitterType;
import edu.alibaba.mpc4j.dp.stream.structure.NaiveStreamCounter;
import edu.alibaba.mpc4j.dp.stream.structure.TestStreamCounter;
import edu.alibaba.mpc4j.dp.stream.tool.StreamDataUtils;
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
import java.util.concurrent.atomic.AtomicInteger;
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
     * warmup num for stream_counter_example_data.txt
     */
    private static final int EXAMPLE_WARMUP_NUM;
    /**
     * correct count map for stream_counter_example_data.txt
     */
    private static final Map<String, Integer> CORRECT_EXAMPLE_COUNT_MAP;
    /**
     * correct count ordered list for stream_counter_example_data.txt
     */
    private static final List<Map.Entry<String, Integer>> CORRECT_EXAMPLE_COUNT_ORDERED_LIST;

    static {
        try {
            EXAMPLE_WARMUP_NUM = (int)Math.round(StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH).count() * 0.01);
            NaiveStreamCounter streamCounter = new NaiveStreamCounter();
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
     * File path for connect.dat
     */
    static final String CONNECT_DATA_PATH = Objects.requireNonNull(
        TestStreamCounter.class.getClassLoader().getResource("connect.dat")
    ).getPath();
    /**
     * Key set for connect.dat
     */
    static final Set<String> CONNECT_DATA_DOMAIN = IntStream.rangeClosed(1, 129)
        .mapToObj(String::valueOf).collect(Collectors.toSet());
    /**
     * Key num for connect.dat
     */
    private static final int CONNECT_D = CONNECT_DATA_DOMAIN.size();
    /**
     * warmup num for connect.dat
     */
    private static final int CONNECT_WARMUP_NUM;

    static {
        try {
            CONNECT_WARMUP_NUM = (int)Math.round(StreamDataUtils.obtainItemStream(CONNECT_DATA_PATH).count() * 0.01);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }
    /**
     * large ε
     */
    static final double LARGE_EPSILON = 128;
    /**
     * default ε
     */
    static final double DEFAULT_EPSILON = 16;
    /**
     * default k
     */
    static final int DEFAULT_K = 20;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // relaxed heavy guardian
        configurations.add(new Object[]{
            LdpHeavyHitterType.RELAX_HG.name(), LdpHeavyHitterType.RELAX_HG,
        });
        // advanced heavy guardian
        configurations.add(new Object[]{
            LdpHeavyHitterType.ADVAN_HG.name(), LdpHeavyHitterType.ADVAN_HG,
        });
        // basic heavy guardian
        configurations.add(new Object[]{
            LdpHeavyHitterType.BASIC_HG.name(), LdpHeavyHitterType.BASIC_HG,
        });
        // NAIVE
        configurations.add(new Object[]{
            LdpHeavyHitterType.NAIVE_RR.name(), LdpHeavyHitterType.NAIVE_RR,
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
    public void testWarmup() throws IOException {
        LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createInstance(
            type, EXAMPLE_DATA_DOMAIN, EXAMPLE_D, DEFAULT_EPSILON
        );
        // warmup
        StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH).forEach(ldpHeavyHitter::warmupInsert);
        Map<String, Double> countMap = ldpHeavyHitter.responseDomain();
        double totalNum = 0;
        for (String item : EXAMPLE_DATA_DOMAIN) {
            totalNum += countMap.get(item);
            Assert.assertEquals(CORRECT_EXAMPLE_COUNT_MAP.get(item), countMap.get(item), DoubleUtils.PRECISION);
        }
        Assert.assertEquals(ldpHeavyHitter.getNum(), totalNum, DoubleUtils.PRECISION);
    }

    @Test
    public void testStopWarmup() throws IOException {
        LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createInstance(
            type, EXAMPLE_DATA_DOMAIN, EXAMPLE_D, DEFAULT_EPSILON
        );
        // warmup
        StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH).forEach(ldpHeavyHitter::warmupInsert);
        ldpHeavyHitter.stopWarmup();
        Map<String, Double> countMap = ldpHeavyHitter.responseDomain();
        double totalNum = 0;
        for (String item : EXAMPLE_DATA_DOMAIN) {
            totalNum += countMap.get(item);
            Assert.assertEquals(CORRECT_EXAMPLE_COUNT_MAP.get(item), countMap.get(item), 0.1);
        }
        Assert.assertEquals(ldpHeavyHitter.getNum(), totalNum, 0.1);
    }

    @Test
    public void testLargeEpsilonFullK() throws IOException {
        LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createInstance(
            type, EXAMPLE_DATA_DOMAIN, EXAMPLE_D, LARGE_EPSILON
        );
        // warmup
        exampleWarmupInsert(ldpHeavyHitter);
        ldpHeavyHitter.stopWarmup();
        // randomize
        exampleRandomizeInsert(ldpHeavyHitter);
        Map<String, Double> countMap = ldpHeavyHitter.responseDomain();
        for (String item : EXAMPLE_DATA_DOMAIN) {
            // verify no-error count
            Assert.assertEquals(CORRECT_EXAMPLE_COUNT_MAP.get(item), countMap.get(item), DoubleUtils.PRECISION);
        }
    }

    @Test
    public void testFullK() throws IOException {
        LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createInstance(
            type, EXAMPLE_DATA_DOMAIN, EXAMPLE_D, DEFAULT_EPSILON
        );
        // warmup
        exampleWarmupInsert(ldpHeavyHitter);
        ldpHeavyHitter.stopWarmup();
        // randomize
        exampleRandomizeInsert(ldpHeavyHitter);
        List<Map.Entry<String, Double>> countOrderedList = ldpHeavyHitter.responseOrderedDomain();
        Assert.assertEquals(CORRECT_EXAMPLE_COUNT_ORDERED_LIST.size(), countOrderedList.size());
        // verify unbaised count
        double totalNum = 0;
        int domainSize = CORRECT_EXAMPLE_COUNT_ORDERED_LIST.size();
        for (int index = 0; index < domainSize; index++) {
            totalNum += countOrderedList.get(index).getValue();
        }
        Assert.assertEquals(ldpHeavyHitter.getNum(), totalNum, totalNum * 0.01);
    }

    @Test
    public void testDefault() throws IOException {
        LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createInstance(
            type, EXAMPLE_DATA_DOMAIN, DEFAULT_K, DEFAULT_EPSILON
        );
        // warmup
        exampleWarmupInsert(ldpHeavyHitter);
        ldpHeavyHitter.stopWarmup();
        // randomize
        exampleRandomizeInsert(ldpHeavyHitter);
        // verify there are k heavy hitters
        Map<String, Double> heavyHitterMap = ldpHeavyHitter.responseHeavyHitters();
        Assert.assertEquals(heavyHitterMap.size(), DEFAULT_K);
        // verify k/2 heavy hitters are the same
        List<Map.Entry<String, Double>> heavyHitterOrderedList = ldpHeavyHitter.responseOrderedHeavyHitters();
        for (int index = 0; index < DEFAULT_K / 2; index++) {
            Assert.assertEquals(
                CORRECT_EXAMPLE_COUNT_ORDERED_LIST.get(index).getKey(), heavyHitterOrderedList.get(index).getKey()
            );
        }
    }

    static void exampleWarmupInsert(LdpHeavyHitter ldpHeavyHitter) throws IOException {
        AtomicInteger warmupIndex = new AtomicInteger();
        StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH)
            .filter(item -> warmupIndex.getAndIncrement() <= EXAMPLE_WARMUP_NUM)
            .forEach(ldpHeavyHitter::warmupInsert);
    }

    static void exampleRandomizeInsert(LdpHeavyHitter ldpHeavyHitter) throws IOException {
        Random ldpRandom = new Random();
        AtomicInteger randomizedIndex = new AtomicInteger();
        StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH)
            .filter(item -> randomizedIndex.getAndIncrement() > EXAMPLE_WARMUP_NUM)
            .map(item -> ldpHeavyHitter.randomize(ldpHeavyHitter.getCurrentDataStructure(), item, ldpRandom))
            .forEach(ldpHeavyHitter::randomizeInsert);
    }

    @Test
    public void testMemory() throws IOException {
        LdpHeavyHitter ldpHeavyHitter = LdpHeavyHitterFactory.createInstance(
            type, CONNECT_DATA_DOMAIN, DEFAULT_K, DEFAULT_EPSILON
        );
        // warmup
        connectWarmupInsert(ldpHeavyHitter);
        ldpHeavyHitter.stopWarmup();
        // randomize
        connectRandomizeInsert(ldpHeavyHitter);
        String memory = RamUsageEstimator.humanSizeOf(ldpHeavyHitter);
        LOGGER.info("{}: k = {}, d = {}, memory = {}", type.name(), DEFAULT_K, CONNECT_D, memory);
    }

    static void connectWarmupInsert(LdpHeavyHitter ldpHeavyHitter) throws IOException {
        AtomicInteger warmupIndex = new AtomicInteger();
        StreamDataUtils.obtainItemStream(CONNECT_DATA_PATH)
            .filter(item -> warmupIndex.getAndIncrement() <= CONNECT_WARMUP_NUM)
            .forEach(ldpHeavyHitter::warmupInsert);
    }

    static void connectRandomizeInsert(LdpHeavyHitter ldpHeavyHitter) throws IOException {
        Random ldpRandom = new Random();
        AtomicInteger randomizedIndex = new AtomicInteger();
        StreamDataUtils.obtainItemStream(CONNECT_DATA_PATH)
            .filter(item -> randomizedIndex.getAndIncrement() > CONNECT_WARMUP_NUM)
            .map(item -> ldpHeavyHitter.randomize(ldpHeavyHitter.getCurrentDataStructure(), item, ldpRandom))
            .forEach(ldpHeavyHitter::randomizeInsert);
    }
}
