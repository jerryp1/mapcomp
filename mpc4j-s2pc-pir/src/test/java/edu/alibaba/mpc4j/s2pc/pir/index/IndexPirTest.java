package edu.alibaba.mpc4j.s2pc.pir.index;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.fastpir.Ayaa21IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.fastpir.Ayaa21IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.onionpir.Mcr21IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.onionpir.Mcr21IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.sealpir.Acls18IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.sealpir.Acls18IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir.Mr23IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirParams;
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
 * 索引PIR测试类。
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class IndexPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexPirTest.class);
    /**
     * 重复检索次数
     */
    private static final int REPEAT_TIME = 1;
    /**
     * 默认元素字节长度
     */
    private static final int DEFAULT_ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * 较小元素字节长度
     */
    private static final int SMALL_ELEMENT_BYTE_LENGTH = 2;//CommonConstants.STATS_BYTE_LENGTH;
    /**
     * 较大元素字节长度
     */
    private static final int LARGE_ELEMENT_BYTE_LENGTH = 64;
    /**
     * 服务端元素数量
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 16;
    /**
     * 较小服务端元素数量
     */
    private static final int SMALL_SERVER_ELEMENT_SIZE = 1 << 10;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // XPIR
        configurations.add(new Object[]{IndexPirFactory.IndexPirType.XPIR.name(), new Mbfk16IndexPirConfig()});
        // SEAL PIR
        configurations.add(new Object[]{IndexPirFactory.IndexPirType.SEAL_PIR.name(), new Acls18IndexPirConfig()});
        // OnionPIR
        configurations.add(new Object[]{IndexPirFactory.IndexPirType.ONION_PIR.name(), new Mcr21IndexPirConfig()});
        // FastPIR
        configurations.add(new Object[]{IndexPirFactory.IndexPirType.FAST_PIR.name(), new Ayaa21IndexPirConfig()});
        // Vectorized PIR
        configurations.add(new Object[]{IndexPirFactory.IndexPirType.VECTORIZED_PIR.name(), new Mr23IndexPirConfig()});
        return configurations;
    }

    /**
     * 服务端
     */
    private final Rpc serverRpc;
    /**
     * 客户端
     */
    private final Rpc clientRpc;
    /**
     * 索引PIR配置项
     */
    private final IndexPirConfig indexPirConfig;

    public IndexPirTest(String name, IndexPirConfig indexPirConfig) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.indexPirConfig = indexPirConfig;
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
    public void testParallelIndexPir() {
        testIndexPir(indexPirConfig, DEFAULT_ELEMENT_BYTE_LENGTH, SERVER_ELEMENT_SIZE, true);
    }

    @Test
    public void testLargeElementByteLength() {
        testIndexPir(indexPirConfig, LARGE_ELEMENT_BYTE_LENGTH, SMALL_SERVER_ELEMENT_SIZE, false);
    }

    @Test
    public void testDefaultElementByteLength() {
        testIndexPir(indexPirConfig, DEFAULT_ELEMENT_BYTE_LENGTH, SERVER_ELEMENT_SIZE, false);
    }

    @Test
    public void testSmallElementByteLength() {
        testIndexPir(indexPirConfig, SMALL_ELEMENT_BYTE_LENGTH, SERVER_ELEMENT_SIZE, false);
    }

    public void testIndexPir(IndexPirConfig config, int elementByteLength, int serverElementSize, boolean parallel) {
        ArrayList<Integer> retrievalIndexList = PirUtils.generateRetrievalIndexList(serverElementSize, REPEAT_TIME);
        // 生成元素数组
        ArrayList<ByteBuffer> elementList = PirUtils.generateElementArrayList(serverElementSize, elementByteLength);
        // 创建参与方实例
        IndexPirServer server = IndexPirFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        IndexPirClient client = IndexPirFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        IndexPirServerThread serverThread = new IndexPirServerThread(server, elementList, elementByteLength, REPEAT_TIME);
        IndexPirClientThread clientThread = new IndexPirClientThread(client, retrievalIndexList, serverElementSize, elementByteLength, REPEAT_TIME);
        try {
            // 开始执行协议
            serverThread.start();
            clientThread.start();
            // 等待线程停止
            serverThread.join();
            clientThread.join();
            LOGGER.info("Server: The Communication costs {}MB", serverRpc.getSendByteLength() * 1.0 / (1024 * 1024));
            serverRpc.reset();
            LOGGER.info("Client: The Communication costs {}MB", clientRpc.getSendByteLength() * 1.0 / (1024 * 1024));
            clientRpc.reset();
            // 验证结果
            ArrayList<ByteBuffer> result = clientThread.getRetrievalResult();
            for (int index = 0; index < REPEAT_TIME; index++) {
                ByteBuffer retrievalElement = result.get(index);
                Assert.assertEquals(retrievalElement, elementList.get(retrievalIndexList.get(index)));
            }
            LOGGER.info("Client: The Retrieval Set Size is {}", result.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}