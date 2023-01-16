package edu.alibaba.mpc4j.dp.service.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.BasicHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.structure.NaiveStreamCounter;
import edu.alibaba.mpc4j.dp.service.structure.StreamCounterTest;
import edu.alibaba.mpc4j.dp.service.tool.StreamDataUtils;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory.HhLdpType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Heavy Hitter with Local Differential Privacy test.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
@RunWith(Parameterized.class)
public class HhLdpServerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HhLdpServerTest.class);
    /**
     * File path for stream_counter_example_data.txt
     */
    static final String EXAMPLE_DATA_PATH = Objects.requireNonNull(
        StreamCounterTest.class.getClassLoader().getResource("stream_counter_example_data.txt")
    ).getPath();
    /**
     * Key set for stream_counter_example_data.txt
     */
    static final Set<String> EXAMPLE_DATA_DOMAIN = IntStream
        .rangeClosed(480, 520)
        .mapToObj(String::valueOf)
        .collect(Collectors.toSet());
    /**
     * Key num for stream_counter_example_data.txt
     */
    static final int EXAMPLE_D = EXAMPLE_DATA_DOMAIN.size();
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
            Stream<String> dataStream = StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH);
            EXAMPLE_WARMUP_NUM = (int)Math.round(dataStream.count() * 0.01);
            dataStream.close();
            NaiveStreamCounter streamCounter = new NaiveStreamCounter();
            dataStream = Files.lines(Paths.get(EXAMPLE_DATA_PATH));
            dataStream.forEach(streamCounter::insert);
            dataStream.close();
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
        StreamCounterTest.class.getClassLoader().getResource("connect.dat")
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
            Stream<String> dataStream = StreamDataUtils.obtainItemStream(CONNECT_DATA_PATH);
            CONNECT_WARMUP_NUM = (int)Math.round(dataStream.count() * 0.01);
            dataStream.close();
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
            HhLdpType.RELAX_HG.name(), HhLdpType.RELAX_HG,
        });
        // advanced heavy guardian
        configurations.add(new Object[]{
            HhLdpType.ADVAN_HG.name(), HhLdpType.ADVAN_HG,
        });
        // basic heavy guardian
        configurations.add(new Object[]{
            HhLdpType.BASIC_HG.name(), HhLdpType.BASIC_HG,
        });
        // NAIVE
        configurations.add(new Object[]{
            HhLdpType.DE_FO.name(), HhLdpType.DE_FO,
        });

        return configurations;
    }

    /**
     * the type
     */
    private final HhLdpType type;

    public HhLdpServerTest(String name, HhLdpType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        HhLdpConfig config = new BasicHhLdpConfig.Builder(type, EXAMPLE_DATA_DOMAIN, EXAMPLE_D, DEFAULT_EPSILON)
            .build();
        // create server
        HhLdpServer server = HhLdpFactory.createServer(config);
        Assert.assertEquals(type, server.getType());
        // create client
        HhLdpClient client = HhLdpFactory.createClient(config);
        Assert.assertEquals(type, client.getType());
    }

    @Test
    public void testWarmup() throws IOException {
        int k = EXAMPLE_D;
        HhLdpConfig config = new BasicHhLdpConfig
            .Builder(type, EXAMPLE_DATA_DOMAIN, k, DEFAULT_EPSILON)
            .build();
        // create server and client
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        // warmup
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH);
        dataStream.map(client::warmup).forEach(server::warmupInsert);
        dataStream.close();
        Map<String, Double> heavyHitters = server.responseHeavyHitters();
        Assert.assertTrue(heavyHitters.size() <= k);
        for (String item : EXAMPLE_DATA_DOMAIN) {
            // there are the cases when the example data does not contain some items in the domain.
            if (heavyHitters.containsKey(item)) {
                // verify no-error count
                Assert.assertEquals(CORRECT_EXAMPLE_COUNT_MAP.get(item), heavyHitters.get(item), DoubleUtils.PRECISION);
            }
        }
    }

    @Test
    public void testStopWarmup() throws IOException {
        int k = EXAMPLE_D;
        HhLdpConfig config = new BasicHhLdpConfig
            .Builder(type, EXAMPLE_DATA_DOMAIN, k, DEFAULT_EPSILON)
            .build();
        // create server and client
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        // warmup
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH);
        dataStream.map(client::warmup).forEach(server::warmupInsert);
        dataStream.close();
        server.stopWarmup();
        Map<String, Double> heavyHitters = server.responseHeavyHitters();
        Assert.assertTrue(heavyHitters.size() <= k);
        for (String item : EXAMPLE_DATA_DOMAIN) {
            // there are the cases when the example data does not contain some items in the domain.
            if (heavyHitters.containsKey(item)) {
                // verify no-error count
                Assert.assertEquals(CORRECT_EXAMPLE_COUNT_MAP.get(item), heavyHitters.get(item), DoubleUtils.PRECISION);
            }
        }
    }

    @Test
    public void testLargeEpsilonFullK() throws IOException {
        int k = EXAMPLE_D;
        HhLdpConfig config = new BasicHhLdpConfig
            .Builder(type, EXAMPLE_DATA_DOMAIN, k, LARGE_EPSILON)
            .build();
        // create server and client
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        // warmup
        exampleWarmupInsert(server, client);
        server.stopWarmup();
        // randomize
        exampleRandomizeInsert(server, client);
        Map<String, Double> heavyHitters = server.responseHeavyHitters();
        Assert.assertTrue(heavyHitters.size() <= k);
        for (String item : EXAMPLE_DATA_DOMAIN) {
            // there are the cases when the example data does not contain some items in the domain.
            if (heavyHitters.containsKey(item)) {
                // verify no-error count
                Assert.assertEquals(CORRECT_EXAMPLE_COUNT_MAP.get(item), heavyHitters.get(item), DoubleUtils.PRECISION);
            }
        }
    }

    @Test
    public void testFullK() throws IOException {
        int k = EXAMPLE_D;
        HhLdpConfig config = new BasicHhLdpConfig
            .Builder(type, EXAMPLE_DATA_DOMAIN, k, DEFAULT_EPSILON)
            .build();
        // create server and client
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        // warmup
        exampleWarmupInsert(server, client);
        server.stopWarmup();
        // randomize
        exampleRandomizeInsert(server, client);
        Map<String, Double> heavyHitters = server.responseHeavyHitters();
        Assert.assertTrue(heavyHitters.size() <= k);
        // verify unbaised count
        double totalNum = heavyHitters.keySet().stream().mapToDouble(heavyHitters::get).sum();
        Assert.assertEquals(server.getNum(), totalNum, totalNum * 0.01);
    }

    @Test
    public void testDefault() throws IOException {
        HhLdpConfig config = new BasicHhLdpConfig
            .Builder(type, EXAMPLE_DATA_DOMAIN, DEFAULT_K, DEFAULT_EPSILON)
            .build();
        // create server and client
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        // warmup
        exampleWarmupInsert(server, client);
        server.stopWarmup();
        // randomize
        exampleRandomizeInsert(server, client);
        // verify there are k heavy hitters
        Map<String, Double> heavyHitters = server.responseHeavyHitters();
        Assert.assertEquals(heavyHitters.size(), DEFAULT_K);
        // verify k/2 heavy hitters are the same
        List<Map.Entry<String, Double>> orderedHeavyHitters = server.responseOrderedHeavyHitters();
        for (int index = 0; index < DEFAULT_K / 2; index++) {
            Assert.assertEquals(
                CORRECT_EXAMPLE_COUNT_ORDERED_LIST.get(index).getKey(), orderedHeavyHitters.get(index).getKey()
            );
        }
    }

    static void exampleWarmupInsert(HhLdpServer server, HhLdpClient client) throws IOException {
        AtomicInteger warmupIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH);
        dataStream.filter(item -> warmupIndex.getAndIncrement() <= EXAMPLE_WARMUP_NUM)
            .map(client::warmup)
            .forEach(server::warmupInsert);
        dataStream.close();
    }

    static void exampleRandomizeInsert(HhLdpServer server, HhLdpClient client) throws IOException {
        Random ldpRandom = new Random();
        AtomicInteger randomizedIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH);
        dataStream.filter(item -> randomizedIndex.getAndIncrement() > EXAMPLE_WARMUP_NUM)
            .map(item -> client.randomize(server.getServerContext(), item, ldpRandom))
            .forEach(server::randomizeInsert);
        dataStream.close();
    }

    @Test
    public void testMemory() throws IOException {
        HhLdpConfig config = new BasicHhLdpConfig
            .Builder(type, CONNECT_DATA_DOMAIN, DEFAULT_K, DEFAULT_EPSILON)
            .build();
        // create server and client
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        // warmup
        connectWarmupInsert(server, client);
        server.stopWarmup();
        // randomize
        connectRandomizeInsert(server, client);
        long memory = GraphLayout.parseInstance(server).totalSize();
        LOGGER.info("{}: k = {}, d = {}, memory = {}", type.name(), DEFAULT_K, CONNECT_D, memory);
    }

    static void connectWarmupInsert(HhLdpServer server, HhLdpClient client) throws IOException {
        AtomicInteger warmupIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(CONNECT_DATA_PATH);
        dataStream.filter(item -> warmupIndex.getAndIncrement() <= CONNECT_WARMUP_NUM)
            .map(client::warmup)
            .forEach(server::warmupInsert);
        dataStream.close();
    }

    static void connectRandomizeInsert(HhLdpServer server, HhLdpClient client) throws IOException {
        Random ldpRandom = new Random();
        AtomicInteger randomizedIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(CONNECT_DATA_PATH);
        dataStream.filter(item -> randomizedIndex.getAndIncrement() > CONNECT_WARMUP_NUM)
            .map(item -> client.randomize(server.getServerContext(), item, ldpRandom))
            .forEach(server::randomizeInsert);
        dataStream.close();
    }
}
