package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirParams;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * 索引PIR测试类。
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
public class IndexPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexPirTest.class);
    /**
     * 重复检索次数
     */
    private static final int REPEAT_TIME = 5;
    /**
     * 短标签字节长度
     */
    private static final int SHORT_ELEMENT_BYTE_LENGTH = 1;
    /**
     * 默认标签字节长度
     */
    private static final int DEFAULT_ELEMENT_BYTE_LENGTH = 32;
    /**
     * 长标签字节长度
     */
    private static final int LONG_ELEMENT_BYTE_LENGTH = 65;
    /**
     * 服务端元素数量
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 20;
    /**
     * 服务端
     */
    private final Rpc serverRpc;
    /**
     * 客户端
     */
    private final Rpc clientRpc;

    public IndexPirTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
    }

    @Test
    public void testOneDimensionXPIR() {
        Mbfk16IndexPirConfig config = new Mbfk16IndexPirConfig.Builder().setPolyModulusDegree(4096).setPlainModulusSize(20).build();
        Mbfk16IndexPirParams mbfk16IndexPirParams = new Mbfk16IndexPirParams(SERVER_ELEMENT_SIZE, DEFAULT_ELEMENT_BYTE_LENGTH, config, 1);
        testXPIR(config, mbfk16IndexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testTwoDimensionXPIRWithShortElementByteLength() {
        Mbfk16IndexPirConfig config = new Mbfk16IndexPirConfig.Builder().setPolyModulusDegree(4096).setPlainModulusSize(20).build();
        Mbfk16IndexPirParams mbfk16IndexPirParams = new Mbfk16IndexPirParams(SERVER_ELEMENT_SIZE, SHORT_ELEMENT_BYTE_LENGTH, config, 2);
        testXPIR(config, mbfk16IndexPirParams, SHORT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testTwoDimensionXPIRWithDefaultElementByteLength() {
        Mbfk16IndexPirConfig config = new Mbfk16IndexPirConfig.Builder().setPolyModulusDegree(4096).setPlainModulusSize(20).build();
        Mbfk16IndexPirParams mbfk16IndexPirParams = new Mbfk16IndexPirParams(SERVER_ELEMENT_SIZE, DEFAULT_ELEMENT_BYTE_LENGTH, config, 2);
        testXPIR(config, mbfk16IndexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testTwoDimensionXPIRWithLongElementByteLength() {
        Mbfk16IndexPirConfig config = new Mbfk16IndexPirConfig.Builder().setPolyModulusDegree(4096).setPlainModulusSize(20).build();
        Mbfk16IndexPirParams mbfk16IndexPirParams = new Mbfk16IndexPirParams(SERVER_ELEMENT_SIZE, LONG_ELEMENT_BYTE_LENGTH, config, 2);
        testXPIR(config, mbfk16IndexPirParams, LONG_ELEMENT_BYTE_LENGTH, false);
    }

    public void testXPIR(Mbfk16IndexPirConfig config, IndexPirParams indexPirParams, int elementByteLength, boolean parallel) {
        ArrayList<Integer> retrievalIndexList = PirUtils.generateRetrievalIndexList(SERVER_ELEMENT_SIZE, REPEAT_TIME);
        // 生成元素数组
        ArrayList<ByteBuffer> elementList = PirUtils.generateElementArrayList(SERVER_ELEMENT_SIZE, elementByteLength);
        // 创建参与方实例
        IndexPirServer server = IndexPirFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        IndexPirClient client = IndexPirFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        IndexPirServerThread serverThread = new IndexPirServerThread(
            server, indexPirParams, elementList, elementByteLength, REPEAT_TIME
        );
        IndexPirClientThread clientThread = new IndexPirClientThread(
            client, indexPirParams, retrievalIndexList, SERVER_ELEMENT_SIZE, elementByteLength, REPEAT_TIME
        );
        try {
            // 开始执行协议
            serverThread.start();
            clientThread.start();
            // 等待线程停止
            serverThread.join();
            clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LOGGER.info("Server: The Communication costs {}MB", serverRpc.getSendByteLength() * 1.0 / (1024 * 1024));
        serverRpc.reset();
        LOGGER.info("Client: The Communication costs {}MB", clientRpc.getSendByteLength() * 1.0 / (1024 * 1024));
        clientRpc.reset();
        // 验证结果
        ArrayList<ByteBuffer> result = clientThread.getRetrievalResult();
        LOGGER.info("Client: The Retrieval Set Size is {}", result.size());
        for (int index = 0; index < REPEAT_TIME; index++) {
            ByteBuffer retrievalElement = result.get(index);
            Assert.assertEquals(retrievalElement, elementList.get(retrievalIndexList.get(index)));
        }
    }
}