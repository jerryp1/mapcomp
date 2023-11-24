package edu.alibaba.mpc4j.s2pc.pso.main.plpsi;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiFactory.PlpsiType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

@RunWith(Parameterized.class)
public class MainPlpsiTest extends AbstractTwoPartyPtoTest {
    /**
     * sender RPC
     */
    protected final Rpc firstRpc;
    /**
     * receiver RPC
     */
    protected final Rpc secondRpc;

    private static final CuckooHashBinType[] CUCKOO_HASH_BIN_TYPES = new CuckooHashBinType[] {
        CuckooHashBinType.NO_STASH_PSZ18_3_HASH,
    };

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        String[] silentChoices = new String[]{"_silent_", "_no_silent_"};
        for (CuckooHashBinType type : CUCKOO_HASH_BIN_TYPES) {
            for(String silentChoice : silentChoices){
                // PSTY19
                configurations.add(new Object[]{
                    PlpsiType.PSTY19.name() + silentChoice + type.name(), "plpsi/psty19" + silentChoice + type.name().toLowerCase() + ".txt",
                });
                // RS21
                configurations.add(new Object[]{
                    PlpsiType.RS21.name() + silentChoice + type.name(), "plpsi/rs21" + silentChoice + type.name().toLowerCase() + ".txt",
                });
            }
        }

        return configurations;
    }

    /**
     * file name
     */
    private final String filePath;

    public MainPlpsiTest(String name, String filePath) {
        super(name);
        RpcManager rpcManager = new MemoryRpcManager(2);
        firstRpc = rpcManager.getRpc(0);
        secondRpc = rpcManager.getRpc(1);
        this.filePath = filePath;
    }

    @Test
    public void testPsi() throws InterruptedException {
        Properties properties = readConfig(filePath);
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
