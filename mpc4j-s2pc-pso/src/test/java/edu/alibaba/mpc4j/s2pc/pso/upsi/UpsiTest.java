package edu.alibaba.mpc4j.s2pc.pso.upsi;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiParams;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

/**
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class UpsiTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpsiTest.class);
    /**
     * 服务端
     */
    private final Rpc serverRpc;
    /**
     * 客户端
     */
    private final Rpc clientRpc;

    public UpsiTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
    }

    @Test
    public void testTwoThousand1() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder()
            .setUpsiParams(Cmg21UpsiParams.TWO_THOUSAND_1)
            .setCuckooHashBinType(CuckooHashBinFactory.CuckooHashBinType.NO_STASH_ONE_HASH)
            .build();
        int serverSize = 2000;
        int clientSize = 1;
        testUpsi(config, serverSize, clientSize, true);
    }

    @Test
    public void testOneHundredThousand1() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder()
            .setUpsiParams(Cmg21UpsiParams.ONE_HUNDRED_THOUSAND_1)
            .setCuckooHashBinType(CuckooHashBinFactory.CuckooHashBinType.NO_STASH_ONE_HASH)
            .build();
        int serverSize = 100000;
        int clientSize = 1;
        testUpsi(config, serverSize, clientSize, true);
    }

    @Test
    public void testOneMillion1024CmpParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_1024_CMP).build();
        int serverSize = 1000000;
        int clientSize = 1024;
        boolean parallel = true;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion1024Cmp() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_1024_CMP).build();
        int serverSize = 1000000;
        int clientSize = 1024;
        boolean parallel = false;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion1024ComParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_1024_COM).build();
        int serverSize = 1000000;
        int clientSize = 1024;
        boolean parallel = true;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion1024Com() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_1024_COM).build();
        int serverSize = 1000000;
        int clientSize = 1024;
        boolean parallel = false;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion11041Parallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_11041).build();
        int serverSize = 1000000;
        int clientSize = 11041;
        boolean parallel = true;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion11041() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_11041).build();
        int serverSize = 1000000;
        int clientSize = 11041;
        boolean parallel = false;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion2048CmpParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_2048_CMP).build();
        int serverSize = 1000000;
        int clientSize = 2048;
        boolean parallel = true;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion2048Cmp() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_2048_CMP).build();
        int serverSize = 1000000;
        int clientSize = 2048;
        boolean parallel = false;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion2048ComParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_2048_COM).build();
        int serverSize = 1000000;
        int clientSize = 2048;
        boolean parallel = true;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion2048Com() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_2048_COM).build();
        int serverSize = 1000000;
        int clientSize = 2048;
        boolean parallel = false;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion256Parallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_256).build();
        int serverSize = 1000000;
        int clientSize = 256;
        boolean parallel = true;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion256() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_256).build();
        int serverSize = 1000000;
        int clientSize = 256;
        boolean parallel = false;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion4096CmpParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_4096_CMP).build();
        int serverSize = 1000000;
        int clientSize = 4096;
        boolean parallel = true;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion4096Cmp() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_4096_CMP).build();
        int serverSize = 1000000;
        int clientSize = 4096;
        boolean parallel = false;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion4096ComParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_4096_COM).build();
        int serverSize = 1000000;
        int clientSize = 4096;
        boolean parallel = true;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion4096Com() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_4096_COM).build();
        int serverSize = 1000000;
        int clientSize = 4096;
        boolean parallel = false;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion512CmpParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_512_CMP).build();
        int serverSize = 1000000;
        int clientSize = 512;
        boolean parallel = true;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion512Cmp() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_512_CMP).build();
        int serverSize = 1000000;
        int clientSize = 512;
        boolean parallel = false;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion512ComParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_512_COM).build();
        int serverSize = 1000000;
        int clientSize = 512;
        boolean parallel = true;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion512Com() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_512_COM).build();
        int serverSize = 1000000;
        int clientSize = 512;
        boolean parallel = false;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion5535Parallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_5535).build();
        int serverSize = 1000000;
        int clientSize = 5535;
        boolean parallel = true;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion5535() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().setUpsiParams(Cmg21UpsiParams.ONE_MILLION_5535).build();
        int serverSize = 1000000;
        int clientSize = 5535;
        boolean parallel = false;
        testUpsi(config, serverSize, clientSize, parallel);
    }

    public void testUpsi(Cmg21UpsiConfig config, int serverSize, int clientSize, boolean parallel) {
        // 数据字节长度
        int elementByteLength = 20;
        List<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSize, clientSize, elementByteLength);
        Set<ByteBuffer> serverElementSet = sets.get(0);
        Set<ByteBuffer> clientElementSet = sets.get(1);
        // 创建参与方实例
        UpsiServer server = UpsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        UpsiClient client = UpsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);

        UpsiServerThread serverThread = new UpsiServerThread(server, serverElementSet, clientElementSet.size(),
            elementByteLength);
        UpsiClientThread clientThread = new UpsiClientThread(client, clientElementSet, elementByteLength);
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
        // 验证结果
        Set<ByteBuffer> psiResult = clientThread.getIntersectionSet();
        LOGGER.info("Main: The size of matched IDs is {}", psiResult.size());
        LOGGER.info("Server: The Communication costs {}MB", serverRpc.getSendByteLength()*1.0/(1024*1024));
        LOGGER.info("Client: The Communication costs {}MB", clientRpc.getSendByteLength()*1.0/(1024*1024));
        sets.get(0).retainAll(sets.get(1));
        if (!sets.get(0).containsAll(psiResult) | !psiResult.containsAll(sets.get(0))) {
            LOGGER.info("Main: unbalance PSI result wrong!");
            assert false;
        }
        LOGGER.info("Main: unbalance PSI result correct!");
        // 打印参数
        LOGGER.info(config.getParams().toString());
    }
}
