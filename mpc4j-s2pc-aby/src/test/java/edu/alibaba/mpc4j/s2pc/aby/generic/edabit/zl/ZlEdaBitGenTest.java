package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.AbyTestUtils;
import edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.ZlEdaBitGenFactory.ZlEdaBitGenType;
import edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.egk20.Egk20ZlEdaBitGenConfig;
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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Zl edaBit generation test.
 *
 * @author Weiran Liu
 * @date 2023/5/18
 */
@RunWith(Parameterized.class)
public class ZlEdaBitGenTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlEdaBitGenTest.class);
    /**
     * random status
     */
    private static final SecureRandom SECURE_RANDOM = AbyTestUtils.SECURE_RANDOM;
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 99;
    /**
     * large num
     */
    private static final int LARGE_NUM = (1 << 14) + 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (Zl zl : AbyTestUtils.ZLS) {
            int l = zl.getL();
            // EGK20
            configurations.add(new Object[]{
                ZlEdaBitGenType.EGK20.name() + " (" + SecurityModel.SEMI_HONEST + ", l = " + l + ")",
                new Egk20ZlEdaBitGenConfig.Builder(SecurityModel.SEMI_HONEST, zl, false).build(),
            });
        }

        return configurations;
    }

    /**
     * sender RPC
     */
    private final Rpc senderRpc;
    /**
     * receiver RPC
     */
    private final Rpc receiverRpc;
    /**
     * config
     */
    private final ZlEdaBitGenConfig config;

    public ZlEdaBitGenTest(String name, ZlEdaBitGenConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Before
    public void connect() {
        senderRpc.connect();
        receiverRpc.connect();
    }

    @After
    public void disconnect() {
        senderRpc.disconnect();
        receiverRpc.disconnect();
    }

    @Test
    public void test1Num() {
        testPto(1, false);
    }

    @Test
    public void test2Num() {
        testPto(2, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(LARGE_NUM, true);
    }

    private void testPto(int num, boolean parallel) {
        ZlEdaBitGenParty sender = ZlEdaBitGenFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        ZlEdaBitGenParty receiver = ZlEdaBitGenFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlEdaBitGenPartyThread senderThread = new ZlEdaBitGenPartyThread(sender, num);
            ZlEdaBitGenPartyThread receiverThread = new ZlEdaBitGenPartyThread(receiver, num);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            long senderByteLength = senderRpc.getSendByteLength();
            long receiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            SquareZlEdaBitVector senderOutput = senderThread.getOutput();
            SquareZlEdaBitVector receiverOutput = receiverThread.getOutput();
            PlainZlEdaBitVector plainZlEdaBitVector = senderOutput.reveal(receiverOutput);
            // verify
            assertOutput(num, plainZlEdaBitVector);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
    }

    private void assertOutput(int num, PlainZlEdaBitVector plainEdaBitVector) {
        Assert.assertEquals(num, plainEdaBitVector.getNum());
        for (int index = 0; index < num; index++) {
            Assert.assertEquals(plainEdaBitVector.getZlElement(index), plainEdaBitVector.getZ2Element(index));
        }
    }
}
