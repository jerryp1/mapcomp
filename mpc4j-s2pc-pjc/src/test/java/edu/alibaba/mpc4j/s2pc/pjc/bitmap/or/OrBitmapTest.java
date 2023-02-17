package edu.alibaba.mpc4j.s2pc.pjc.bitmap.or;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
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
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{"secret v. secret", false, false});
        configurations.add(new Object[]{"public v. secret", true, false});
        configurations.add(new Object[]{"secret v. public", false, true});
        configurations.add(new Object[]{"public v. public", true, true});

        return configurations;
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

    public OrBitmapTest(String name, boolean xPublic, boolean yPublic) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = new SecureBitmapConfig.Builder().build();
        this.xPublic = xPublic;
        this.yPublic = yPublic;
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
    public void test1() {
        testBitmap(1, false);
    }

    @Test
    public void test2() {
        testBitmap(2, false);
    }

    @Test
    public void test8() {
        testBitmap(8, false);
    }

    @Test
    public void testDefaultNum() {
        testBitmap(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testBitmap(DEFAULT_NUM, true);
    }

    @Test
    public void testLargeNum() {
        testBitmap(LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testBitmap(LARGE_NUM, true);
    }

    private void testBitmap(int maxNum, boolean parallel) {
        BitmapParty sender = new BitmapSender(senderRpc, receiverRpc.ownParty(), config);
        BitmapParty receiver = new BitmapReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
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
        sender.destroy();
        receiver.destroy();
    }

    private void assertOutput(RoaringBitmap xPlain, RoaringBitmap yPlain, RoaringBitmap zResult) {
        RoaringBitmap zPlain = xPlain.clone();
        zPlain.or(yPlain);
        Assert.assertEquals(zResult.toString(), zPlain.toString());
        Assert.assertEquals(zResult, zResult);
    }
}
