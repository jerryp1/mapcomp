package edu.alibaba.mpc4j.s2pc.pir.batchpir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir.Lpzg24BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirConfig;
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
 * 批量索引PIR测试类。
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
@RunWith(Parameterized.class)
public class BatchPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchPirTest.class);
    /**
     * 默认字节长度
     */
    private static final int DEFAULT_LABEL_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * 服务端元素数量
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 16;
    /**
     * 默认客户端元素数量
     */
    private static final int DEFAULT_CLIENT_SET_SIZE = 1 << 8;
    /**
     * 较大客户端元素数量
     */
    private static final int LARGE_CLIENT_SET_SIZE = 1 << 12;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // PSI-PIR
//        configurations.add(new Object[]{
//            BatchIndexPirFactory.BatchIndexPirType.PSI_PIR.name(), new Lpzg24BatchIndexPirConfig.Builder().build()
//        });
        // vectorized batch PIR
        configurations.add(new Object[]{
            BatchIndexPirFactory.BatchIndexPirType.VECTORIZED_BATCH_PIR.name(), new Mr23BatchIndexPirConfig.Builder().build()
        });
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
     * 批量索引PIR配置项
     */
    private final BatchIndexPirConfig config;

    public BatchPirTest(String name, BatchIndexPirConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testDefaultRetrievalSizeParallel() {
        testPir(DEFAULT_CLIENT_SET_SIZE, config, true);
    }

    @Test
    public void testDefaultRetrievalSize() {
        testPir(DEFAULT_CLIENT_SET_SIZE, config, false);
    }

    public void testPir(int retrievalSize, BatchIndexPirConfig config, boolean parallel) {
        Set<Integer> retrievalIndexSet = PirUtils.generateRetrievalIndexSet(SERVER_ELEMENT_SIZE, retrievalSize);
        // 随机构建服务端关键词和标签映射
        ArrayList<ByteBuffer> serverElementList = PirUtils.generateElementArrayList(SERVER_ELEMENT_SIZE);
        // 创建参与方实例
        BatchIndexPirServer server = BatchIndexPirFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        BatchIndexPirClient client = BatchIndexPirFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        BatchPirServerThread serverThread = new BatchPirServerThread(
            server, serverElementList, 8, DEFAULT_CLIENT_SET_SIZE
        );
        ArrayList<Integer> retrievalIndexList = new ArrayList<>(retrievalIndexSet);
        BatchPirClientThread clientThread = new BatchPirClientThread(
            client, retrievalIndexList, 8, SERVER_ELEMENT_SIZE, DEFAULT_CLIENT_SET_SIZE
        );
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
            Map<Integer, ByteBuffer> result = clientThread.getRetrievalResult();
            Assert.assertEquals(retrievalSize, result.size());
            result.forEach((key, value) -> Assert.assertEquals(serverElementList.get(key), value));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}


