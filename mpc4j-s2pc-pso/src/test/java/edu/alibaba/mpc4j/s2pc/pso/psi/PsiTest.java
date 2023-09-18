package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.cm20.Cm20PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.czz22.Czz22PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.gmr21.Gmr21PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.Hfh99ByteEccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.Hfh99EccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.kkrt16.Kkrt16PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.prty19.Prty19FastPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.prty19.Prty19LowPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.prty20.Prty20SmPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.rt21.Rt21ElligatorPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17.Ra17ByteEccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17.Ra17EccPsiConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * PSI tests.
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
@RunWith(Parameterized.class)
public class PsiTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsiTest.class);
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 99;
    /**
     * element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * large size
     */
    private static final int LARGE_SIZE = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RT21
        configurations.add(new Object[] {
            PsiType.RT21.name(), new Rt21ElligatorPsiConfig.Builder().build(),
        });
        // CM20
        configurations.add(new Object[] {
            PsiType.CM20.name(), new Cm20PsiConfig.Builder().build(),
        });
        // CZZ22
        configurations.add(new Object[] {
            PsiType.CZZ22.name(), new Czz22PsiConfig.Builder().build(),
        });
        // GMR21
        configurations.add(new Object[] {
            PsiType.GMR21.name(), new Gmr21PsiConfig.Builder(false).build(),
        });
        configurations.add(new Object[] {
            PsiType.GMR21.name(), new Gmr21PsiConfig.Builder(true).build(),
        });
        // PRTY20
        configurations.add(new Object[] {
            PsiType.PRTY20_SEMI_HONEST.name(), new Prty20SmPsiConfig.Builder().build(),
        });
        configurations.add(new Object[] {
            PsiType.PRTY20_SEMI_HONEST.name() + " (" + Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT + ")",
            new Prty20SmPsiConfig.Builder().setPaxosType(Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT).build(),
        });
        // RA17_ECC
        configurations.add(new Object[] {
            PsiType.RA17_ECC.name(), new Ra17EccPsiConfig.Builder().build(),
        });
        // RA17_BYTE_ECC
        configurations.add(new Object[] {
            PsiType.RA17_BYTE_ECC.name(), new Ra17ByteEccPsiConfig.Builder().build(),
        });
//        configurations.add(new Object[] {
//            PsiType.RA17.name() + "ECC", new Ra17PsiConfig.Builder().
//            setSqOprfConfig(new Ra17EccSqOprfConfig.Builder().build()).build(),
//        });
//        // PSZ14_GBF
//        configurations.add(new Object[] {
//            PsiType.PSZ14_GBF.name(), new Psz14GbfPsiConfig.Builder().build(),
//        });
//        // PSZ14_ORI
//        configurations.add(new Object[] {
//            PsiType.PSZ14.name() + "_ORI", new Psz14PsiConfig.Builder().setOprfConfig(new Psz14OriOprfConfig.Builder().build()).build(),
//        });
//        // PSZ14
//        configurations.add(new Object[] {
//            PsiType.PSZ14.name(), new Psz14PsiConfig.Builder().build(),
//        });
        // PRTY19_FAST
        configurations.add(new Object[] {
            PsiType.PRTY19_FAST.name(), new Prty19FastPsiConfig.Builder().build(),
        });
        // PRTY19_LOW
        configurations.add(new Object[] {
            PsiType.PRTY19_LOW.name(), new Prty19LowPsiConfig.Builder().build(),
        });

        // KKRT16 (no-stash)
        configurations.add(new Object[] {
            PsiFactory.PsiType.KKRT16.name() + " (no-stash)",
            new Kkrt16PsiConfig.Builder().setCuckooHashBinType(CuckooHashBinType.NO_STASH_NAIVE).build(),
        });
        // KKRT16 (4 hash)
        configurations.add(new Object[] {
            PsiFactory.PsiType.KKRT16.name() + " (4 hash)",
            new Kkrt16PsiConfig.Builder().setCuckooHashBinType(CuckooHashBinType.NAIVE_4_HASH).build(),
        });
        // KKRT16
        configurations.add(new Object[] {
            PsiFactory.PsiType.KKRT16.name(), new Kkrt16PsiConfig.Builder().build(),
        });
        // HFH99_BYTE_ECC
        configurations.add(new Object[] {
            PsiFactory.PsiType.HFH99_BYTE_ECC.name(), new Hfh99ByteEccPsiConfig.Builder().build(),
        });
        // HFH99_ECC (compress)
        configurations.add(new Object[] {
            PsiFactory.PsiType.HFH99_ECC.name() + " (compress)",
            new Hfh99EccPsiConfig.Builder().setCompressEncode(true).build(),
        });
        // HFH99_ECC (uncompress)
        configurations.add(new Object[] {
            PsiFactory.PsiType.HFH99_ECC.name() + " (uncompress)",
            new Hfh99EccPsiConfig.Builder().setCompressEncode(false).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final PsiConfig config;

    public PsiTest(String name, PsiConfig config) {
       super(name);
        this.config = config;
    }

    @Test
    public void test1() {
        testPto(1, 1, false);
    }

    @Test
    public void test2() {
        testPto(2, 2, false);
    }

    @Test
    public void test10() {
        testPto(10, 10, false);
    }

    @Test
    public void testLargeServerSize() {
        testPto(DEFAULT_SIZE, 10, false);
    }

    @Test
    public void testLargeClientSize() {
        testPto(10, DEFAULT_SIZE, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, true);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, true);
    }

    private void testPto(int serverSetSize, int clientSetSize, boolean parallel) {
        PsiServer<ByteBuffer> server = PsiFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        PsiClient<ByteBuffer> client = PsiFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_size = {}，client_size = {}-----",
                server.getPtoDesc().getPtoName(), serverSetSize, clientSetSize
            );
            // generate sets
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSetSize, clientSetSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverSet = sets.get(0);
            Set<ByteBuffer> clientSet = sets.get(1);
            PsiServerThread serverThread = new PsiServerThread(server, serverSet, clientSet.size());
            PsiClientThread clientThread = new PsiClientThread(client, clientSet, serverSet.size());
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            serverThread.start();
            clientThread.start();
            // stop
            serverThread.join();
            clientThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            assertOutput(serverSet, clientSet, clientThread.getIntersectionSet());
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(Set<ByteBuffer> serverSet, Set<ByteBuffer> clientSet, Set<ByteBuffer> outputIntersectionSet) {
        Set<ByteBuffer> expectIntersectionSet = new HashSet<>(serverSet);
        expectIntersectionSet.retainAll(clientSet);
        Assert.assertTrue(outputIntersectionSet.containsAll(expectIntersectionSet));
        Assert.assertTrue(expectIntersectionSet.containsAll(outputIntersectionSet));
    }
}
