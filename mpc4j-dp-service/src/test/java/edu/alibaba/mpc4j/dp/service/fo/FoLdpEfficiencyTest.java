package edu.alibaba.mpc4j.dp.service.fo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.LdpTestDataUtils;
import edu.alibaba.mpc4j.dp.service.fo.config.BasicFoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.tool.StreamDataUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
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

        // OPTIMIZED_UNARY_ENCODING
        configurations.add(new Object[]{
            FoLdpFactory.FoLdpType.OUE.name(), FoLdpFactory.FoLdpType.OUE,
        });
        // SYMMETRIC_UNARY_ENCODING
        configurations.add(new Object[]{
            FoLdpFactory.FoLdpType.SUE.name(), FoLdpFactory.FoLdpType.SUE,
        });
        // DE_INDEX_ENCODING
        configurations.add(new Object[]{
            FoLdpFactory.FoLdpType.DE_INDEX_ENCODING.name(), FoLdpFactory.FoLdpType.DE_INDEX_ENCODING,
        });
        // DE_STRING_ENCODING
        configurations.add(new Object[]{
            FoLdpFactory.FoLdpType.DE_STRING_ENCODING.name(), FoLdpFactory.FoLdpType.DE_STRING_ENCODING,
        });

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
        FoLdpConfig config = new BasicFoLdpConfig
            .Builder(type, LdpTestDataUtils.CONNECT_DATA_DOMAIN, DEFAULT_EPSILON)
            .build();
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
