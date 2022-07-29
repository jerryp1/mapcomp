package edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BooleanTriple;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BtgTestUtils;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.alsz13.Alsz13RbtgConfig;
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

/**
 * RBTG协议测试。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
@RunWith(Parameterized.class)
public class RbtgTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RbtgTest.class);
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

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // ALSZ13
        configurations.add(new Object[]{
            RbtgFactory.RbtgType.ALSZ13.name(), new Alsz13RbtgConfig.Builder().build(),
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
    private final RbtgConfig config;

    public RbtgTest(String name, RbtgConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        RbtgParty sender = RbtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        RbtgParty receiver = RbtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1Num() {
        RbtgParty sender = RbtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        RbtgParty receiver = RbtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 1);
    }

    @Test
    public void test2Num() {
        RbtgParty sender = RbtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        RbtgParty receiver = RbtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, 2);
    }

    @Test
    public void testDefault() {
        RbtgParty sender = RbtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        RbtgParty receiver = RbtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefault() {
        RbtgParty sender = RbtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        RbtgParty receiver = RbtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        RbtgParty sender = RbtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        RbtgParty receiver = RbtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testPto(sender, receiver, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        RbtgParty sender = RbtgFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        RbtgParty receiver = RbtgFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testPto(sender, receiver, LARGE_NUM);
    }

    private void testPto(RbtgParty sender, RbtgParty receiver, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            RbtgPartyThread senderThread = new RbtgPartyThread(sender, num);
            RbtgPartyThread receiverThread = new RbtgPartyThread(receiver, num);
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
            BooleanTriple senderOutput = senderThread.getOutput();
            BooleanTriple receiverOutput = receiverThread.getOutput();
            // 验证结果
            BtgTestUtils.assertOutput(num, senderOutput, receiverOutput);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
