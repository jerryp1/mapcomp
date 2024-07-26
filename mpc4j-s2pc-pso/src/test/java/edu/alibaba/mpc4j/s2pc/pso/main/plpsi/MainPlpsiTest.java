package edu.alibaba.mpc4j.s2pc.pso.main.plpsi;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;
import java.util.Properties;

public class MainPlpsiTest {
    /**
     * sender RPC
     */
    protected final Rpc firstRpc;
    /**
     * receiver RPC
     */
    protected final Rpc secondRpc;

    public MainPlpsiTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        firstRpc = rpcManager.getRpc(0);
        secondRpc = rpcManager.getRpc(1);
    }

    @Test
    public void testRs21() throws InterruptedException {
        Properties properties = readConfig("plpsi/rs21_no_silent_no_stash_psz18_3_hash.txt");
        runTest(new PlpsiMain(properties));
    }

    private void runTest(PlpsiMain plpsiMain) throws InterruptedException {
        MainPlpsiServerThread serverThread = new MainPlpsiServerThread(firstRpc, secondRpc.ownParty(), plpsiMain);
        MainPlpsiClientThread clientThread = new MainPlpsiClientThread(secondRpc, firstRpc.ownParty(), plpsiMain);
        serverThread.start();
        Thread.sleep(1000);
        clientThread.start();
        serverThread.join();
        clientThread.join();
        Assert.assertTrue(serverThread.getSuccess());
        Assert.assertTrue(clientThread.getSuccess());
    }

    private Properties readConfig(String path) {
        String configPath = Objects.requireNonNull(MainPlpsiTest.class.getClassLoader().getResource(path)).getPath();
        return PropertiesUtils.loadProperties(configPath);
    }
}
