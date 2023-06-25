package edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * AAAG22 keyword PIR test.
 *
 * @author Liqiang Peng
 * @date 2023/6/20
 */
@RunWith(Parameterized.class)
public class Aaag22KwPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aaag22KwPirTest.class);
    /**
     * repeat time
     */
    private static final int REPEAT_TIME = 1;
    /**
     * short label byte length
     */
    private static final int SHORT_LABEL_BYTE_LENGTH = 5;
    /**
     * default label byte length
     */
    private static final int DEFAULT_LABEL_BYTE_LENGTH = 8;
    /**
     * long label byte length
     */
    private static final int LONG_LABEL_BYTE_LENGTH = 128;
    /**
     * server element size
     */
    private static final int SERVER_MAP_SIZE = (1 << 18) + 5;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            KwPirFactory.KwPirType.CMG21.name(), new Aaag22KwPirConfig.Builder().build(),
            Aaag22KwPirParams.DEFAULT_PARAMS
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
     * AAAG22 keyword PIR config
     */
    private final Aaag22KwPirConfig config;
    /**
     * AAAG22 keyword PIR params
     */
    private final Aaag22KwPirParams params;

    public Aaag22KwPirTest(String name, Aaag22KwPirConfig config, Aaag22KwPirParams params) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
        this.params = params;
    }

    @Test
    public void testShortLabelParallel() {
        testPir(params, SHORT_LABEL_BYTE_LENGTH, config, true);
    }

    @Test
    public void testDefaultLabelParallel() {
        testPir(params, DEFAULT_LABEL_BYTE_LENGTH, config, true);
    }

    @Test
    public void testLongLabelParallel() {
        testPir(params, LONG_LABEL_BYTE_LENGTH, config, true);
    }

    public void testPir(Aaag22KwPirParams kwPirParams, int labelByteLength, Aaag22KwPirConfig config, boolean parallel) {
        int retrievalSize = kwPirParams.maxRetrievalSize();
        List<Set<String>> randomSets = PirUtils.generateStringSets(SERVER_MAP_SIZE, retrievalSize, REPEAT_TIME);
        Map<String, ByteBuffer> keywordLabelMap = PirUtils.generateKeywordLabelMap(randomSets.get(0), labelByteLength);
        // create instances
        Aaag22KwPirServer<String> server = new Aaag22KwPirServer<>(serverRpc, clientRpc.ownParty(), config);
        Aaag22KwPirClient<String> client = new Aaag22KwPirClient<>(clientRpc, serverRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        Aaag22KwPirServerThread<String> serverThread = new Aaag22KwPirServerThread<>(
            server, kwPirParams, keywordLabelMap, labelByteLength, REPEAT_TIME
        );
        Aaag22KwPirClientThread<String> clientThread = new Aaag22KwPirClientThread<>(
            client, kwPirParams, Lists.newArrayList(randomSets.subList(1, REPEAT_TIME + 1)), labelByteLength
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
                pirResult.forEach((key, value) -> {
                    Assert.assertEquals(value, keywordLabelMap.get(key));
                    LOGGER.info("key {}, value{}", key, Arrays.toString(value.array()));
                });
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}


