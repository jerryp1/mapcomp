package edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.BitOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.BitOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.BitOtTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.direct.DirectNcBitOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.kk13.Kk13NcBitOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.*;
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
 * NC-BitOt协议测试。
 *
 * @author Hanwen Feng
 * @date 2022/08/12
 */
@RunWith(Parameterized.class)
public class NcBitOtTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NcCotTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_NUM = 100;
    /**
     * 默认轮数
     */
    private static final int DEFAULT_ROUND = 2;
    /**
     * 较大数量
     */
    private static final int LARGE_NUM = 1 << 18;
    /**
     * 较大轮数
     */
    private static final int LARGE_ROUND = 5;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // DIRECT
        configurations.add(new Object[] {
                NcBitOtFactory.NcBitOtType.DIRECT.name() ,
                new DirectNcBitOtConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // KK13  l = 3
        configurations.add(new Object[] {
                NcBitOtFactory.NcBitOtType.KK13.name() + " l = 3" ,
                new Kk13NcBitOtConfig.Builder().setL(3).build(),
        });
        // KK13  l = 6
        configurations.add(new Object[] {
                NcBitOtFactory.NcBitOtType.KK13.name() + " l = 6" ,
                new Kk13NcBitOtConfig.Builder().setL(6).build(),
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
    private final NcBitOtConfig config;

    public NcBitOtTest(String name, NcBitOtConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        NcBitOtSender sender = NcBitOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcBitOtReceiver receiver = NcBitOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Round1Num() {
        NcBitOtSender sender = NcBitOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcBitOtReceiver receiver = NcBitOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1, 1);
    }


    @Test
    public void test2Round2Num() {
        NcBitOtSender sender = NcBitOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcBitOtReceiver receiver = NcBitOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2, 2);
    }

    @Test
    public void testDefaultRoundDefaultNum() {
        NcBitOtSender sender = NcBitOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcBitOtReceiver receiver = NcBitOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM, DEFAULT_ROUND);
    }

    @Test
    public void testParallelDefaultRoundDefaultNum() {
        NcBitOtSender sender = NcBitOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcBitOtReceiver receiver = NcBitOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM, DEFAULT_ROUND);
    }

    @Test
    public void test12LogNum() {
        NcBitOtSender sender = NcBitOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcBitOtReceiver receiver = NcBitOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1 << 12, DEFAULT_ROUND);
    }

    @Test
    public void test16LogNum() {
        NcBitOtSender sender = NcBitOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcBitOtReceiver receiver = NcBitOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1 << 16, DEFAULT_ROUND);
    }

    @Test
    public void testLargeRoundDefaultNum() {
        NcBitOtSender sender = NcBitOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcBitOtReceiver receiver = NcBitOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM, LARGE_ROUND);
    }

    @Test
    public void testParallelLargeRoundDefaultNum() {
        NcBitOtSender sender = NcBitOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcBitOtReceiver receiver = NcBitOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM, LARGE_ROUND);
    }

    @Test
    public void testDefaultRoundLargeNum() {
        NcBitOtSender sender = NcBitOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcBitOtReceiver receiver = NcBitOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_NUM, DEFAULT_ROUND);
    }

    @Test
    public void testParallelDefaultRoundLargeNum() {
        NcBitOtSender sender = NcBitOtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        NcBitOtReceiver receiver = NcBitOtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_NUM, DEFAULT_ROUND);
    }


    private void testPto(NcBitOtSender sender, NcBitOtReceiver receiver, int num, int round) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            NcBitOtSenderThread senderThread = new NcBitOtSenderThread(sender, num, round);
            NcBitOtReceiverThread receiverThread = new NcBitOtReceiverThread(receiver, num, round);
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
            senderRpc.reset();
            long receiverByteLength = receiverRpc.getSendByteLength();
            receiverRpc.reset();
            BitOtSenderOutput[] senderOutputs = senderThread.getSenderOutputs();
            BitOtReceiverOutput[] receiverOutputs = receiverThread.getReceiverOutputs();
            // 验证结果
            IntStream.range(0, round).forEach(index -> {
                try {
                    BitOtTestUtils.assertOutput(num, senderOutputs[index], receiverOutputs[index]);
                } catch (AssertionError e) {
                    System.out.println("Index: " + index);
                    System.out.println("Break");
                }
                    }
            );
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                    senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
