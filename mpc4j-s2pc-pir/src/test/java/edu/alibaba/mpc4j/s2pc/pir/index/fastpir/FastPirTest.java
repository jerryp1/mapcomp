package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirServer;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * FastPIR test.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class FastPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FastPirTest.class);
    /**
     * default element bit length
     */
    private static final int DEFAULT_ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * large element bit length
     */
    private static final int LARGE_ELEMENT_BIT_LENGTH = 160000;
    /**
     * small element bit length
     */
    private static final int SMALL_ELEMENT_BIT_LENGTH = CommonConstants.STATS_BIT_LENGTH;
    /**
     * database size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        Ayaa21SingleIndexPirConfig fastpirConfig = new Ayaa21SingleIndexPirConfig.Builder().build();
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.FAST_PIR.name(),
            fastpirConfig,
            new Ayaa21SingleIndexPirParams(
                4096,
                1073153L,
                new long[]{1152921504606830593L, 562949953216513L}
            )
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
     * FastPIR config
     */
    private final Ayaa21SingleIndexPirConfig indexPirConfig;
    /**
     * FastPIR params
     */
    private final Ayaa21SingleIndexPirParams indexPirParams;

    public FastPirTest(String name, Ayaa21SingleIndexPirConfig indexPirConfig, Ayaa21SingleIndexPirParams indexPirParams) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.indexPirConfig = indexPirConfig;
        this.indexPirParams = indexPirParams;
    }

    @Before
    public void connect() {
        serverRpc.connect();
        clientRpc.connect();
    }

    @After
    public void disconnect() {
        serverRpc.disconnect();
        clientRpc.disconnect();
    }

    @Test
    public void testFastPir() {
        testFastPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelFastPir() {
        testFastPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementFastPir() {
        testFastPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementFastPir() {
        testFastPir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testFastPir(Ayaa21SingleIndexPirConfig config, Ayaa21SingleIndexPirParams indexPirParams,
                            int elementBitLength, boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        Ayaa21SingleIndexPirServer server = new Ayaa21SingleIndexPirServer(serverRpc, clientRpc.ownParty(), config);
        Ayaa21SingleIndexPirClient client = new Ayaa21SingleIndexPirClient(clientRpc, serverRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        FastPirServerThread serverThread = new FastPirServerThread(server, indexPirParams, database);
        FastPirClientThread clientThread = new FastPirClientThread(
            client, indexPirParams, retrievalIndex, SERVER_ELEMENT_SIZE, elementBitLength
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
            LOGGER.info("Parameters: \n {}", indexPirParams.toString());
            // verify result
            ByteBuffer result = clientThread.getRetrievalResult();
            Assert.assertEquals(
                result, ByteBuffer.wrap(database.getBytesData(retrievalIndex))
            );
            LOGGER.info("Client: The Retrieval Result is Correct");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}