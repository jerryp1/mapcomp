package edu.alibaba.mpc4j.s2pc.pir.index.mulpir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirServer;
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
 * Mul PIR test.
 *
 * @author Qixian Zhou
 * @date 2023/5/29
 */
@RunWith(Parameterized.class)
public class MulPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MulPirTest.class);
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
        Alpr21SingleIndexPirConfig mulpirConfig = new Alpr21SingleIndexPirConfig();
        // Mul PIR (1-dimension)
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.MUL_PIR.name() + " (1-dimension)",
            mulpirConfig,
            new Alpr21SingleIndexPirParams(
                4096,
                20,
                1
            )
        });
        // Mul PIR (2-dimension)
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.MUL_PIR.name() + " (2-dimension)",
            mulpirConfig,
            new Alpr21SingleIndexPirParams(
                8192,
                20,
                2
            )
        });
        // Mul PIR (3-dimension)
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.MUL_PIR.name() + " (3-dimension)",
            mulpirConfig,
            new Alpr21SingleIndexPirParams(
                8192,
                20,
                3
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
     * Mul PIR config
     */
    private final Alpr21SingleIndexPirConfig indexPirConfig;
    /**
     * Mul PIR params
     */
    private final Alpr21SingleIndexPirParams indexPirParams;

    public MulPirTest(String name, Alpr21SingleIndexPirConfig indexPirConfig, Alpr21SingleIndexPirParams indexPirParams) {
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
    public void testMulPir() {
        testMulPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelMulPir() {
        testMulPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementMulPir() {
        testMulPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementMulPir() {
        testMulPir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testMulPir(Alpr21SingleIndexPirConfig config, Alpr21SingleIndexPirParams indexPirParams,
                           int elementBitLength, boolean parallel) {
        int retrievalSingleIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        Alpr21SingleIndexPirServer server = new Alpr21SingleIndexPirServer(serverRpc, clientRpc.ownParty(), config);
        Alpr21SingleIndexPirClient client = new Alpr21SingleIndexPirClient(clientRpc, serverRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        MulPirServerThread serverThread = new MulPirServerThread(server, indexPirParams, database);
        MulPirClientThread clientThread = new MulPirClientThread(
            client, indexPirParams, retrievalSingleIndex, SERVER_ELEMENT_SIZE, elementBitLength
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
            Assert.assertEquals(result, ByteBuffer.wrap(database.getBytesData(retrievalSingleIndex)));
            LOGGER.info("Main: The Retrieval Result is Correct");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}
