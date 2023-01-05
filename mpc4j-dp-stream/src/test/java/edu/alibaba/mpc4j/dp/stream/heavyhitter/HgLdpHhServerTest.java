package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory.LdpHhType;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.HgLdpHhClientConfig;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.HgLdpHhServerConfig;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.hg.HgLdpHhClient;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.hg.HgLdpHhServer;
import edu.alibaba.mpc4j.dp.stream.structure.HeavyGuardian;
import edu.alibaba.mpc4j.dp.stream.tool.StreamDataUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * tests for Heavy Hitter with Local Differential Privacy based on HeavyGuardian.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
@RunWith(Parameterized.class)
public class HgLdpHhServerTest {
    /**
     * HeavyGuardian seed
     */
    private static final long HEAVY_GUARDIAN_SEED = 1234567890L;
    /**
     * w = 1
     */
    private static final int W1 = 1;
    /**
     * λ_h for w = 1
     */
    private static final int W1_LAMBDA_H = (int) Math.ceil((double) LdpHhServerTest.DEFAULT_K / W1);
    /**
     * w = 2
     */
    private static final int W2 = 2;
    /**
     * λ_h for w = 2
     */
    private static final int W2_LAMBDA_H = (int) Math.ceil((double) LdpHhServerTest.DEFAULT_K / W2);
    /**
     * w = 3
     */
    private static final int W3 = 3;
    /**
     * λ_h for w = 3
     */
    private static final int W3_LAMBDA_H = (int) Math.ceil((double) LdpHhServerTest.DEFAULT_K / W3);
    /**
     * correct HeavyGuardian count ordered list for stream_counter_example_data.txt with w = 1
     */
    private static final List<Map.Entry<String, Integer>> CORRECT_W1_HG_EXAMPLE_COUNT_ORDERED_LIST;
    /**
     * correct HeavyGuardian count ordered list for stream_counter_example_data.txt with w = 2
     */
    private static final List<Map.Entry<String, Integer>> CORRECT_W2_HG_EXAMPLE_COUNT_ORDERED_LIST;
    /**
     * correct HeavyGuardian count ordered list for stream_counter_example_data.txt with w = 3
     */
    private static final List<Map.Entry<String, Integer>> CORRECT_W3_HG_EXAMPLE_COUNT_ORDERED_LIST;

    static {
        try {
            Random random = new Random();
            // w = 1
            random.setSeed(HEAVY_GUARDIAN_SEED);
            HeavyGuardian w1HeavyGuardian = new HeavyGuardian(W1, W1_LAMBDA_H, 0, random);
            Stream<String> w1DataStream = StreamDataUtils.obtainItemStream(LdpHhServerTest.EXAMPLE_DATA_PATH);
            w1DataStream.forEach(w1HeavyGuardian::insert);
            w1DataStream.close();
            Map<String, Integer> correctW1CountMap = w1HeavyGuardian.getRecordItemSet().stream()
                .collect(Collectors.toMap(item -> item, w1HeavyGuardian::query));
            CORRECT_W1_HG_EXAMPLE_COUNT_ORDERED_LIST = new ArrayList<>(correctW1CountMap.entrySet());
            CORRECT_W1_HG_EXAMPLE_COUNT_ORDERED_LIST.sort(Comparator.comparingInt(Map.Entry::getValue));
            Collections.reverse(CORRECT_W1_HG_EXAMPLE_COUNT_ORDERED_LIST);
            // w = 2
            random.setSeed(HEAVY_GUARDIAN_SEED);
            HeavyGuardian w2HeavyGuardian = new HeavyGuardian(W2, W2_LAMBDA_H, 0, random);
            Stream<String> w2DataStream = StreamDataUtils.obtainItemStream(LdpHhServerTest.EXAMPLE_DATA_PATH);
            w2DataStream.forEach(w2HeavyGuardian::insert);
            w2DataStream.close();
            Map<String, Integer> correctW2CountMap = w2HeavyGuardian.getRecordItemSet().stream()
                .collect(Collectors.toMap(item -> item, w2HeavyGuardian::query));
            CORRECT_W2_HG_EXAMPLE_COUNT_ORDERED_LIST = new ArrayList<>(correctW2CountMap.entrySet());
            CORRECT_W2_HG_EXAMPLE_COUNT_ORDERED_LIST.sort(Comparator.comparingInt(Map.Entry::getValue));
            Collections.reverse(CORRECT_W2_HG_EXAMPLE_COUNT_ORDERED_LIST);
            // w = 3
            random.setSeed(HEAVY_GUARDIAN_SEED);
            HeavyGuardian w3HeavyGuardian = new HeavyGuardian(W3, W3_LAMBDA_H, 0, random);
            Stream<String> w3DataStream = StreamDataUtils.obtainItemStream(LdpHhServerTest.EXAMPLE_DATA_PATH);
            w3DataStream.forEach(w3HeavyGuardian::insert);
            w3DataStream.close();
            Map<String, Integer> correctW3CountMap = w3HeavyGuardian.getRecordItemSet().stream()
                .collect(Collectors.toMap(item -> item, w3HeavyGuardian::query));
            CORRECT_W3_HG_EXAMPLE_COUNT_ORDERED_LIST = new ArrayList<>(correctW3CountMap.entrySet());
            // descending sort
            CORRECT_W3_HG_EXAMPLE_COUNT_ORDERED_LIST.sort(Comparator.comparingInt(Map.Entry::getValue));
            Collections.reverse(CORRECT_W3_HG_EXAMPLE_COUNT_ORDERED_LIST);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // related heavy guardian
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

        return configurations;
    }

    /**
     * the type
     */
    private final LdpHhType type;

    public HgLdpHhServerTest(String name, LdpHhType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testW1Warmup() throws IOException {
        Random hgRandom = new Random(HEAVY_GUARDIAN_SEED);
        HgLdpHhServerConfig serverConfig = new HgLdpHhServerConfig
            .Builder(type, LdpHhServerTest.EXAMPLE_D, LdpHhServerTest.DEFAULT_K, LdpHhServerTest.DEFAULT_EPSILON)
            .setBucketParams(W1, W1_LAMBDA_H)
            .setRandom(hgRandom)
            .build();
        HgLdpHhServer server = LdpHhFactory.createHgServer(serverConfig);
        testWarmup(server, CORRECT_W1_HG_EXAMPLE_COUNT_ORDERED_LIST);
    }

    @Test
    public void testW2Warmup() throws IOException {
        Random hgRandom = new Random(HEAVY_GUARDIAN_SEED);
        HgLdpHhServerConfig serverConfig = new HgLdpHhServerConfig
            .Builder(type, LdpHhServerTest.EXAMPLE_D, LdpHhServerTest.DEFAULT_K, LdpHhServerTest.DEFAULT_EPSILON)
            .setBucketParams(W2, W2_LAMBDA_H)
            .setRandom(hgRandom)
            .build();
        HgLdpHhServer server = LdpHhFactory.createHgServer(serverConfig);
        testWarmup(server, CORRECT_W2_HG_EXAMPLE_COUNT_ORDERED_LIST);
    }

    @Test
    public void testW3Warmup() throws IOException {
        Random hgRandom = new Random(HEAVY_GUARDIAN_SEED);
        HgLdpHhServerConfig serverConfig = new HgLdpHhServerConfig
            .Builder(type, LdpHhServerTest.EXAMPLE_D, LdpHhServerTest.DEFAULT_K, LdpHhServerTest.DEFAULT_EPSILON)
            .setBucketParams(W3, W3_LAMBDA_H)
            .setRandom(hgRandom)
            .build();
        HgLdpHhServer server = LdpHhFactory.createHgServer(serverConfig);
        testWarmup(server, CORRECT_W3_HG_EXAMPLE_COUNT_ORDERED_LIST);
    }

    private void testWarmup(HgLdpHhServer server, List<Map.Entry<String, Integer>> correctOrderedList)
        throws IOException {
        // warmup
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(LdpHhServerTest.EXAMPLE_DATA_PATH);
        dataStream.forEach(server::warmupInsert);
        dataStream.close();
        // get heavy hitters
        Map<String, Double> heavyHitterMap = server.responseHeavyHitters();
        Assert.assertEquals(LdpHhServerTest.DEFAULT_K, heavyHitterMap.size());
        List<Map.Entry<String, Double>> orderedHeavyHitterList = server.responseOrderedHeavyHitters();
        Assert.assertEquals(LdpHhServerTest.DEFAULT_K, orderedHeavyHitterList.size());
        // verify no-error count
        for (int index = 0; index < LdpHhServerTest.DEFAULT_K; index++) {
            Assert.assertEquals(correctOrderedList.get(index).getKey(), orderedHeavyHitterList.get(index).getKey());
        }
    }

    @Test
    public void testW1StopWarmup() throws IOException {
        Random hgRandom = new Random(HEAVY_GUARDIAN_SEED);
        HgLdpHhServerConfig serverConfig = new HgLdpHhServerConfig
            .Builder(type, LdpHhServerTest.EXAMPLE_D, LdpHhServerTest.DEFAULT_K, LdpHhServerTest.DEFAULT_EPSILON)
            .setBucketParams(W1, W1_LAMBDA_H)
            .setRandom(hgRandom)
            .build();
        HgLdpHhServer server = LdpHhFactory.createHgServer(serverConfig);
        testStopWarmup(server, CORRECT_W1_HG_EXAMPLE_COUNT_ORDERED_LIST);
    }

    @Test
    public void testW2StopWarmup() throws IOException {
        Random hgRandom = new Random(HEAVY_GUARDIAN_SEED);
        HgLdpHhServerConfig serverConfig = new HgLdpHhServerConfig
            .Builder(type, LdpHhServerTest.EXAMPLE_D, LdpHhServerTest.DEFAULT_K, LdpHhServerTest.DEFAULT_EPSILON)
            .setBucketParams(W2, W2_LAMBDA_H)
            .setRandom(hgRandom)
            .build();
        HgLdpHhServer server = LdpHhFactory.createHgServer(serverConfig);
        testStopWarmup(server, CORRECT_W2_HG_EXAMPLE_COUNT_ORDERED_LIST);
    }

    @Test
    public void testW3StopWarmup() throws IOException {
        Random hgRandom = new Random(HEAVY_GUARDIAN_SEED);
        HgLdpHhServerConfig serverConfig = new HgLdpHhServerConfig
            .Builder(type, LdpHhServerTest.EXAMPLE_D, LdpHhServerTest.DEFAULT_K, LdpHhServerTest.DEFAULT_EPSILON)
            .setBucketParams(W3, W3_LAMBDA_H)
            .setRandom(hgRandom)
            .build();
        HgLdpHhServer server = LdpHhFactory.createHgServer(serverConfig);
        testStopWarmup(server, CORRECT_W3_HG_EXAMPLE_COUNT_ORDERED_LIST);
    }

    private void testStopWarmup(HgLdpHhServer hgLdpHeavyHitter, List<Map.Entry<String, Integer>> correctOrderedList)
        throws IOException {
        // warmup
        StreamDataUtils.obtainItemStream(LdpHhServerTest.EXAMPLE_DATA_PATH).forEach(hgLdpHeavyHitter::warmupInsert);
        hgLdpHeavyHitter.stopWarmup();
        // get heavy hitters
        Map<String, Double> heavyHitterMap = hgLdpHeavyHitter.responseHeavyHitters();
        Assert.assertEquals(LdpHhServerTest.DEFAULT_K, heavyHitterMap.size());
        List<Map.Entry<String, Double>> orderedHeavyHitterList = hgLdpHeavyHitter.responseOrderedHeavyHitters();
        Assert.assertEquals(LdpHhServerTest.DEFAULT_K, orderedHeavyHitterList.size());
        // verify no-error count
        for (int index = 0; index < LdpHhServerTest.DEFAULT_K; index++) {
            Assert.assertEquals(correctOrderedList.get(index).getKey(), orderedHeavyHitterList.get(index).getKey());
        }
    }

    @Test
    public void testW1LargeEpsilon() throws IOException {
        Random hgRandom = new Random(HEAVY_GUARDIAN_SEED);
        HgLdpHhServerConfig serverConfig = new HgLdpHhServerConfig
            .Builder(type, LdpHhServerTest.EXAMPLE_D, LdpHhServerTest.DEFAULT_K, LdpHhServerTest.LARGE_EPSILON)
            .setBucketParams(W1, W1_LAMBDA_H)
            .setRandom(hgRandom)
            .build();
        HgLdpHhServer server = LdpHhFactory.createHgServer(serverConfig);
        HgLdpHhClientConfig clientConfig = new HgLdpHhClientConfig
            .Builder(type, LdpHhServerTest.EXAMPLE_DATA_DOMAIN, LdpHhServerTest.DEFAULT_K, LdpHhServerTest.LARGE_EPSILON)
            .setBucketParams(W1, W1_LAMBDA_H)
            .build();
        HgLdpHhClient client = LdpHhFactory.createHgClient(clientConfig);
        testLargeEpsilon(server, client, CORRECT_W1_HG_EXAMPLE_COUNT_ORDERED_LIST);
    }

    @Test
    public void testW2LargeEpsilon() throws IOException {
        Random hgRandom = new Random(HEAVY_GUARDIAN_SEED);
        HgLdpHhServerConfig serverConfig = new HgLdpHhServerConfig
            .Builder(type, LdpHhServerTest.EXAMPLE_D, LdpHhServerTest.DEFAULT_K, LdpHhServerTest.LARGE_EPSILON)
            .setBucketParams(W2, W2_LAMBDA_H)
            .setRandom(hgRandom)
            .build();
        HgLdpHhServer server = LdpHhFactory.createHgServer(serverConfig);
        HgLdpHhClientConfig clientConfig = new HgLdpHhClientConfig
            .Builder(type, LdpHhServerTest.EXAMPLE_DATA_DOMAIN, LdpHhServerTest.DEFAULT_K, LdpHhServerTest.LARGE_EPSILON)
            .setBucketParams(W2, W2_LAMBDA_H)
            .build();
        HgLdpHhClient client = LdpHhFactory.createHgClient(clientConfig);
        testLargeEpsilon(server, client, CORRECT_W2_HG_EXAMPLE_COUNT_ORDERED_LIST);
    }

    @Test
    public void testW3LargeEpsilon() throws IOException {
        Random hgRandom = new Random(HEAVY_GUARDIAN_SEED);
        HgLdpHhServerConfig serverConfig = new HgLdpHhServerConfig
            .Builder(type, LdpHhServerTest.EXAMPLE_D, LdpHhServerTest.DEFAULT_K, LdpHhServerTest.LARGE_EPSILON)
            .setBucketParams(W3, W3_LAMBDA_H)
            .setRandom(hgRandom)
            .build();
        HgLdpHhServer server = LdpHhFactory.createHgServer(serverConfig);
        HgLdpHhClientConfig clientConfig = new HgLdpHhClientConfig
            .Builder(type, LdpHhServerTest.EXAMPLE_DATA_DOMAIN, LdpHhServerTest.DEFAULT_K, LdpHhServerTest.LARGE_EPSILON)
            .setBucketParams(W3, W3_LAMBDA_H)
            .build();
        HgLdpHhClient client = LdpHhFactory.createHgClient(clientConfig);
        testLargeEpsilon(server, client, CORRECT_W3_HG_EXAMPLE_COUNT_ORDERED_LIST);
    }

    private void testLargeEpsilon(HgLdpHhServer server, HgLdpHhClient client,
                                  List<Map.Entry<String, Integer>> correctOrderedList) throws IOException {
        // warmup
        LdpHhServerTest.exampleWarmupInsert(server);
        server.stopWarmup();
        // randomize
        LdpHhServerTest.exampleRandomizeInsert(server, client);
        // get heavy hitters
        Map<String, Double> heavyHitterMap = server.responseHeavyHitters();
        Assert.assertEquals(LdpHhServerTest.DEFAULT_K, heavyHitterMap.size());
        List<Map.Entry<String, Double>> orderedHeavyHitterList = server.responseOrderedHeavyHitters();
        Assert.assertEquals(LdpHhServerTest.DEFAULT_K, orderedHeavyHitterList.size());
        // verify no-error count
        for (int index = 0; index < LdpHhServerTest.DEFAULT_K; index++) {
            Assert.assertEquals(correctOrderedList.get(index).getKey(), orderedHeavyHitterList.get(index).getKey());
        }
    }
}
