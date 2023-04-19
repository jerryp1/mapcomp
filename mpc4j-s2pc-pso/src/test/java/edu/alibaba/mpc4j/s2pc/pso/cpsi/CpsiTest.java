package edu.alibaba.mpc4j.s2pc.pso.cpsi;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.psty19.Psty19CpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.CpsiFactory.CpsiType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * circuit PSI test.
 *
 * @author Liqiang Peng
 * @date 2023/1/30
 */
@RunWith(Parameterized.class)
public class CpsiTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpsiTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 99;
    /**
     * 较大数量
     */
    private static final int LARGE_SIZE = 1 << 14;
    /**
     * element byte length
     */
    private static final int ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = CommonUtils.getByteLength(ELEMENT_BIT_LENGTH);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // PSTY19 (default)
        configurations.add(new Object[] {
            CpsiType.PSTY19.name() + " (default)",
            new Psty19CpsiConfig.Builder().build(),
        });
        // PSTY19 (2 hash)
        configurations.add(new Object[] {
            CpsiType.PSTY19.name() + " (2 hash)",
            new Psty19CpsiConfig.Builder().setCuckooHashBinType(CuckooHashBinFactory.CuckooHashBinType.NAIVE_2_HASH).build(),
        });
        // PSTY19 (4 hash)
        configurations.add(new Object[] {
            CpsiType.PSTY19.name() + " (4 hash)",
            new Psty19CpsiConfig.Builder().setCuckooHashBinType(CuckooHashBinFactory.CuckooHashBinType.NAIVE_4_HASH).build(),
        });

        return configurations;
    }
    /**
     * server RPC
     */
    private final Rpc serverRpc;
    /**
     * client RPC
     */
    private final Rpc clientRpc;
    /**
     * the config
     */
    private final CpsiConfig config;

    public CpsiTest(String name, CpsiConfig config) {
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
    public void testDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, true);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, true);
    }

    @Test
    public void testLargeServerSize() {
        testPto(LARGE_SIZE, DEFAULT_SIZE, false);
    }

    @Test
    public void testLargeClientSize() {
        testPto(DEFAULT_SIZE, LARGE_SIZE, false);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, true);
    }

    public void testPto(int serverSetSize, int clientSetSize, boolean parallel) {
        CpsiServer server = CpsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        CpsiClient client = CpsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_set_size = {}，client_set_size = {}-----",
                server.getPtoDesc().getPtoName(), serverSetSize, clientSetSize
            );
            // generate the inputs
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSetSize, clientSetSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverElementSet = sets.get(0);
            Set<ByteBuffer> clientElementSet = sets.get(1);
            CpsiServerThread serverThread = new CpsiServerThread(server, serverElementSet, clientSetSize);
            CpsiClientThread clientThread = new CpsiClientThread(client, clientElementSet, serverSetSize);
            StopWatch stopWatch = new StopWatch();
            // execute the protocol
            stopWatch.start();
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            CpsiServerOutput serverOutput = serverThread.getServerOutput();
            SquareShareZ2Vector clientOutput = clientThread.getClientOutput();
            assertOutput(serverElementSet, clientElementSet, serverOutput, clientOutput);
            LOGGER.info("Server data_packet_num = {}, payload_bytes = {}B, send_bytes = {}B, time = {}ms",
                serverRpc.getSendDataPacketNum(), serverRpc.getPayloadByteLength(), serverRpc.getSendByteLength(),
                time
            );
            LOGGER.info("Client data_packet_num = {}, payload_bytes = {}B, send_bytes = {}B, time = {}ms",
                clientRpc.getSendDataPacketNum(), clientRpc.getPayloadByteLength(), clientRpc.getSendByteLength(),
                time
            );
            serverRpc.reset();
            clientRpc.reset();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }

    private void assertOutput(Set<ByteBuffer> serverElementSet, Set<ByteBuffer> clientElementSet,
                              CpsiServerOutput serverOutput, SquareShareZ2Vector clientOutput) {
        Set<ByteBuffer> expectIntersectionSet = new HashSet<>(serverElementSet);
        expectIntersectionSet.retainAll(clientElementSet);
        ByteBuffer[] table = serverOutput.getTable();
        BitVector z = serverOutput.getZ0().getBitVector().xor(clientOutput.getBitVector());
        int beta = serverOutput.getBeta();
        for (int i = 0; i < beta; i++) {
            if (expectIntersectionSet.contains(table[i])) {
                Assert.assertTrue(z.get(i));
            } else {
                Assert.assertFalse(z.get(i));
            }
        }
    }
}
