package edu.alibaba.mpc4j.s2pc.groupagg.main.view;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.junit.Test;

import java.util.Objects;
import java.util.Properties;

/**
 * @author Feng Han
 * @date 2024/7/24
 */
public class MainPkFkViewTest {
    /**
     * sender RPC
     */
    protected final Rpc firstRpc;
    /**
     * receiver RPC
     */
    protected final Rpc secondRpc;

    public MainPkFkViewTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        firstRpc = rpcManager.getRpc(0);
        secondRpc = rpcManager.getRpc(1);
    }

    @Test
    public void testBaseline() throws Exception {
        Properties properties = readConfig("view/conf_pk_fk_view_baseline.config");
        runTest(new PkFkViewMain(properties));
    }
    @Test
    public void testPhP24Psi() throws Exception {
        Properties properties = readConfig("view/conf_pk_fk_view_php24_psi.config");
        runTest(new PkFkViewMain(properties));
    }
    @Test
    public void testPhP24Pid() throws Exception {
        Properties properties = readConfig("view/conf_pk_fk_view_php24_pid.config");
        runTest(new PkFkViewMain(properties));
    }
    @Test
    public void testPhP24Pmap() throws Exception {
        Properties properties = readConfig("view/conf_pk_fk_view_php24_pmap.config");
        runTest(new PkFkViewMain(properties));
    }

    private void runTest(PkFkViewMain viewMain) throws InterruptedException {
        MainPkFkViewReceiverThread receiverThread = new MainPkFkViewReceiverThread(firstRpc, secondRpc.ownParty(), viewMain);
        MainPkFkViewSenderThread senderThread = new MainPkFkViewSenderThread(secondRpc, firstRpc.ownParty(), viewMain);
        receiverThread.start();
        Thread.sleep(1000);
        senderThread.start();
        receiverThread.join();
        senderThread.join();
    }

    private Properties readConfig(String path) {
        String configPath = Objects.requireNonNull(MainPkFkViewTest.class.getClassLoader().getResource(path)).getPath();
        return PropertiesUtils.loadProperties(configPath);
    }
}
