package edu.alibaba.mpc4j.s2pc.pir.batchpir.mr23;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirServer;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * MR23批量查询PIR测试类。
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
@RunWith(Parameterized.class)
public class Mr23BatchPirTest {
    /**
     * 默认比特长度
     */
    private static final int DEFAULT_BIT_LENGTH = 32;
    /**
     * 较小比特长度
     */
    private static final int SMALL_BIT_LENGTH = 1;
    /**
     * 较大比特长度
     */
    private static final int LARGE_BIT_LENGTH = 15;
    /**
     * 服务端元素数量
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 17;
    /**
     * 检索数目2^8
     */
    private static final int LOG_RETRIEVAL_SIZE_8 = 1 << 8;
    /**
     * 检索数目2^9
     */
    private static final int LOG_RETRIEVAL_SIZE_9 = 1 << 9;
    /**
     * 检索数目2^10
     */
    private static final int LOG_RETRIEVAL_SIZE_10 = 1 << 10;
    /**
     * 检索数目2^11
     */
    private static final int LOG_RETRIEVAL_SIZE_11 = 1 << 11;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // vectorized batch PIR
        configurations.add(new Object[]{
            BatchIndexPirFactory.BatchIndexPirType.VECTORIZED_BATCH_PIR.name(),
            new Mr23BatchIndexPirConfig.Builder().build()
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

    public Mr23BatchPirTest(String name, BatchIndexPirConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testSmallBitLengthParallel() {
        testPir(SMALL_BIT_LENGTH, LOG_RETRIEVAL_SIZE_8, config, true);
    }

    @Test
    public void testDefaultBitLengthParallel() {
        testPir(DEFAULT_BIT_LENGTH, LOG_RETRIEVAL_SIZE_8, config, true);
    }

    @Test
    public void testLargeBitLengthParallel() {
        testPir(LARGE_BIT_LENGTH, LOG_RETRIEVAL_SIZE_8, config, false);
    }

    @Test
    public void testLogRetrievalSize8() {
        testPir(SMALL_BIT_LENGTH, LOG_RETRIEVAL_SIZE_8, config, true);
    }

    @Test
    public void testLogRetrievalSize9() {
        testPir(SMALL_BIT_LENGTH, LOG_RETRIEVAL_SIZE_9, config, true);
    }

    @Test
    public void testLogRetrievalSize10() {
        testPir(SMALL_BIT_LENGTH, LOG_RETRIEVAL_SIZE_10, config, true);
    }

    @Test
    public void testLogRetrievalSize11() {
        testPir(SMALL_BIT_LENGTH, LOG_RETRIEVAL_SIZE_11, config, true);
    }

    public void testPir(int elementBitLength, int retrievalSize, BatchIndexPirConfig config, boolean parallel) {
        Set<Integer> retrievalIndexSet = PirUtils.generateRetrievalIndexSet(SERVER_ELEMENT_SIZE, retrievalSize);
        // 随机构建服务端关键词和标签映射
        byte[][] serverElementArray = PirUtils.generateElementArray(SERVER_ELEMENT_SIZE, elementBitLength);
        // 创建参与方实例
        Mr23BatchIndexPirServer server = (Mr23BatchIndexPirServer) BatchIndexPirFactory.createServer(
            serverRpc, clientRpc.ownParty(), config
        );
        Mr23BatchIndexPirClient client = (Mr23BatchIndexPirClient) BatchIndexPirFactory.createClient(
            clientRpc, serverRpc.ownParty(), config
        );
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        Mr23BatchPirServerThread serverThread = new Mr23BatchPirServerThread(
            server, serverElementArray, elementBitLength, retrievalSize
        );
        ArrayList<Integer> retrievalIndexList = new ArrayList<>(retrievalIndexSet);
        Mr23BatchPirClientThread clientThread = new Mr23BatchPirClientThread(
            client, retrievalIndexList, elementBitLength, SERVER_ELEMENT_SIZE, retrievalSize
        );
        try {
            // 开始执行协议
            serverThread.start();
            clientThread.start();
            // 等待线程停止
            serverThread.join();
            clientThread.join();
            // 验证结果
            Map<Integer, byte[]> result = clientThread.getRetrievalResult();
            Assert.assertEquals(retrievalSize, result.size());
            result.forEach((key, value) ->
                Assert.assertEquals(ByteBuffer.wrap(serverElementArray[key]), ByteBuffer.wrap(value))
            );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}


