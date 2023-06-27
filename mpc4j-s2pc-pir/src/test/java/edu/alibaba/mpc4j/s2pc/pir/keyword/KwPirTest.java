package edu.alibaba.mpc4j.s2pc.pir.keyword;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22.Aaag22KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
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
    private static final int SERVER_MAP_SIZE = 1 << 18;
    /**
     * client element size
     */
    private static final int CLIENT_SET_SIZE = 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CMG21
        configurations.add(new Object[]{
            KwPirFactory.KwPirType.CMG21.name(), new Cmg21KwPirConfig.Builder().build()
        });
        // AAAG22
        configurations.add(new Object[]{
            KwPirFactory.KwPirType.AAAG22.name(), new Aaag22KwPirConfig.Builder().build()
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
    public void testShortLabelParallel() {
        testPir(SHORT_LABEL_BYTE_LENGTH, config, true);
    }

    @Test
    public void testDefaultLabelParallel() {
        testPir(DEFAULT_LABEL_BYTE_LENGTH, config, true);
    }

    @Test
    public void testLongLabelParallel() {
        testPir(LONG_LABEL_BYTE_LENGTH, config, true);
    }

    public void testPir(int labelByteLength, KwPirConfig config, boolean parallel) {
        List<Set<ByteBuffer>> randomSets = PirUtils.generateByteBufferSets(SERVER_MAP_SIZE, CLIENT_SET_SIZE, REPEAT_TIME);
        Map<ByteBuffer, ByteBuffer> keywordLabelMap = PirUtils.generateKeywordByteBufferLabelMap(
            randomSets.get(0), labelByteLength
        );
        // create instances
        KwPirServer server = KwPirFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        KwPirClient client = KwPirFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        KwPirServerThread serverThread = new KwPirServerThread(
            server, keywordLabelMap, CLIENT_SET_SIZE, labelByteLength, REPEAT_TIME
        );
        KwPirClientThread clientThread = new KwPirClientThread(
            client, Lists.newArrayList(randomSets.subList(1, REPEAT_TIME + 1)), CLIENT_SET_SIZE, SERVER_MAP_SIZE, labelByteLength
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