package edu.alibaba.mpc4j.s2pc.pir.index.constantweightpir;


import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirServer;
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
 * Constant-Weight PIR test
 *
 * @author Qixian Zhou
 * @date 2023/5/29
 */
@RunWith(Parameterized.class)
public class ConstantWeightPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConstantWeightPirTest.class);
    /**
     * default element bit length
     */
    private static final int DEFAULT_ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * large element bit length
     */
    private static final int LARGE_ELEMENT_BIT_LENGTH = 10000;
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
        Mk22SingleIndexPirConfig config = new Mk22SingleIndexPirConfig();
        // ConstantWeight PIR (CONSTANT_WEIGHT_EQ)
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.CONSTANT_WEIGHT_PIR.name() + " (CONSTANT_WEIGHT_EQ)",
            config, new Mk22SingleIndexPirParams(2, 16384, 21, Mk22SingleIndexPirParams.EqualityType.CONSTANT_WEIGHT)
        });
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.CONSTANT_WEIGHT_PIR.name() + " (FOLKLORE)",
            config, new Mk22SingleIndexPirParams(2, 16384, 21, Mk22SingleIndexPirParams.EqualityType.FOLKLORE)
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
     * ConstantWeight PIR config
     */
    private final Mk22SingleIndexPirConfig indexPirConfig;
    /**
     * ConstantWeight PIR params
     */
    private final Mk22SingleIndexPirParams indexPirParams;

    public ConstantWeightPirTest(String name, Mk22SingleIndexPirConfig indexPirConfig,
                                 Mk22SingleIndexPirParams indexPirParams) {
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
    public void testConstantWeightPir() {
        testConstantWeightPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelConstantWeightPir() {
        testConstantWeightPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementMulPir() {
        testConstantWeightPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementMulPir() {
        testConstantWeightPir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testConstantWeightPir(Mk22SingleIndexPirConfig config, Mk22SingleIndexPirParams indexPirParams,
                                      int elementBitLength, boolean parallel) {
        int retrievalSingleIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        Mk22SingleIndexPirServer server = new Mk22SingleIndexPirServer(serverRpc, clientRpc.ownParty(), config);
        Mk22SingleIndexPirClient client = new Mk22SingleIndexPirClient(clientRpc, serverRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        ConstantWeightPirServerThread serverThread = new ConstantWeightPirServerThread(server, indexPirParams, database);
        ConstantWeightPirClientThread clientThread = new ConstantWeightPirClientThread(
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
