package edu.alibaba.mpc4j.s2pc.pjc.bitmap.or;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.*;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Bitmap or tests.
 *
 * @author Li Peng
 * @date 2022/11/24
 */
@RunWith(Parameterized.class)
public class OrBitmapTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrBitmapTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认运算数量
     */
    private static final int DEFAULT_NUM = 1 << 3;
    /**
     * 较大运算数量
     */
    private static final int LARGE_NUM = 1 << 18;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        configurationParams.add(new Object[]{new SecureBitmapConfig.Builder().build(), false, false});
        configurationParams.add(new Object[]{new SecureBitmapConfig.Builder().build(), true, false});
        configurationParams.add(new Object[]{new SecureBitmapConfig.Builder().build(), false, true});
        configurationParams.add(new Object[]{new SecureBitmapConfig.Builder().build(), true, true});
        return configurationParams;
    }

    /**
     * 发送端
     */
    private final Rpc senderRpc;
    /**
     * 接收端
     */
    private final Rpc receiverRpc;
    /**
     * 协议类型
     */
    private final BitmapConfig config;
    /**
     * x是否为公开导线
     */
    private final boolean xPublic;
    /**
     * y是否为公开导线
     */
    private final boolean yPublic;

    public OrBitmapTest(BitmapConfig bitmapConfig, boolean xPublic, boolean yPublic) {
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = bitmapConfig;
        this.xPublic = xPublic;
        this.yPublic = yPublic;
    }

    @Test
    public void testPtoType() {
        BitmapParty sender = new BitmapSender(senderRpc, receiverRpc.ownParty(), config);
        BitmapParty receiver = new BitmapReceiver(receiverRpc, senderRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), sender.getPtoType());
        Assert.assertEquals(config.getPtoType(), receiver.getPtoType());
    }

    @Test
    public void test1() {
        BitmapParty sender = new BitmapSender(senderRpc, receiverRpc.ownParty(), config);
        BitmapParty receiver = new BitmapReceiver(receiverRpc, senderRpc.ownParty(), config);
        testBitmap(sender, receiver, 1);
    }

    @Test
    public void test2() {
        BitmapParty sender = new BitmapSender(senderRpc, receiverRpc.ownParty(), config);
        BitmapParty receiver = new BitmapReceiver(receiverRpc, senderRpc.ownParty(), config);
        testBitmap(sender, receiver, 2);
    }

    @Test
    public void test8() {
        BitmapParty sender = new BitmapSender(senderRpc, receiverRpc.ownParty(), config);
        BitmapParty receiver = new BitmapReceiver(receiverRpc, senderRpc.ownParty(), config);
        testBitmap(sender, receiver, 8);
    }

    @Test
    public void testDefaultNum() {
        BitmapParty sender = new BitmapSender(senderRpc, receiverRpc.ownParty(), config);
        BitmapParty receiver = new BitmapReceiver(receiverRpc, senderRpc.ownParty(), config);
        testBitmap(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefaultNum() {
        BitmapParty sender = new BitmapSender(senderRpc, receiverRpc.ownParty(), config);
        BitmapParty receiver = new BitmapReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testBitmap(sender, receiver, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        BitmapParty sender = new BitmapSender(senderRpc, receiverRpc.ownParty(), config);
        BitmapParty receiver = new BitmapReceiver(receiverRpc, senderRpc.ownParty(), config);
        testBitmap(sender, receiver, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        BitmapParty sender = new BitmapSender(senderRpc, receiverRpc.ownParty(), config);
        BitmapParty receiver = new BitmapReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(true);
        receiver.setParallel(true);
        testBitmap(sender, receiver, LARGE_NUM);
    }

    private void testBitmap(BitmapParty sender, BitmapParty receiver, int maxNum) {
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);

        // 生成随机num个元素，并插入roaringBitmap
        RoaringBitmap xPlain = new RoaringBitmap();
        RoaringBitmap yPlain = new RoaringBitmap();
        xPlain.add(IntStream.range(0, maxNum / 2 + 1).map(i -> SECURE_RANDOM.nextInt(maxNum)).toArray());
        yPlain.add(IntStream.range(0, maxNum / 2 + 1).map(i -> SECURE_RANDOM.nextInt(maxNum)).toArray());

        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            OrBitmapSenderThread senderThread = new OrBitmapSenderThread(sender, xPlain, xPublic, yPublic ? yPlain : null, yPublic, maxNum);
            OrBitmapReceiverThread receiverThread = new OrBitmapReceiverThread(receiver, xPublic ? xPlain : null, xPublic, yPlain, yPublic, maxNum);

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

            RoaringBitmap zResult = receiverThread.getOutput();

            // 验证结果
            assertOutput(xPlain, yPlain, zResult);
            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void assertOutput(RoaringBitmap xPlain, RoaringBitmap yPlain, RoaringBitmap zResult) {
        RoaringBitmap zPlain = xPlain.clone();
        zPlain.or(yPlain);
        System.out.println("zResult(top100 elements):" + Arrays.toString(zResult.stream().limit(100).toArray()));
        System.out.println("zPlain(top100 elements):" + Arrays.toString(zPlain.stream().limit(100).toArray()));
        Assert.assertEquals(zResult.toString(), zPlain.toString());
        Assert.assertEquals(zResult, zResult);
    }

}
