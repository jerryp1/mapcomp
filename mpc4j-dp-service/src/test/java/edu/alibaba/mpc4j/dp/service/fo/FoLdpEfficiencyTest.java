package edu.alibaba.mpc4j.dp.service.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.LdpTestDataUtils;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory.FoLdpType;
import edu.alibaba.mpc4j.dp.service.tool.StreamDataUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Frequency Oracle LDP efficiency test.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
@Ignore
@RunWith(Parameterized.class)
public class FoLdpEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FoLdpEfficiencyTest.class);
    /**
     * default ε
     */
    private static final double DEFAULT_EPSILON = 8;
    /**
     * server stop watch
     */
    private static final StopWatch SERVER_STOP_WATCH = new StopWatch();
    /**
     * client stop watch
     */
    private static final StopWatch CLIENT_STOP_WATCH = new StopWatch();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Hadamard Response with high ε
        configurations.add(new Object[]{FoLdpType.HR_HIGH_EPSILON.name(), FoLdpType.HR_HIGH_EPSILON,});
        // Hadamard Response with low ε
        configurations.add(new Object[]{FoLdpType.HR_LOW_EPSILON.name(), FoLdpType.HR_LOW_EPSILON,});
        // Binary Local Hash
        configurations.add(new Object[]{FoLdpType.BLH.name(), FoLdpType.BLH,});
        // RAPPOR
        configurations.add(new Object[]{FoLdpType.RAPPOR.name(), FoLdpType.RAPPOR,});
        // Optimized Unary Encoding
        configurations.add(new Object[]{FoLdpType.OUE.name(), FoLdpType.OUE,});
        // Symmetric Unary Encoding
        configurations.add(new Object[]{FoLdpType.SUE.name(), FoLdpType.SUE,});
        // Direct Encoding via Index Encoding
        configurations.add(new Object[]{FoLdpType.DE_INDEX_ENCODING.name(), FoLdpType.DE_INDEX_ENCODING,});
        // Direct Encoding via String Encoding
        configurations.add(new Object[]{FoLdpType.DE_STRING_ENCODING.name(), FoLdpType.DE_STRING_ENCODING,});

        return configurations;
    }

    /**
     * the type
     */
    private final FoLdpFactory.FoLdpType type;

    public FoLdpEfficiencyTest(String name, FoLdpFactory.FoLdpType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testEfficiency() throws IOException {
        FoLdpConfig config = FoLdpFactory.createDefaultConfig(type, LdpTestDataUtils.CONNECT_DATA_DOMAIN, DEFAULT_EPSILON);
        // create server and client
        FoLdpServer server = FoLdpFactory.createServer(config);
        FoLdpClient client = FoLdpFactory.createClient(config);
        // randomize
        SERVER_STOP_WATCH.start();
        SERVER_STOP_WATCH.suspend();
        CLIENT_STOP_WATCH.start();
        CLIENT_STOP_WATCH.suspend();
        Random ldpRandom = new Random();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(LdpTestDataUtils.CONNECT_DATA_PATH);
        long payloadBytes = dataStream
            .mapToLong(item -> {
                CLIENT_STOP_WATCH.resume();
                byte[] itemBytes = client.randomize(item, ldpRandom);
                CLIENT_STOP_WATCH.suspend();
                SERVER_STOP_WATCH.resume();
                server.insert(itemBytes);
                SERVER_STOP_WATCH.suspend();
                return itemBytes.length;
            })
            .sum();
        dataStream.close();
        Map<String, Double> estimates = server.estimate();
        double variance = LdpTestDataUtils.getVariance(estimates, LdpTestDataUtils.CORRECT_CONNECT_COUNT_MAP);
        // server time
        SERVER_STOP_WATCH.stop();
        double serverTime = (double) SERVER_STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
        SERVER_STOP_WATCH.reset();
        // client time
        CLIENT_STOP_WATCH.stop();
        double clientTime = (double) CLIENT_STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
        CLIENT_STOP_WATCH.reset();
        long memory = GraphLayout.parseInstance(server).totalSize();
        LOGGER.info("{}: variance = {}, s_time = {}, c_time = {}, byte_length = {}, memory = {}",
            type.name(), variance, serverTime, clientTime, payloadBytes, memory
        );
    }
}
