package edu.alibaba.mpc4j.dp.service.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.service.LdpTestDataUtils;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory.FoLdpType;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.tool.StreamDataUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Frequency Oracle LDP test.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
@RunWith(Parameterized.class)
public class FoLdpTest {
    /**
     * large ε
     */
    static final double LARGE_EPSILON = 128;
    /**
     * default ε
     */
    static final double DEFAULT_EPSILON = 16;
    /**
     * large ε absolute precision
     */
    private static final double LARGE_EPSILON_ABS_PRECISION
        = (double) LdpTestDataUtils.EXAMPLE_TOTAL_NUM / LdpTestDataUtils.EXAMPLE_DATA_D;
    /**
     * default ε variance precision
     */
    private static final double DEFAULT_EPSILON_VARIANCE_PRECISION = 5;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RAPPOR
        configurations.add(new Object[]{FoLdpType.RAPPOR.name(), FoLdpType.RAPPOR,});
        // OPTIMIZED_UNARY_ENCODING
        configurations.add(new Object[]{FoLdpType.OUE.name(), FoLdpType.OUE,});
        // SYMMETRIC_UNARY_ENCODING
        configurations.add(new Object[]{FoLdpType.SUE.name(), FoLdpType.SUE,});
        // DE_INDEX_ENCODING
        configurations.add(new Object[]{FoLdpType.DE_INDEX_ENCODING.name(), FoLdpType.DE_INDEX_ENCODING,});
        // DE_STRING_ENCODING
        configurations.add(new Object[]{FoLdpType.DE_STRING_ENCODING.name(), FoLdpType.DE_STRING_ENCODING,});

        return configurations;
    }

    /**
     * the type
     */
    private final FoLdpType type;

    public FoLdpTest(String name, FoLdpType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testType() {
        FoLdpConfig config = FoLdpFactory.createDefaultConfig(type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, DEFAULT_EPSILON);
        // create server
        FoLdpServer server = FoLdpFactory.createServer(config);
        Assert.assertEquals(type, server.getType());
        // create client
        FoLdpClient client = FoLdpFactory.createClient(config);
        Assert.assertEquals(type, client.getType());
    }

    @Test
    public void testLargeEpsilon() throws IOException {
        FoLdpConfig config = FoLdpFactory.createDefaultConfig(type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, LARGE_EPSILON);
        // create server and client
        FoLdpServer server = FoLdpFactory.createServer(config);
        FoLdpClient client = FoLdpFactory.createClient(config);
        // randomize
        exampleRandomizeInsert(server, client);
        Map<String, Double> frequencyEstimates = server.estimate();
        Assert.assertEquals(LdpTestDataUtils.EXAMPLE_DATA_D, frequencyEstimates.size());
        if (FoLdpFactory.isConverge(type)) {
            for (String item : LdpTestDataUtils.EXAMPLE_DATA_DOMAIN) {
                // verify no-error count
                Assert.assertEquals(
                    LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_MAP.get(item), frequencyEstimates.get(item), DoubleUtils.PRECISION
                );
            }
        } else {
            // there are some mechanisms that do not get accurate answer even for large epsilon
            for (String item : LdpTestDataUtils.EXAMPLE_DATA_DOMAIN) {
                int correct = LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_MAP.get(item);
                double estimate = frequencyEstimates.get(item);
                // verify bounded error
                Assert.assertTrue(Math.abs(correct - estimate) <= LARGE_EPSILON_ABS_PRECISION);
            }
        }
    }

    @Test
    public void testDefault() throws IOException {
        FoLdpConfig config = FoLdpFactory.createDefaultConfig(type, LdpTestDataUtils.EXAMPLE_DATA_DOMAIN, DEFAULT_EPSILON);
        // create server and client
        FoLdpServer server = FoLdpFactory.createServer(config);
        FoLdpClient client = FoLdpFactory.createClient(config);
        // randomize
        exampleRandomizeInsert(server, client);
        Map<String, Double> frequencyEstimates = server.estimate();
        Assert.assertEquals(LdpTestDataUtils.EXAMPLE_DATA_D, frequencyEstimates.size());
        // compute the variance
        int totalNum = LdpTestDataUtils.EXAMPLE_TOTAL_NUM;
        double variance = LdpTestDataUtils.getVariance(frequencyEstimates, LdpTestDataUtils.CORRECT_EXAMPLE_COUNT_MAP);
        double averageVariance = variance / totalNum;
        Assert.assertTrue(averageVariance <= DEFAULT_EPSILON_VARIANCE_PRECISION);
    }

    private static void exampleRandomizeInsert(FoLdpServer server, FoLdpClient client) throws IOException {
        Random ldpRandom = new Random();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(LdpTestDataUtils.EXAMPLE_DATA_PATH);
        dataStream.map(item -> client.randomize(item, ldpRandom)).forEach(server::insert);
        dataStream.close();
    }
}
