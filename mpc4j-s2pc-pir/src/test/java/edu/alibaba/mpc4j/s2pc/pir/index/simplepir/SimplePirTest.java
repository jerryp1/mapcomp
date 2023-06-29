package edu.alibaba.mpc4j.s2pc.pir.index.simplepir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.*;
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
 * Simple PIR test.
 *
 * @author Liqiang Peng
 * @date 2023/5/29
 */
@RunWith(Parameterized.class)
public class SimplePirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePirTest.class);
    /**
     * default element bit length
     */
    private static final int DEFAULT_ELEMENT_BIT_LENGTH = Double.SIZE;
    /**
     * large element bit length
     */
    private static final int LARGE_ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * small element bit length
     */
    private static final int SMALL_ELEMENT_BIT_LENGTH = Byte.SIZE;
    /**
     * database size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 14;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        Hhcm23SimpleSingleIndexPirConfig config = new Hhcm23SimpleSingleIndexPirConfig.Builder().build();
        // simple PIR
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.SIMPLE_PIR.name(),
            config,
            Hhcm23SimpleSingleIndexPirParams.DEFAULT_PARAMS
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
     * Simple PIR config
     */
    private final Hhcm23SimpleSingleIndexPirConfig indexPirConfig;
    /**
     * Simple PIR params
     */
    private final Hhcm23SimpleSingleIndexPirParams indexPirParams;

    public SimplePirTest(String name, Hhcm23SimpleSingleIndexPirConfig indexPirConfig,
                         Hhcm23SimpleSingleIndexPirParams indexPirParams) {
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
    public void testSimplePir() {
        testSimplePir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelSimplePir() {
        testSimplePir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementSimplePir() {
        testSimplePir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementSimplePir() {
        testSimplePir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testSimplePir(Hhcm23SimpleSingleIndexPirConfig config, Hhcm23SimpleSingleIndexPirParams indexPirParams,
                              int elementBitLength, boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        Hhcm23SimpleSingleIndexPirServer server = new Hhcm23SimpleSingleIndexPirServer(serverRpc, clientRpc.ownParty(), config);
        Hhcm23SimpleSingleIndexPirClient client = new Hhcm23SimpleSingleIndexPirClient(clientRpc, serverRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        SimplePirServerThread serverThread = new SimplePirServerThread(server, indexPirParams, database);
        SimplePirClientThread clientThread = new SimplePirClientThread(
            client, indexPirParams, retrievalIndex, SERVER_ELEMENT_SIZE, elementBitLength
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            serverRpc.reset();
            clientRpc.reset();
            LOGGER.info("Parameters: \n {}", indexPirParams.toString());
            // verify result
            ByteBuffer result = clientThread.getRetrievalResult();
            Assert.assertEquals(result, ByteBuffer.wrap(database.getBytesData(retrievalIndex)));
            LOGGER.info("Main: The Retrieval Result is Correct");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}
