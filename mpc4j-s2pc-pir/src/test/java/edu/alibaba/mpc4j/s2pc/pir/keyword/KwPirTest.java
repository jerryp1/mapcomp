package edu.alibaba.mpc4j.s2pc.pir.keyword;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;

/**
 * keyword PIR test.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
@RunWith(Parameterized.class)
public class KwPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(KwPirTest.class);
    /**
     * repeat time
     */
    private static final int REPEAT_TIME = 1;
    /**
     * default label byte length
     */
    private static final int DEFAULT_LABEL_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * server element size
     */
    private static final int SERVER_MAP_SIZE = 1 << 18;
    /**
     * small client element size
     */
    private static final int SMALL_CLIENT_SET_SIZE = 1;
    /**
     * default client element size
     */
    private static final int DEFAULT_CLIENT_SET_SIZE = 1 << 8;
    /**
     * large client element size
     */
    private static final int LARGE_CLIENT_SET_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CMG21
        configurations.add(new Object[]{
            KwPirFactory.KwPirType.CMG21.name(), new Cmg21KwPirConfig.Builder().build()
        });

        return configurations;
    }

    /**
     * server rpc
     */
    private final Rpc serverRpc;
    /**
     * client rpc
     */
    private final Rpc clientRpc;
    /**
     * keyword PIR config
     */
    private final KwPirConfig config;

    public KwPirTest(String name, KwPirConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testSmallRetrievalSizeParallel() {
        testPir(SMALL_CLIENT_SET_SIZE, config, true);
    }

    @Test
    public void test() {
        System.out.println(new SecureRandom().nextLong());
    }

    @Test
    public void testDefaultRetrievalSizeParallel() {
        testPir(DEFAULT_CLIENT_SET_SIZE, config, true);
    }

    @Test
    public void testLargeRetrievalSizeParallel() {
        testPir(LARGE_CLIENT_SET_SIZE, config, true);
    }

    public void testPir(int retrievalSize, KwPirConfig config, boolean parallel) {
        List<Set<String>> randomSets = PirUtils.generateStringSets(SERVER_MAP_SIZE, retrievalSize, REPEAT_TIME);
        Map<String, ByteBuffer> keywordLabelMap = PirUtils.generateKeywordLabelMap(
            randomSets.get(0), DEFAULT_LABEL_BYTE_LENGTH
        );
        // create instances
        KwPirServer<String> server = KwPirFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        KwPirClient<String> client = KwPirFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        KwPirServerThread<String> serverThread = new KwPirServerThread<>(
            server, keywordLabelMap, retrievalSize, DEFAULT_LABEL_BYTE_LENGTH, REPEAT_TIME
        );
        KwPirClientThread<String> clientThread = new KwPirClientThread<>(
            client, Lists.newArrayList(randomSets.subList(1, REPEAT_TIME + 1)), retrievalSize, DEFAULT_LABEL_BYTE_LENGTH
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            LOGGER.info("Server: The Communication costs {}MB", serverRpc.getSendByteLength() * 1.0 / (1024 * 1024));
            serverRpc.reset();
            LOGGER.info("Client: The Communication costs {}MB", clientRpc.getSendByteLength() * 1.0 / (1024 * 1024));
            clientRpc.reset();
            // verify result
            for (int index = 0; index < REPEAT_TIME; index++) {
                Set<String> intersectionSet = new HashSet<>(randomSets.get(index + 1));
                intersectionSet.retainAll(randomSets.get(0));
                Map<String, ByteBuffer> pirResult = clientThread.getRetrievalResult(index);
                Assert.assertEquals(intersectionSet.size(), pirResult.size());
                pirResult.forEach((key, value) -> Assert.assertEquals(value, keywordLabelMap.get(key)));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}