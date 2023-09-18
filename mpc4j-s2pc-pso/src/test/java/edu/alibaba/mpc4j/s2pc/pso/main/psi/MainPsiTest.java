package edu.alibaba.mpc4j.s2pc.pso.main.psi;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

/**
 * PSI main tests.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
@RunWith(Parameterized.class)
public class MainPsiTest extends AbstractTwoPartyPtoTest {
    /**
     * sender RPC
     */
    protected final Rpc firstRpc;
    /**
     * receiver RPC
     */
    protected final Rpc secondRpc;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // HFH99_ECC
        configurations.add(new Object[] {
            PsiType.HFH99_ECC.name() + " (uncompress)", "psi/hfh99_ecc_compress_false.txt",
        });
        // HFH99_BYTE_ECC
        configurations.add(new Object[] {
            PsiType.HFH99_ECC.name() + " (compressed)", "psi/hfh99_ecc.txt",
        });
        // HFH99_BYTE_ECC
        configurations.add(new Object[] {
            PsiType.HFH99_BYTE_ECC.name(), "psi/hfh99_byte_ecc.txt",
        });
        // KKRT16 (no-stash)
        configurations.add(new Object[] {
            PsiFactory.PsiType.KKRT16.name() + " (no-stash)", "psi/kkrt16_noStashNaive.txt",
        });
        // KKRT16 (4 hash)
        configurations.add(new Object[] {
            PsiFactory.PsiType.KKRT16.name() + " (4 hash)", "psi/kkrt16_naive4hash.txt",
        });
        // KKRT16
        configurations.add(new Object[] {
            PsiFactory.PsiType.KKRT16.name(), "psi/kkrt16_naive3hash.txt",
        });
        // DCW13
        configurations.add(new Object[] {
            PsiType.DCW13.name(), "psi/dcw13.txt",
        });
        // PSZ14
        configurations.add(new Object[] {
            PsiType.PSZ14.name(), "psi/psz14.txt",
        });
        // RA17
        configurations.add(new Object[] {
            PsiType.RA17_BYTE_ECC.name(), "psi/ra17_byte_ecc.txt",
        });
        configurations.add(new Object[] {
            PsiType.RA17_ECC.name(), "psi/ra17_ecc.txt",
        });
        // PRTY19_FAST
        configurations.add(new Object[] {
            PsiType.PRTY19_FAST.name(), "psi/prty19_fast.txt",
        });
        // PRTY20_SEMI_HONEST
        configurations.add(new Object[] {
            PsiType.PRTY20_SEMI_HONEST.name(), "psi/prty20_semi_honest.txt",
        });
        // CM20
        configurations.add(new Object[] {
            PsiType.CM20.name(), "psi/cm20.txt",
        });
        // GMR21
        configurations.add(new Object[] {
            PsiType.GMR21.name(), "psi/gmr21.txt",
        });
        // CZZ22
        configurations.add(new Object[] {
            PsiType.CZZ22.name(), "psi/czz22.txt",
        });

        return configurations;
    }

    /**
     * file name
     */
    private final String filePath;

    public MainPsiTest(String name, String filePath) {
        super(name);
        RpcManager rpcManager = new MemoryRpcManager(2);
        firstRpc = rpcManager.getRpc(0);
        secondRpc = rpcManager.getRpc(1);
        this.filePath = filePath;
    }

    @Test
    public void testPsi() throws InterruptedException {
        Properties properties = readConfig(filePath);
        runTest(new PsiMain(properties));
    }

    private void runTest(PsiMain psiMain) throws InterruptedException {
        MainPsiServerThread serverThread = new MainPsiServerThread(firstRpc, secondRpc.ownParty(), psiMain);
        MainPsiClientThread clientThread = new MainPsiClientThread(secondRpc, firstRpc.ownParty(), psiMain);
        serverThread.start();
        Thread.sleep(1000);
        clientThread.start();
        serverThread.join();
        clientThread.join();
    }

    private Properties readConfig(String path) {
        String configPath = Objects.requireNonNull(MainPsiTest.class.getClassLoader().getResource(path)).getPath();
        return PropertiesUtils.loadProperties(configPath);
    }
}
