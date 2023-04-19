package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiConfig;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * unbalanced circuit PSI test
 *
 * @author Liqiang Peng
 * @date 2023/4/18
 */
@RunWith(Parameterized.class)
public class UcpsiTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcpsiTest.class);

    /**
     * server element size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 12;
    /**
     * client element size
     */
    private static final int CLIENT_ELEMENT_SIZE = 1 << 6;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // PSTY19
        configurations.add(new Object[]{
            UcpsiFactory.UcpsiType.PSTY19.name(), new Psty19UcpsiConfig.Builder().build()
        });

        return configurations;
    }
    /**
     * server rpc
     */
    private final Rpc serverRpc;
    /**
     * client rpc
     */
    private final Rpc clientRpc;
    /**
     * the unbalanced PSI config
     */
    private final UcpsiConfig config;

    public UcpsiTest(String name, UcpsiConfig config) {
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
        List<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSize, clientSize, CommonConstants.BLOCK_BYTE_LENGTH);
        Set<ByteBuffer> serverElementSet = sets.get(0);
        Set<ByteBuffer> clientElementSet = sets.get(1);
        // create instance
        UcpsiServer server = UcpsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        UcpsiClient client = UcpsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        int randomTaskId = Math.abs(new SecureRandom().nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        UcpsiServerThread serverThread = new UcpsiServerThread(server, serverElementSet, clientSize);
        UcpsiClientThread clientThread = new UcpsiClientThread(client, clientElementSet, serverSize);
        try {
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
            long senderByteLength = serverRpc.getSendByteLength();
            long senderRound = serverRpc.getSendDataPacketNum();
            long receiverByteLength = clientRpc.getSendByteLength();
            long receiverRound = clientRpc.getSendDataPacketNum();
            serverRpc.reset();
            clientRpc.reset();
            // verify
            UcpsiClientOutput cpsiClientOutput = clientThread.getOutputs();
            SquareShareZ2Vector serverOutput = serverThread.getOutputs();
            Set<ByteBuffer> intersection = new HashSet<>();
            Assert.assertEquals(cpsiClientOutput.getZ0().getNum(), serverOutput.getNum());
            BitVector z = cpsiClientOutput.getZ0().xor(serverOutput, true).getBitVector();
            for (int i = 0; i < z.bitNum(); i++) {
                if (z.get(i)) {
                    intersection.add(cpsiClientOutput.getTable().get(i));
                }
            }
            sets.get(0).retainAll(sets.get(1));
            Assert.assertTrue(sets.get(0).containsAll(intersection));
            Assert.assertTrue(intersection.containsAll(sets.get(0)));
            LOGGER.info("Sender sends {}B / {} rounds, Receiver sends {}B / {} rounds, time = {}ms",
                senderByteLength, senderRound, receiverByteLength, receiverRound, time
            );
            LOGGER.info("-----test {} end-----", server.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}
