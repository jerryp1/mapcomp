package edu.alibaba.mpc4j.s2pc.pir.index.fastpir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirFactory;
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
 * FastPIR测试类。
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class FastPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FastPirTest.class);
    /**
     * 重复检索次数
     */
    private static final int REPEAT_TIME = 1;
    /**
     * 默认元素字节长度
     */
    private static final int DEFAULT_ELEMENT_BYTE_LENGTH = 64;
    /**
     * 服务端元素数量
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 20;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        Ayaa21IndexPirConfig fastpirConfig = new Ayaa21IndexPirConfig();
        configurations.add(new Object[]{
            IndexPirFactory.IndexPirType.FAST_PIR.name(),
            fastpirConfig,
            new Ayaa21IndexPirParams(
                4096,
                1073153L,
                new long[]{1152921504606830593L, 562949953216513L}
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
     * FastPIR配置项
     */
    private final Ayaa21IndexPirConfig indexPirConfig;
    /**
     * FastPIR参数
     */
    private final Ayaa21IndexPirParams indexPirParams;

    public FastPirTest(String name, Ayaa21IndexPirConfig indexPirConfig, Ayaa21IndexPirParams indexPirParams) {
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
        testFastPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelFastPir() {
        testFastPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, true);
    }

    public void testFastPir(Ayaa21IndexPirConfig config, Ayaa21IndexPirParams indexPirParams, int elementByteLength,
                         boolean parallel) {
        ArrayList<Integer> retrievalIndexList = PirUtils.generateRetrievalIndexList(SERVER_ELEMENT_SIZE, REPEAT_TIME);
        // 生成元素数组
        ArrayList<ByteBuffer> elementList = PirUtils.generateElementArrayList(SERVER_ELEMENT_SIZE, elementByteLength);
        // 创建参与方实例
        Ayaa21IndexPirServer server = new Ayaa21IndexPirServer(serverRpc, clientRpc.ownParty(), config);
        Ayaa21IndexPirClient client = new Ayaa21IndexPirClient(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        FastPirServerThread serverThread = new FastPirServerThread(
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