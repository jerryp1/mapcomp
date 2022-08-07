package edu.alibaba.mpc4j.s2pc.pso.upsi;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiParams;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

/**
 * UPSI协议测试。
 *
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
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 2000;
        int clientSize = 1;
        testUpsi(config, Cmg21UpsiParams.TWO_THOUSAND_1, serverSize, clientSize, true);
    }

    @Test
    public void testOneHundredThousand1() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 100000;
        int clientSize = 1;
        testUpsi(config, Cmg21UpsiParams.ONE_HUNDRED_THOUSAND_1, serverSize, clientSize, true);
    }

    @Test
    public void testOneMillion1024CmpParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 1024;
        boolean parallel = true;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_1024_CMP, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion1024Cmp() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 1024;
        boolean parallel = false;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_1024_CMP, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion1024ComParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 1024;
        boolean parallel = true;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_1024_COM, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion1024Com() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 1024;
        boolean parallel = false;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_1024_COM, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion11041Parallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 11041;
        boolean parallel = true;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_11041, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion11041() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 11041;
        boolean parallel = false;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_11041, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion2048CmpParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 2048;
        boolean parallel = true;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_2048_CMP, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion2048Cmp() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 2048;
        boolean parallel = false;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_2048_CMP, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion2048ComParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 2048;
        boolean parallel = true;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_2048_COM, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion2048Com() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 2048;
        boolean parallel = false;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_2048_COM, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion256Parallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 256;
        boolean parallel = true;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_256, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion256() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 256;
        boolean parallel = false;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_256, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion4096CmpParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 4096;
        boolean parallel = true;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_4096_CMP, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion4096Cmp() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 4096;
        boolean parallel = false;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_4096_CMP, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion4096ComParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 4096;
        boolean parallel = true;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_4096_COM, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion4096Com() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 4096;
        boolean parallel = false;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_4096_COM, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion512CmpParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 512;
        boolean parallel = true;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_512_CMP, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion512Cmp() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 512;
        boolean parallel = false;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_512_CMP, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion512ComParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 512;
        boolean parallel = true;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_512_COM, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion512Com() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 512;
        boolean parallel = false;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_512_COM, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion5535Parallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 5535;
        boolean parallel = true;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_5535, serverSize, clientSize, parallel);
    }

    @Test
    public void testOneMillion5535() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        int serverSize = 1000000;
        int clientSize = 5535;
        boolean parallel = false;
        testUpsi(config, Cmg21UpsiParams.ONE_MILLION_5535, serverSize, clientSize, parallel);
    }

    public void testUpsi(Cmg21UpsiConfig config, UpsiParams upsiParams, int serverSize, int clientSize, boolean parallel) {
        List<Set<String>> sets = PsoUtils.generateStringSets("ID", serverSize, clientSize);
        Set<String> serverElementSet = sets.get(0);
        Set<String> clientElementSet = sets.get(1);
        // 创建参与方实例
        UpsiServer<String> server = UpsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        UpsiClient<String> client = UpsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        long randomTaskId = Math.abs(new SecureRandom().nextLong());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        UpsiServerThread<String> serverThread = new UpsiServerThread<>(
            server, upsiParams, serverElementSet, clientElementSet.size()
        );
        UpsiClientThread<String> clientThread = new UpsiClientThread<>(client, upsiParams, clientElementSet);
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
        Set<String> psiResult = clientThread.getIntersectionSet();
        LOGGER.info("Server: The Communication costs {}MB", serverRpc.getSendByteLength() * 1.0 / (1024 * 1024));
        LOGGER.info("Client: The Communication costs {}MB", clientRpc.getSendByteLength() * 1.0 / (1024 * 1024));
        sets.get(0).retainAll(sets.get(1));
        Assert.assertTrue(sets.get(0).containsAll(psiResult));
        Assert.assertTrue(psiResult.containsAll(sets.get(0)));
        LOGGER.info("Main: unbalance PSI result correct!");
    }
}
