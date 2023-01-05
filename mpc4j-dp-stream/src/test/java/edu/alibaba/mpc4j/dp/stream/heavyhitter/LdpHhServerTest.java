package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory.LdpHhType;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.BasicLdpHhClientConfig;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.BasicLdpHhServerConfig;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.LdpHhClientConfig;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.LdpHhServerConfig;
import edu.alibaba.mpc4j.dp.stream.structure.NaiveStreamCounter;
import edu.alibaba.mpc4j.dp.stream.structure.StreamCounterTest;
import edu.alibaba.mpc4j.dp.stream.tool.StreamDataUtils;
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
public class LdpHhServerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdpHhServerTest.class);
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
            LdpHhType.RELAX_HG.name(), LdpHhType.RELAX_HG,
        });
        // advanced heavy guardian
        configurations.add(new Object[]{
            LdpHhType.ADVAN_HG.name(), LdpHhType.ADVAN_HG,
        });
        // basic heavy guardian
        configurations.add(new Object[]{
            LdpHhType.BASIC_HG.name(), LdpHhType.BASIC_HG,
        });
        // NAIVE
        configurations.add(new Object[]{
            LdpHhType.DE_FO.name(), LdpHhType.DE_FO,
        });

        return configurations;
    }

    /**
     * the type
     */
    private final LdpHhType type;

    public LdpHhServerTest(String name, LdpHhType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        // create server
        LdpHhServerConfig serverConfig = new BasicLdpHhServerConfig
            .Builder(type, EXAMPLE_D, EXAMPLE_D, DEFAULT_EPSILON)
            .build();
        LdpHhServer server = LdpHhFactory.createServer(serverConfig);
        Assert.assertEquals(type, server.getType());
        // create client
        LdpHhClientConfig clientConfig = new BasicLdpHhClientConfig
            .Builder(type, EXAMPLE_DATA_DOMAIN, EXAMPLE_D, DEFAULT_EPSILON)
            .build();
        LdpHhClient client = LdpHhFactory.createClient(clientConfig);
        Assert.assertEquals(type, client.getType());
    }

    @Test
    public void testWarmup() throws IOException {
        // create server
        LdpHhServerConfig serverConfig = new BasicLdpHhServerConfig
            .Builder(type, EXAMPLE_D, EXAMPLE_D, DEFAULT_EPSILON)
            .build();
        LdpHhServer server = LdpHhFactory.createServer(serverConfig);
        // warmup
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH);
        dataStream.forEach(server::warmupInsert);
        dataStream.close();
        Map<String, Double> countMap = server.responseDomain(EXAMPLE_DATA_DOMAIN);
        double totalNum = 0;
        for (String item : EXAMPLE_DATA_DOMAIN) {
            totalNum += countMap.get(item);
            Assert.assertEquals(CORRECT_EXAMPLE_COUNT_MAP.get(item), countMap.get(item), DoubleUtils.PRECISION);
        }
        Assert.assertEquals(server.getNum(), totalNum, DoubleUtils.PRECISION);
    }

    @Test
    public void testStopWarmup() throws IOException {
        // create server
        LdpHhServerConfig serverConfig = new BasicLdpHhServerConfig
            .Builder(type, EXAMPLE_D, EXAMPLE_D, DEFAULT_EPSILON)
            .build();
        LdpHhServer server = LdpHhFactory.createServer(serverConfig);
        // warmup
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH);
        dataStream.forEach(server::warmupInsert);
        dataStream.close();
        server.stopWarmup();
        Map<String, Double> countMap = server.responseDomain(EXAMPLE_DATA_DOMAIN);
        double totalNum = 0;
        for (String item : EXAMPLE_DATA_DOMAIN) {
            totalNum += countMap.get(item);
            Assert.assertEquals(CORRECT_EXAMPLE_COUNT_MAP.get(item), countMap.get(item), 0.1);
        }
        Assert.assertEquals(server.getNum(), totalNum, 0.1);
    }

    @Test
    public void testLargeEpsilonFullK() throws IOException {
        // create server
        LdpHhServerConfig serverConfig = new BasicLdpHhServerConfig
            .Builder(type, EXAMPLE_D, EXAMPLE_D, LARGE_EPSILON)
            .build();
        LdpHhServer server = LdpHhFactory.createServer(serverConfig);
        // create client
        LdpHhClientConfig clientConfig = new BasicLdpHhClientConfig
            .Builder(type, EXAMPLE_DATA_DOMAIN, EXAMPLE_D, LARGE_EPSILON)
            .build();
        LdpHhClient client = LdpHhFactory.createClient(clientConfig);
        // warmup
        exampleWarmupInsert(server);
        server.stopWarmup();
        // randomize
        exampleRandomizeInsert(server, client);
        Map<String, Double> countMap = server.responseDomain(EXAMPLE_DATA_DOMAIN);
        for (String item : EXAMPLE_DATA_DOMAIN) {
            // verify no-error count
            Assert.assertEquals(CORRECT_EXAMPLE_COUNT_MAP.get(item), countMap.get(item), DoubleUtils.PRECISION);
        }
    }

    @Test
    public void testFullK() throws IOException {
        // create server
        LdpHhServerConfig serverConfig = new BasicLdpHhServerConfig
            .Builder(type, EXAMPLE_D, EXAMPLE_D, DEFAULT_EPSILON)
            .build();
        LdpHhServer server = LdpHhFactory.createServer(serverConfig);
        // create client
        LdpHhClientConfig clientConfig = new BasicLdpHhClientConfig
            .Builder(type, EXAMPLE_DATA_DOMAIN, EXAMPLE_D, DEFAULT_EPSILON)
            .build();
        LdpHhClient client = LdpHhFactory.createClient(clientConfig);
        // warmup
        exampleWarmupInsert(server);
        server.stopWarmup();
        // randomize
        exampleRandomizeInsert(server, client);
        List<Map.Entry<String, Double>> countOrderedList = server.responseOrderedDomain(EXAMPLE_DATA_DOMAIN);
        Assert.assertEquals(CORRECT_EXAMPLE_COUNT_ORDERED_LIST.size(), countOrderedList.size());
        // verify unbaised count
        double totalNum = 0;
        int domainSize = CORRECT_EXAMPLE_COUNT_ORDERED_LIST.size();
        for (int index = 0; index < domainSize; index++) {
            totalNum += countOrderedList.get(index).getValue();
        }
        Assert.assertEquals(server.getNum(), totalNum, totalNum * 0.01);
    }

    @Test
    public void testDefault() throws IOException {
        // create server
        LdpHhServerConfig serverConfig = new BasicLdpHhServerConfig
            .Builder(type, EXAMPLE_D, DEFAULT_K, DEFAULT_EPSILON)
            .build();
        LdpHhServer server = LdpHhFactory.createServer(serverConfig);
        // create client
        LdpHhClientConfig clientConfig = new BasicLdpHhClientConfig
            .Builder(type, EXAMPLE_DATA_DOMAIN, DEFAULT_K, DEFAULT_EPSILON)
            .build();
        LdpHhClient client = LdpHhFactory.createClient(clientConfig);
        // warmup
        exampleWarmupInsert(server);
        server.stopWarmup();
        // randomize
        exampleRandomizeInsert(server, client);
        // verify there are k heavy hitters
        Map<String, Double> heavyHitterMap = server.responseHeavyHitters();
        Assert.assertEquals(heavyHitterMap.size(), DEFAULT_K);
        // verify k/2 heavy hitters are the same
        List<Map.Entry<String, Double>> heavyHitterOrderedList = server.responseOrderedHeavyHitters();
        for (int index = 0; index < DEFAULT_K / 2; index++) {
            Assert.assertEquals(
                CORRECT_EXAMPLE_COUNT_ORDERED_LIST.get(index).getKey(), heavyHitterOrderedList.get(index).getKey()
            );
        }
    }

    static void exampleWarmupInsert(LdpHhServer server) throws IOException {
        AtomicInteger warmupIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(EXAMPLE_DATA_PATH);
        dataStream.filter(item -> warmupIndex.getAndIncrement() <= EXAMPLE_WARMUP_NUM).forEach(server::warmupInsert);
        dataStream.close();
    }

    static void exampleRandomizeInsert(LdpHhServer server, LdpHhClient client) throws IOException {
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
        // create server
        LdpHhServerConfig serverConfig = new BasicLdpHhServerConfig
            .Builder(type, CONNECT_D, DEFAULT_K, DEFAULT_EPSILON)
            .build();
        LdpHhServer server = LdpHhFactory.createServer(serverConfig);
        // create client
        LdpHhClientConfig clientConfig = new BasicLdpHhClientConfig
            .Builder(type, CONNECT_DATA_DOMAIN, DEFAULT_K, DEFAULT_EPSILON)
            .build();
        LdpHhClient client = LdpHhFactory.createClient(clientConfig);
        // warmup
        connectWarmupInsert(server);
        server.stopWarmup();
        // randomize
        connectRandomizeInsert(server, client);
        long memory = GraphLayout.parseInstance(server).totalSize();
        LOGGER.info("{}: k = {}, d = {}, memory = {}", type.name(), DEFAULT_K, CONNECT_D, memory);
    }

    static void connectWarmupInsert(LdpHhServer server) throws IOException {
        AtomicInteger warmupIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(CONNECT_DATA_PATH);
        dataStream.filter(item -> warmupIndex.getAndIncrement() <= CONNECT_WARMUP_NUM)
            .forEach(server::warmupInsert);
        dataStream.close();
    }

    static void connectRandomizeInsert(LdpHhServer server, LdpHhClient client) throws IOException {
        Random ldpRandom = new Random();
        AtomicInteger randomizedIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(CONNECT_DATA_PATH);
        dataStream.filter(item -> randomizedIndex.getAndIncrement() > CONNECT_WARMUP_NUM)
            .map(item -> client.randomize(server.getServerContext(), item, ldpRandom))
            .forEach(server::randomizeInsert);
        dataStream.close();
    }
}
