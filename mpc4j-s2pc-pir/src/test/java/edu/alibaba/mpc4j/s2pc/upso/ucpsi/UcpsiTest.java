package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.upsi.UpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.upsi.UpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.upsi.UpsiParams;
import edu.alibaba.mpc4j.s2pc.pso.upsi.UpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiParams;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UnbalancedCpsiConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Liqiang Peng
 * @date 2023/4/18
 */
@RunWith(Parameterized.class)
public class UcpsiTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcpsiTest.class);

    /**
     * server element size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 5;
    /**
     * client element size
     */
    private static final int CLIENT_ELEMENT_SIZE = 1 << 0;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // PSTY19
        configurations.add(new Object[]{
            UnbalancedCpsiFactory.UnbalancedCpsiType.PSTY19.name(), new Psty19UnbalancedCpsiConfig.Builder().build()
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
     * the unbalanced PSI config
     */
    private final UnbalancedCpsiConfig config;

    public UcpsiTest(String name, UnbalancedCpsiConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
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
    public void testUcpsiParallel() {
        testUcpsi(SERVER_ELEMENT_SIZE, CLIENT_ELEMENT_SIZE, true);
    }


    public void testUcpsi(int serverSize, int clientSize, boolean parallel) {
        List<Set<String>> sets = PsoUtils.generateStringSets("ID", serverSize, clientSize);
        Set<String> serverElementSet = sets.get(0);
        Set<String> clientElementSet = sets.get(1);
        System.out.println(serverElementSet);
        System.out.println(clientElementSet);
        // 创建参与方实例
        UnbalancedCpsiServer<String> server = UnbalancedCpsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        UnbalancedCpsiClient<String> client = UnbalancedCpsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        int randomTaskId = Math.abs(new SecureRandom().nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        UcpsiServerThread<String> serverThread = new UcpsiServerThread<>(server, serverElementSet, clientSize);
        UcpsiClientThread<String> clientThread = new UcpsiClientThread<>(client, clientElementSet, serverSize);
        try {
            // 开始执行协议
            serverThread.start();
            clientThread.start();
            // 等待线程停止
            serverThread.join();
            clientThread.join();
            // 验证结果
//            Set<String> psiResult = clientThread.getIntersectionSet();
//            LOGGER.info("Server: The Communication costs {}MB", serverRpc.getSendByteLength() * 1.0 / (1024 * 1024));
//            LOGGER.info("Client: The Communication costs {}MB", clientRpc.getSendByteLength() * 1.0 / (1024 * 1024));
//            sets.get(0).retainAll(sets.get(1));
//            Assert.assertTrue(sets.get(0).containsAll(psiResult));
//            Assert.assertTrue(psiResult.containsAll(sets.get(0)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}
