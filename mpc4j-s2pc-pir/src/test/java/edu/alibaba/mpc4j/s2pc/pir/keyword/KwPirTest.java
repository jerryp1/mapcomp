package edu.alibaba.mpc4j.s2pc.pir.keyword;

import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirParams;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * 关键词索引PIR测试类。
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class KwPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(KwPirTest.class);
    /**
     * 服务端
     */
    private final Rpc serverRpc;
    /**
     * 客户端
     */
    private final Rpc clientRpc;

    public KwPirTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
    }

    @Test
    public void testOneMillion4096_32() {
        Cmg21KwPirConfig config = new Cmg21KwPirConfig.Builder()
            .setPirParams(Cmg21KwPirParams.ONE_MILLION_4096_32)
            .setCuckooHashBinType(CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH)
            .build();
        boolean parallel = true;
        int labelByteLength = 32;
        int retrievalElementSize = 4000;
        testPir(config, parallel, labelByteLength, retrievalElementSize);
    }

    @Test
    public void testOneMillion1_32() {
        Cmg21KwPirConfig config = new Cmg21KwPirConfig.Builder()
            .setPirParams(Cmg21KwPirParams.ONE_MILLION_1_32)
            .setCuckooHashBinType(CuckooHashBinFactory.CuckooHashBinType.NO_STASH_ONE_HASH)
            .build();
        boolean parallel = true;
        int labelByteLength = 32;
        int retrievalElementSize = 1;
        testPir(config, parallel, labelByteLength, retrievalElementSize);
    }

    public void testPir(Cmg21KwPirConfig config, boolean parallel, int labelByteLength, int retrievalElementSize) {
        // 数据字节长度
        int elementByteLength = 20;
        // 检索次数
        int retrievalNumber = 20;
        // 随机生成服务端数据库关键词
        int serverElementSize = 1 << 20;
        ArrayList<Set<ByteBuffer>> randomSets = PirUtils.generateBytesSets(serverElementSize, retrievalElementSize,
            retrievalNumber, elementByteLength);
        // 随机构建服务端关键词和标签映射
        Map<ByteBuffer, ByteBuffer> serverKwLabelMap = PirUtils.generateKwLabelMap(randomSets.get(0), labelByteLength);
        // 创建参与方实例
        KwPirServer server = KwPirFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        KwPirClient client = KwPirFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        KwPirServerThread serverThread = new KwPirServerThread(server, serverKwLabelMap, labelByteLength,
            retrievalNumber);
        KwPirClientThread clientThread = new KwPirClientThread(client,
            Lists.newArrayList(randomSets.subList(1, retrievalNumber + 1)), labelByteLength);
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
        // 真实结果
        Set<ByteBuffer> intersectionSet = new HashSet<>();
        for (int i = 0; i < retrievalNumber; i++) {
            randomSets.get(i + 1).retainAll(randomSets.get(0));
            intersectionSet.addAll(randomSets.get(i + 1));
        }
        // 验证结果
        Map<ByteBuffer, ByteBuffer> resultMap = clientThread.getPirResult();
        Assert.assertEquals(resultMap.size(), intersectionSet.size());
        LOGGER.info("Main: The size of matched IDs is {}", resultMap.size());
        LOGGER.info("Main: Check that we retrieved the correct element");
        resultMap.forEach((key, value) -> Assert.assertEquals(value, serverKwLabelMap.get(key)));
        LOGGER.info("Main: Keyword PIR result correct!");
        // 打印参数
        LOGGER.info(config.getParams().toString());
    }
}


