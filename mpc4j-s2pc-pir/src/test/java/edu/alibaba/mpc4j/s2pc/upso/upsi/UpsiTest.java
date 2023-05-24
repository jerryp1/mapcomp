package edu.alibaba.mpc4j.s2pc.upso.upsi;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiConfig;
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
 * UPSI test.
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
@RunWith(Parameterized.class)
public class UpsiTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpsiTest.class);
    /**
     * server element size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 18;
    /**
     * client element size
     */
    private static final int CLIENT_ELEMENT_SIZE = 1 << 12;
    /**
     * max client element size
     */
    private static final int MAX_CLIENT_ELEMENT_SIZE = 5535;
    /**
     * server rpc
     */
    private final Rpc serverRpc;
    /**
     * client rpc
     */
    private final Rpc clientRpc;
    /**
     * UPSI config
     */
    private final UpsiConfig config;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CMG21
        configurations.add(new Object[]{
            UpsiFactory.UpsiType.CMG21.name(), new Cmg21UpsiConfig.Builder().build()
        });

        return configurations;
    }

    public UpsiTest(String name, UpsiConfig config) {
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
    public void testCmg21Parallel() {
        testUpsi(SERVER_ELEMENT_SIZE, CLIENT_ELEMENT_SIZE, true);
    }

    @Test
    public void testCmg21() {
        testUpsi(SERVER_ELEMENT_SIZE, CLIENT_ELEMENT_SIZE, false);
    }

    public void testUpsi(int serverSize, int clientSize, boolean parallel) {
        assert clientSize <= MAX_CLIENT_ELEMENT_SIZE;
        List<Set<String>> sets = PsoUtils.generateStringSets("ID", serverSize, clientSize);
        Set<String> serverElementSet = sets.get(0);
        Set<String> clientElementSet = sets.get(1);
        // create instances
        UpsiServer<String> server = UpsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        UpsiClient<String> client = UpsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        int randomTaskId = Math.abs(new SecureRandom().nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        UpsiServerThread<String> serverThread = new UpsiServerThread<>(
            server, MAX_CLIENT_ELEMENT_SIZE, serverElementSet, clientElementSet.size()
        );
        UpsiClientThread<String> clientThread = new UpsiClientThread<>(
            client, MAX_CLIENT_ELEMENT_SIZE, clientElementSet
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify result
            Set<String> psiResult = clientThread.getIntersectionSet();
            LOGGER.info("Server: The Communication costs {}MB", serverRpc.getSendByteLength() * 1.0 / (1024 * 1024));
            LOGGER.info("Client: The Communication costs {}MB", clientRpc.getSendByteLength() * 1.0 / (1024 * 1024));
            sets.get(0).retainAll(sets.get(1));
            Assert.assertTrue(sets.get(0).containsAll(psiResult));
            Assert.assertTrue(psiResult.containsAll(sets.get(0)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}
