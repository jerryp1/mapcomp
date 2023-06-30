package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.*;
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
 * CMG21 keyword PIR test.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
@RunWith(Parameterized.class)
public class Cmg21KwPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Cmg21KwPirTest.class);
    /**
     * repeat time
     */
    private static final int REPEAT_TIME = 1;
    /**
     * short label byte length
     */
    private static final int SHORT_LABEL_BYTE_LENGTH = CommonConstants.STATS_BYTE_LENGTH;
    /**
     * default label byte length
     */
    private static final int DEFAULT_LABEL_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * long label byte length
     */
    private static final int LONG_LABEL_BYTE_LENGTH = CommonConstants.STATS_BIT_LENGTH;
    /**
     * server element size
     */
    private static final int SERVER_MAP_SIZE = 1 << 20;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CMG21
        configurations.add(new Object[]{
            KwPirFactory.KwPirType.CMG21.name() + " max client set size 1", new Cmg21KwPirConfig.Builder().build(),
            Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_1
        });
        configurations.add(new Object[]{
            KwPirFactory.KwPirType.CMG21.name() + " max client set size 4096", new Cmg21KwPirConfig.Builder().build(),
            Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_4096
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
     * CMG21 keyword PIR config
     */
    private final Cmg21KwPirConfig config;
    /**
     * CMG21 keyword PIR params
     */
    private final Cmg21KwPirParams params;

    public Cmg21KwPirTest(String name, Cmg21KwPirConfig config, Cmg21KwPirParams params) {
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

    public void testPir(Cmg21KwPirParams kwPirParams, int labelByteLength, Cmg21KwPirConfig config, boolean parallel) {
        int retrievalSize = kwPirParams.maxRetrievalSize();
        List<Set<ByteBuffer>> randomSets = PirUtils.generateByteBufferSets(SERVER_MAP_SIZE, retrievalSize, REPEAT_TIME);
        Map<ByteBuffer, ByteBuffer> keywordLabelMap = PirUtils.generateKeywordByteBufferLabelMap(randomSets.get(0), labelByteLength);
        // create instances
        Cmg21KwPirServer server = new Cmg21KwPirServer(serverRpc, clientRpc.ownParty(), config);
        Cmg21KwPirClient client = new Cmg21KwPirClient(clientRpc, serverRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        Cmg21KwPirServerThread serverThread = new Cmg21KwPirServerThread(
            server, kwPirParams, keywordLabelMap, labelByteLength, REPEAT_TIME
        );
        Cmg21KwPirClientThread clientThread = new Cmg21KwPirClientThread(
            client, kwPirParams, Lists.newArrayList(randomSets.subList(1, REPEAT_TIME + 1)), SERVER_MAP_SIZE, labelByteLength
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            serverRpc.reset();
            clientRpc.reset();
            // verify result
            for (int index = 0; index < REPEAT_TIME; index++) {
                Set<ByteBuffer> intersectionSet = new HashSet<>(randomSets.get(index + 1));
                intersectionSet.retainAll(randomSets.get(0));
                Map<ByteBuffer, ByteBuffer> pirResult = clientThread.getRetrievalResult(index);
                Assert.assertEquals(intersectionSet.size(), pirResult.size());
                pirResult.forEach((key, value) -> {
                    Assert.assertEquals(value, keywordLabelMap.get(key));
                    LOGGER.info("key {}, value{}", Arrays.toString(key.array()), Arrays.toString(value.array()));
                });
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}


