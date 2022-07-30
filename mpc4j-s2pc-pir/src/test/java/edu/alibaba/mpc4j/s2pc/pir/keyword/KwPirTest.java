package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirParams;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
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
    /**
     * 服务端元素映射
     */
    private final Map<ByteBuffer, ByteBuffer> serverElementMap = new HashMap<>();
    /**
     * 服务端元素集合
     */
    private final Set<ByteBuffer> serverElementSet = new HashSet<>();

    public KwPirTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
    }

    private void setDatabase(SecureRandom secureRandom, int elementByteLength, int labelByteLength) {
        int serverElementSize = 10000;
        for (int j = 0; j < serverElementSize; j++) {
            byte[] item = new byte[elementByteLength];
            do {
                secureRandom.nextBytes(item);
            } while (!serverElementSet.add(ByteBuffer.wrap(item)));
            byte[] label = new byte[labelByteLength];
            secureRandom.nextBytes(label);
            serverElementMap.put(ByteBuffer.wrap(item), ByteBuffer.wrap(label));
        }
    }

    @Test
    public void testOneMillion4096_32() {
        Cmg21KwPirConfig config = new Cmg21KwPirConfig.Builder()
            .setPirParams(Cmg21KwPirParams.ONE_MILLION_4096_32)
            .setCuckooHashBinType(CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH)
            .build();
        boolean parallel = true;
        int labelByteLength = 20;
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
        int labelByteLength = 20;
        int retrievalElementSize = 1;
        testPir(config, parallel, labelByteLength, retrievalElementSize);
    }

    public void testPir(Cmg21KwPirConfig config, boolean parallel, int labelByteLength, int retrievalElementSize) {
        // 数据字节长度
        int elementByteLength = 20;
        int retrievalNumber = 1;
        // 随机生成服务端数据库元素
        SecureRandom secureRandom = new SecureRandom();
        setDatabase(secureRandom, elementByteLength, labelByteLength);
        // 创建参与方实例
        KwPirServer server = KwPirFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        KwPirClient client = KwPirFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        KwPirServerThread serverThread = new KwPirServerThread(server, serverElementMap, elementByteLength, labelByteLength,
            retrievalNumber);
        KwPirClientThread clientThread = new KwPirClientThread(client, serverElementSet, elementByteLength, labelByteLength,
            retrievalElementSize, retrievalNumber);
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
        Map<ByteBuffer, ByteBuffer> pirResultMap = clientThread.getPirResult();
        LOGGER.info("Main: The size of matched IDs is {}", pirResultMap.size());
        if (pirResultMap.isEmpty()) {
            LOGGER.info("Main: Keyword PIR result wrong!");
            assert false;
        }
        LOGGER.info("Main: Check that we retrieved the correct element");
        pirResultMap.forEach((key, value) -> {
            if (!BigIntegerUtils.byteArrayToBigInteger(value.array()).equals(
                BigIntegerUtils.byteArrayToBigInteger(serverElementMap.get(key).array()))) {
                LOGGER.info("Main: Keyword PIR result wrong!");
                System.out.println(Arrays.toString(value.array()));
                System.out.println(Arrays.toString(serverElementMap.get(key).array()));
                assert false;
            }
        });
        LOGGER.info("Main: Keyword PIR result correct!");
        // 打印参数
        LOGGER.info(config.getParams().toString());
    }
}


