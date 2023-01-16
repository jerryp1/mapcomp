package edu.alibaba.mpc4j.dp.service.heavyhitter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.LdpTestDataUtils;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.BasicHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
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
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Heavy Hitter LDP efficiency test.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
@RunWith(Parameterized.class)
public class HhLdpEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HhLdpEfficiencyTest.class);
    /**
     * default ε
     */
    private static final double DEFAULT_EPSILON = 16;
    /**
     * default k
     */
    private static final int DEFAULT_K = 20;
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

        // relaxed heavy guardian
        configurations.add(new Object[]{
            HhLdpFactory.HhLdpType.RELAX_HG.name(), HhLdpFactory.HhLdpType.RELAX_HG,
        });
        // advanced heavy guardian
        configurations.add(new Object[]{
            HhLdpFactory.HhLdpType.ADVAN_HG.name(), HhLdpFactory.HhLdpType.ADVAN_HG,
        });
        // basic heavy guardian
        configurations.add(new Object[]{
            HhLdpFactory.HhLdpType.BASIC_HG.name(), HhLdpFactory.HhLdpType.BASIC_HG,
        });
        // NAIVE
        configurations.add(new Object[]{
            HhLdpFactory.HhLdpType.DE_FO.name(), HhLdpFactory.HhLdpType.DE_FO,
        });

        return configurations;
    }

    /**
     * the type
     */
    private final HhLdpFactory.HhLdpType type;

    public HhLdpEfficiencyTest(String name, HhLdpFactory.HhLdpType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testEfficiency() throws IOException {
        HhLdpConfig config = new BasicHhLdpConfig
            .Builder(type, LdpTestDataUtils.CONNECT_DATA_DOMAIN, DEFAULT_K, DEFAULT_EPSILON)
            .build();
        // create server and client
        HhLdpServer server = HhLdpFactory.createServer(config);
        HhLdpClient client = HhLdpFactory.createClient(config);
        // warmup
        AtomicInteger warmupIndex = new AtomicInteger();
        Stream<String> dataStream = StreamDataUtils.obtainItemStream(LdpTestDataUtils.CONNECT_DATA_PATH);
        dataStream.filter(item -> warmupIndex.getAndIncrement() <= LdpTestDataUtils.CONNECT_WARMUP_NUM)
            .map(client::warmup)
            .forEach(server::warmupInsert);
        dataStream.close();
        server.stopWarmup();
        // randomize
        SERVER_STOP_WATCH.start();
        SERVER_STOP_WATCH.suspend();
        CLIENT_STOP_WATCH.start();
        CLIENT_STOP_WATCH.suspend();
        Random ldpRandom = new Random();
        AtomicInteger randomizedIndex = new AtomicInteger();
        dataStream = StreamDataUtils.obtainItemStream(LdpTestDataUtils.CONNECT_DATA_PATH);
        long itemByteLength = dataStream
            .filter(item -> randomizedIndex.getAndIncrement() > LdpTestDataUtils.CONNECT_WARMUP_NUM)
            .mapToLong(item -> {
                CLIENT_STOP_WATCH.resume();
                byte[] itemBytes = client.randomize(server.getServerContext(), item, ldpRandom);
                CLIENT_STOP_WATCH.suspend();
                SERVER_STOP_WATCH.resume();
                server.randomizeInsert(itemBytes);
                SERVER_STOP_WATCH.suspend();
                return itemBytes.length;
            })
            .sum();
        dataStream.close();
        // server time
        SERVER_STOP_WATCH.stop();
        double serverTime = (double) SERVER_STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
        SERVER_STOP_WATCH.reset();
        // client time
        CLIENT_STOP_WATCH.stop();
        double clientTime = (double) CLIENT_STOP_WATCH.getTime(TimeUnit.MILLISECONDS) / 1000;
        CLIENT_STOP_WATCH.reset();
        long memory = GraphLayout.parseInstance(server).totalSize();
        LOGGER.info("{}: k = {}, d = {}, s_time = {}, c_time = {}, byte_length = {}, memory = {}",
            type.name(), DEFAULT_K, LdpTestDataUtils.CONNECT_DATA_D,
            serverTime,  clientTime, itemByteLength, memory
        );
    }
}
