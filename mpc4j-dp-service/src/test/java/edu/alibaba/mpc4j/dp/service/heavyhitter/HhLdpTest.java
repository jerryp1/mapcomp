package edu.alibaba.mpc4j.dp.service.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.service.LdpTestDataUtils;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.BasicHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.tool.StreamDataUtils;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory.HhLdpType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Heavy Hitter LDP test.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
@RunWith(Parameterized.class)
public class HhLdpTest {
    /**
     * large ε
     */
    private static final double LARGE_EPSILON = 128;
    /**
     * default ε
     */
    private static final double DEFAULT_EPSILON = 16;
    /**
     * default k
     */
    private static final int DEFAULT_K = 20;

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

    public HhLdpTest(String name, HhLdpType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        HhLdpConfig config = new BasicHhLdpConfig
            .Builder(type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, LdpTestDataUtils.EXAMPLE_DATA_D, DEFAULT_EPSILON)
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
        int k = LdpTestDataUtils.EXAMPLE_DATA_D;
        HhLdpConfig config = new BasicHhLdpConfig
            .Builder(type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, k, DEFAULT_EPSILON)
            .build();
        // create server and client
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        // warmup
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(LdpTestDataUtils.EXAMPLE_DATA_PATH);
        dataStream.map(client::warmup).forEach(server::warmupInsert);
        dataStream.close();
        Map<String, Double> heavyHitters = server.heavyHitters();
        Assert.assertTrue(heavyHitters.size() <= k);
        for (String item : LdpTestDataUtils.EXAMPLE_DATA_DOMAIN) {
            // there are the cases when the example data does not contain some items in the domain.
            if (heavyHitters.containsKey(item)) {
                // verify no-error count
                Assert.assertEquals(
                    LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_MAP.get(item), heavyHitters.get(item), DoubleUtils.PRECISION
                );
            }
        }
    }

    @Test
    public void testStopWarmup() throws IOException {
        int k = LdpTestDataUtils.EXAMPLE_DATA_D;
        HhLdpConfig config = new BasicHhLdpConfig
            .Builder(type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, k, DEFAULT_EPSILON)
            .build();
        // create server and client
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        // warmup
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(LdpTestDataUtils.EXAMPLE_DATA_PATH);
        dataStream.map(client::warmup).forEach(server::warmupInsert);
        dataStream.close();
        server.stopWarmup();
        Map<String, Double> heavyHitters = server.heavyHitters();
        Assert.assertTrue(heavyHitters.size() <= k);
        for (String item : LdpTestDataUtils.EXAMPLE_DATA_DOMAIN) {
            // there are the cases when the example data does not contain some items in the domain.
            if (heavyHitters.containsKey(item)) {
                // verify no-error count
                Assert.assertEquals(
                    LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_MAP.get(item), heavyHitters.get(item), DoubleUtils.PRECISION
                );
            }
        }
    }

    @Test
    public void testLargeEpsilonFullK() throws IOException {
        int k = LdpTestDataUtils.EXAMPLE_DATA_D;
        HhLdpConfig config = new BasicHhLdpConfig
            .Builder(type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, k, LARGE_EPSILON)
            .build();
        // create server and client
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        // warmup
        exampleWarmupInsert(server, client);
        server.stopWarmup();
        // randomize
        exampleRandomizeInsert(server, client);
        Map<String, Double> heavyHitters = server.heavyHitters();
        Assert.assertTrue(heavyHitters.size() <= k);
        for (String item : LdpTestDataUtils.EXAMPLE_DATA_DOMAIN) {
            // there are the cases when the example data does not contain some items in the domain.
            if (heavyHitters.containsKey(item)) {
                // verify no-error count
                Assert.assertEquals(
                    LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_MAP.get(item), heavyHitters.get(item), DoubleUtils.PRECISION
                );
            }
        }
    }

    @Test
    public void testFullK() throws IOException {
        int k = LdpTestDataUtils.EXAMPLE_DATA_D;
        HhLdpConfig config = new BasicHhLdpConfig
            .Builder(type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, k, DEFAULT_EPSILON)
            .build();
        // create server and client
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        // warmup
        exampleWarmupInsert(server, client);
        server.stopWarmup();
        // randomize
        exampleRandomizeInsert(server, client);
        Map<String, Double> heavyHitters = server.heavyHitters();
        Assert.assertTrue(heavyHitters.size() <= k);
        // verify unbaised count
        double totalNum = heavyHitters.keySet().stream().mapToDouble(heavyHitters::get).sum();
        Assert.assertEquals(server.getNum(), totalNum, totalNum * 0.01);
    }

    @Test
    public void testDefault() throws IOException {
        HhLdpConfig config = new BasicHhLdpConfig
            .Builder(type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, DEFAULT_K, DEFAULT_EPSILON)
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
        Map<String, Double> heavyHitters = server.heavyHitters();
        Assert.assertEquals(heavyHitters.size(), DEFAULT_K);
        // verify k/2 heavy hitters are the same
        List<Map.Entry<String, Double>> orderedHeavyHitters = server.orderedHeavyHitters();
        for (int index = 0; index < DEFAULT_K / 2; index++) {
            Assert.assertEquals(
                LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_ORDERED_LIST.get(index).getKey(),
                orderedHeavyHitters.get(index).getKey()
            );
        }
    }

    static void exampleWarmupInsert(HhLdpServer server, HhLdpClient client) throws IOException {
        AtomicInteger warmupIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(LdpTestDataUtils.EXAMPLE_DATA_PATH);
        dataStream.filter(item -> warmupIndex.getAndIncrement() <= LdpTestDataUtils.EXAMPLE_WARMUP_NUM)
            .map(client::warmup)
            .forEach(server::warmupInsert);
        dataStream.close();
    }

    static void exampleRandomizeInsert(HhLdpServer server, HhLdpClient client) throws IOException {
        Random ldpRandom = new Random();
        AtomicInteger randomizedIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(LdpTestDataUtils.EXAMPLE_DATA_PATH);
        dataStream.filter(item -> randomizedIndex.getAndIncrement() > LdpTestDataUtils.EXAMPLE_WARMUP_NUM)
            .map(item -> client.randomize(server.getServerContext(), item, ldpRandom))
            .forEach(server::randomizeInsert);
        dataStream.close();
    }
}
