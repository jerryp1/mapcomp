package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.fastpir.Ayaa21IndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.fastpir.Ayaa21IndexPirServer;
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
 * Vectorized PIR测试类。
 *
 * @author Liqiang Peng
 * @date 2023/3/24
 */
@RunWith(Parameterized.class)
public class VectorizedPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorizedPirTest.class);
    /**
     * 重复检索次数
     */
    private static final int REPEAT_TIME = 1;
    /**
     * 默认元素字节长度
     */
    private static final int DEFAULT_ELEMENT_BYTE_LENGTH = 8;
    /**
     * 服务端元素数量
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        Mr23IndexPirConfig pirConfig = new Mr23IndexPirConfig();
        configurations.add(new Object[]{
            IndexPirFactory.IndexPirType.FAST_PIR.name(),
            pirConfig,
            new Mr23IndexPirParams(
                8192,
                20,
                new int[]{50, 50, 50, 50},
                64,
                20
            )
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
     * Vectorized PIR配置项
     */
    private final Mr23IndexPirConfig indexPirConfig;
    /**
     * Vectorized PIR参数
     */
    private final Mr23IndexPirParams indexPirParams;

    public VectorizedPirTest(String name, Mr23IndexPirConfig indexPirConfig, Mr23IndexPirParams indexPirParams) {
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
    public void testVectorizedPir() {
        testVectorizedPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelVectorizedPir() {
        testVectorizedPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, true);
    }

    public void testVectorizedPir(Mr23IndexPirConfig config, Mr23IndexPirParams indexPirParams, int elementByteLength,
                                  boolean parallel) {
        ArrayList<Integer> retrievalIndexList = PirUtils.generateRetrievalIndexList(SERVER_ELEMENT_SIZE, REPEAT_TIME);
        // 生成元素数组
        ArrayList<ByteBuffer> elementList = PirUtils.generateElementArrayList(SERVER_ELEMENT_SIZE, elementByteLength);
        // 创建参与方实例
        Mr23IndexPirServer server = new Mr23IndexPirServer(serverRpc, clientRpc.ownParty(), config);
        Mr23IndexPirClient client = new Mr23IndexPirClient(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        VectorizedPirServerThread serverThread = new VectorizedPirServerThread(
            server, indexPirParams, elementList, elementByteLength, REPEAT_TIME
        );
        FastPirClientThread clientThread = new FastPirClientThread(
            client, indexPirParams, retrievalIndexList, SERVER_ELEMENT_SIZE, elementByteLength, REPEAT_TIME
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
            LOGGER.info("Parameters: \n {}", indexPirParams.toString());
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