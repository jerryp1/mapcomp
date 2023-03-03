package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.*;
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
 * XPIR测试类。
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class XPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(XPirTest.class);
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
        Mbfk16IndexPirConfig xpirConfig = new Mbfk16IndexPirConfig();
        // XPIR (1-dimension)
        configurations.add(new Object[]{
            IndexPirFactory.IndexPirType.XPIR.name() + " (1-dimension)",
            xpirConfig,
            new Mbfk16IndexPirParams(
                4096,
                20,
                1
            )
        });
        // XPIR (2-dimension)
        configurations.add(new Object[]{
            IndexPirFactory.IndexPirType.XPIR.name() + " (2-dimension)",
            xpirConfig,
            new Mbfk16IndexPirParams(
                4096,
                20,
                2
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
     * XPIR配置项
     */
    private final Mbfk16IndexPirConfig indexPirConfig;
    /**
     * XPIR参数
     */
    private final Mbfk16IndexPirParams indexPirParams;

    public XPirTest(String name, Mbfk16IndexPirConfig indexPirConfig, Mbfk16IndexPirParams indexPirParams) {
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
    public void testXPir() {
        testXPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelXPir() {
        testXPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, true);
    }

    public void testXPir(Mbfk16IndexPirConfig config, Mbfk16IndexPirParams indexPirParams, int elementByteLength,
                         boolean parallel) {
        ArrayList<Integer> retrievalIndexList = PirUtils.generateRetrievalIndexList(SERVER_ELEMENT_SIZE, REPEAT_TIME);
        // 生成元素数组
        ArrayList<ByteBuffer> elementList = PirUtils.generateElementArrayList(SERVER_ELEMENT_SIZE, elementByteLength);
        // 创建参与方实例
        Mbfk16IndexPirServer server = new Mbfk16IndexPirServer(serverRpc, clientRpc.ownParty(), config);
        Mbfk16IndexPirClient client = new Mbfk16IndexPirClient(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        XPirServerThread serverThread = new XPirServerThread(
            server, indexPirParams, elementList, elementByteLength, REPEAT_TIME
        );
        XPirClientThread clientThread = new XPirClientThread(
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