package edu.alibaba.mpc4j.s2pc.pjc.main.pmap;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pjc.main.pid.MainPidTest;
import org.junit.Test;

import java.util.Objects;
import java.util.Properties;

public class MainPmapTest {
    /**
     * sender RPC
     */
    protected final Rpc firstRpc;
    /**
     * receiver RPC
     */
    protected final Rpc secondRpc;

    public MainPmapTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        firstRpc = rpcManager.getRpc(0);
        secondRpc = rpcManager.getRpc(1);
    }

    @Test
    public void testHpl24Silent() throws Exception {
        Properties properties = readConfig("pmap/conf_pmap_php24_silent.txt");
        runTest(new PmapMain(properties));
    }

    @Test
    public void testHpl24NoSilent() throws Exception {
        Properties properties = readConfig("pmap/conf_pmap_php24_no_silent.txt");
        runTest(new PmapMain(properties));
    }

    private void runTest(PmapMain pidMain) throws InterruptedException {
        MainPmapServerThread serverThread = new MainPmapServerThread(firstRpc, secondRpc.ownParty(), pidMain);
        MainPmapClientThread clientThread = new MainPmapClientThread(secondRpc, firstRpc.ownParty(), pidMain);
        serverThread.start();
        Thread.sleep(1000);
        clientThread.start();
        serverThread.join();
        clientThread.join();
    }

    private Properties readConfig(String path) {
        String configPath = Objects.requireNonNull(MainPidTest.class.getClassLoader().getResource(path)).getPath();
        return PropertiesUtils.loadProperties(configPath);
    }
}
