package edu.alibaba.mpc4j.s2pc.pir.index.doublepir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirServer;
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
 * Double PIR test.
 *
 * @author Liqiang Peng
 * @date 2023/5/29
 */
@RunWith(Parameterized.class)
public class DoublePirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DoublePirTest.class);
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
    private static final int SERVER_ELEMENT_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        Hhcm23DoubleSingleIndexPirConfig config = new Hhcm23DoubleSingleIndexPirConfig();
        // double PIR
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.DOUBLE_PIR.name(),
            config,
            Hhcm23DoubleSingleIndexPirParams.DEFAULT_PARAMS
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
     * Double PIR config
     */
    private final Hhcm23DoubleSingleIndexPirConfig config;
    /**
     * Double PIR params
     */
    private final Hhcm23DoubleSingleIndexPirParams params;

    public DoublePirTest(String name, Hhcm23DoubleSingleIndexPirConfig config, Hhcm23DoubleSingleIndexPirParams params) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
        this.params = params;
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
    public void testDoublePir() {
        testDoublePir(config, params, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelDoublePir() {
        testDoublePir(config, params, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementDoublePir() {
        testDoublePir(config, params, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementDoublePir() {
        testDoublePir(config, params, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testDoublePir(Hhcm23DoubleSingleIndexPirConfig config, Hhcm23DoubleSingleIndexPirParams params,
                              int elementBitLength, boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        Hhcm23DoubleSingleIndexPirServer server = new Hhcm23DoubleSingleIndexPirServer(serverRpc, clientRpc.ownParty(), config);
        Hhcm23DoubleSingleIndexPirClient client = new Hhcm23DoubleSingleIndexPirClient(clientRpc, serverRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        DoublePirServerThread serverThread = new DoublePirServerThread(server, params, database);
        DoublePirClientThread clientThread = new DoublePirClientThread(
            client, params, retrievalIndex, SERVER_ELEMENT_SIZE, elementBitLength
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            serverRpc.reset();
            clientRpc.reset();
            LOGGER.info("Parameters: \n {}", params.toString());
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
