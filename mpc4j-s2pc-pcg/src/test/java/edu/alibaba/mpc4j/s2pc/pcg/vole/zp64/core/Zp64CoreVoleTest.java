package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core.kos16.Kos16ShZp64CoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.core.Zp64CoreVoleFactory.Zp64CoreVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleTestUtils;
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
import java.util.stream.IntStream;


/**
 * Zp64-Vole协议测试
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
@RunWith(Parameterized.class)
public class Zp64CoreVoleTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Zp64CoreVoleTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * 较大数量
     */
    private static final int LARGE_NUM = 1 << 18;
    /**
     * 默认素数域
     */
    private static final long DEFAULT_PRIME = ZpManager.getPrime(32).longValue();
    /**
     * 较大素数域
     */
    private static final long LARGE_PRIME = ZpManager.getPrime(62).longValue();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // KOS16_SEMI_HONEST
        configurations.add(
            new Object[]{Zp64CoreVoleType.KOS16_SEMI_HONEST.name(), new Kos16ShZp64CoreVoleConfig.Builder().build(),}
        );

        return configurations;
    }

    /**
     * 发送方
     */
    private final Rpc senderRpc;
    /**
     * 接收方
     */
    private final Rpc receiverRpc;
    /**
     * 协议类型
     */
    private final Zp64CoreVoleConfig config;

    public Zp64CoreVoleTest(String name, Zp64CoreVoleConfig config) {
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
        testPto(1, DEFAULT_PRIME, false);
    }

    @Test
    public void test2Num() {
        testPto(2, DEFAULT_PRIME, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_NUM, DEFAULT_PRIME, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_NUM, DEFAULT_PRIME, true);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM, DEFAULT_PRIME, false);
    }

    @Test
    public void testLargePrime() {
        testPto(DEFAULT_NUM, LARGE_PRIME, false);
    }

    @Test
    public void testParallelLargePrime() {
        testPto(DEFAULT_NUM, LARGE_PRIME, true);
    }

    private void testPto(int num, long prime, boolean parallel) {
        Zp64CoreVoleSender sender = Zp64CoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64CoreVoleReceiver receiver = Zp64CoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            // Δ的取值范围是[0, 2^k)
            long delta = LongUtils.randomNonNegative(1L << (LongUtils.ceilLog2(prime) - 1), SECURE_RANDOM);
            long[] x = IntStream.range(0, num)
                .mapToLong(index -> LongUtils.randomNonNegative(prime, SECURE_RANDOM))
                .toArray();
            Zp64CoreVoleSenderThread senderThread = new Zp64CoreVoleSenderThread(sender, prime, x);
            Zp64CoreVoleReceiverThread receiverThread = new Zp64CoreVoleReceiverThread(receiver, prime, delta, num);
            StopWatch stopWatch = new StopWatch();
            // 开始执行协议
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            long senderByteLength = senderRpc.getSendByteLength();
            long receiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            Zp64VoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Zp64VoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            // 验证结果
            Zp64VoleTestUtils.assertOutput(num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
            sender.destroy();
            receiver.destroy();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testResetDelta() {
        Zp64CoreVoleSender sender = Zp64CoreVoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64CoreVoleReceiver receiver = Zp64CoreVoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} (reset Δ) start-----", sender.getPtoDesc().getPtoName());
            // Δ的取值范围是[0, 2^k)
            long delta = LongUtils.randomNonNegative(1L << (LongUtils.ceilLog2(DEFAULT_PRIME) - 1), SECURE_RANDOM);
            long[] x = IntStream.range(0, DEFAULT_NUM)
                .mapToLong(index -> LongUtils.randomNonNegative(DEFAULT_PRIME, SECURE_RANDOM))
                .toArray();
            // 第一次执行
            Zp64CoreVoleSenderThread senderThread = new Zp64CoreVoleSenderThread(sender, DEFAULT_PRIME, x);
            Zp64CoreVoleReceiverThread receiverThread = new Zp64CoreVoleReceiverThread(receiver, DEFAULT_PRIME, delta, DEFAULT_NUM);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long firstTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            long firstSenderByteLength = senderRpc.getSendByteLength();
            long firstReceiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            Zp64VoleSenderOutput senderOutput = senderThread.getSenderOutput();
            Zp64VoleReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            Zp64VoleTestUtils.assertOutput(DEFAULT_NUM, senderOutput, receiverOutput);
            // 第二次执行，重置Δ
            delta = LongUtils.randomNonNegative(1L << (LongUtils.ceilLog2(DEFAULT_PRIME) - 1), SECURE_RANDOM);
            x = IntStream.range(0, DEFAULT_NUM)
                .mapToLong(index -> LongUtils.randomNonNegative(DEFAULT_PRIME, SECURE_RANDOM))
                .toArray();
            senderThread = new Zp64CoreVoleSenderThread(sender, DEFAULT_PRIME, x);
            receiverThread = new Zp64CoreVoleReceiverThread(receiver, DEFAULT_PRIME, delta, DEFAULT_NUM);
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long secondTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            long secondSenderByteLength = senderRpc.getSendByteLength();
            long secondReceiverByteLength = receiverRpc.getSendByteLength();
            senderRpc.reset();
            receiverRpc.reset();
            Zp64VoleSenderOutput secondSenderOutput = senderThread.getSenderOutput();
            Zp64VoleReceiverOutput secondReceiverOutput = receiverThread.getReceiverOutput();
            Zp64VoleTestUtils.assertOutput(DEFAULT_NUM, secondSenderOutput, secondReceiverOutput);
            // Δ应该不等
            Assert.assertNotEquals(secondReceiverOutput.getDelta(), receiverOutput.getDelta());
            // 通信量应该相等
            Assert.assertEquals(firstReceiverByteLength, secondReceiverByteLength);
            Assert.assertEquals(firstSenderByteLength, secondSenderByteLength);
            LOGGER.info("1st round, Send. {}B, Recv. {}B, {}ms",
                firstSenderByteLength, firstReceiverByteLength, firstTime
            );
            LOGGER.info("2nd round, Send. {}B, Recv. {}B, {}ms",
                secondSenderByteLength, secondReceiverByteLength, secondTime
            );
            LOGGER.info("-----test {} (reset Δ) end-----", sender.getPtoDesc().getPtoName());
            sender.destroy();
            receiver.destroy();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
