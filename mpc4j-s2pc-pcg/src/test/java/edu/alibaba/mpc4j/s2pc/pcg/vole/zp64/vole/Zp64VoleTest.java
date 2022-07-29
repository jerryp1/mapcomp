package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.vole;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.np01.Np01BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.vole.kos16.Kos16ShZp64VoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.Zp64VoleTestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
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
public class Zp64VoleTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Zp64VoleTest.class);
    /**
     * 随机状态\
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
        configurations.add(new Object[]{
                Zp64VoleFactory.Zp64VoleType.KOS16_SEMI_HONEST.name(),
                new Kos16ShZp64VoleConfig.Builder()
                        .setBaseOtConfig(new Np01BaseOtConfig.Builder().setEnvType(EnvType.STANDARD_JDK).build()).build(),
        });

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
    private final Zp64VoleConfig config;

    public Zp64VoleTest(String name, Zp64VoleConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        Zp64VoleSender sender = Zp64VoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64VoleReceiver receiver = Zp64VoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        Zp64VoleSender sender = Zp64VoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64VoleReceiver receiver = Zp64VoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1, DEFAULT_PRIME);
    }

    @Test
    public void test2Num() {
        Zp64VoleSender sender = Zp64VoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64VoleReceiver receiver = Zp64VoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2, DEFAULT_PRIME);
    }

    @Test
    public void testDefault() {
        Zp64VoleSender sender = Zp64VoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64VoleReceiver receiver = Zp64VoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM, DEFAULT_PRIME);
    }

    @Test
    public void testParallelDefault() {
        Zp64VoleSender sender = Zp64VoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64VoleReceiver receiver = Zp64VoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM, DEFAULT_PRIME);
    }

    @Test
    public void testLargeNum() {
        Zp64VoleSender sender = Zp64VoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64VoleReceiver receiver = Zp64VoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_NUM, DEFAULT_PRIME);
    }

    @Test
    public void testLargePrime() {
        Zp64VoleSender sender = Zp64VoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64VoleReceiver receiver = Zp64VoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM, LARGE_PRIME);
    }

    @Test
    public void testParallelLargePrime() {
        Zp64VoleSender sender = Zp64VoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64VoleReceiver receiver = Zp64VoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM, LARGE_PRIME);
    }

    private void testPto(Zp64VoleSender sender, Zp64VoleReceiver receiver, int num, long prime) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            // Δ的取值范围是[0, 2^k)
            long delta = LongUtils.randomNonNegative(1L << (LongUtils.ceilLog2(prime) - 1), SECURE_RANDOM);
            long[] x = IntStream.range(0, num)
                    .mapToLong(index -> LongUtils.randomNonNegative(prime, SECURE_RANDOM))
                    .toArray();
            Zp64VoleSenderThread senderThread = new Zp64VoleSenderThread(sender, prime, x);
            Zp64VoleReceiverThread receiverThread = new Zp64VoleReceiverThread(receiver, prime, delta, num);
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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testResetDelta() {
        Zp64VoleSender sender = Zp64VoleFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Zp64VoleReceiver receiver = Zp64VoleFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
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
            Zp64VoleSenderThread senderThread = new Zp64VoleSenderThread(sender, DEFAULT_PRIME, x);
            Zp64VoleReceiverThread receiverThread = new Zp64VoleReceiverThread(receiver, DEFAULT_PRIME, delta, DEFAULT_NUM);
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
            senderThread = new Zp64VoleSenderThread(sender, DEFAULT_PRIME, x);
            receiverThread = new Zp64VoleReceiverThread(receiver, DEFAULT_PRIME, delta, DEFAULT_NUM);
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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
