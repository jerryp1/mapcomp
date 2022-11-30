package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.liu22.Liu22BitmapConfig;
import org.apache.commons.lang3.StringUtils;
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

import static edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapFactory.BitmapType;
import static edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer.BIT_LENGTH;

/**
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/24
 */
@RunWith(Parameterized.class)
public class BitmapTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BitmapTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // Liu22
        configurationParams.add(new Object[]{
                BitmapType.LIU22.name(), new Liu22BitmapConfig.Builder().build(), false, false});
        // Liu22
        configurationParams.add(new Object[]{
                BitmapType.LIU22.name(), new Liu22BitmapConfig.Builder().build(), true, false});
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


    public BitmapTest(String name, BitmapConfig bitmapConfig, boolean xPublic, boolean yPublic) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        senderRpc = rpcManager.getRpc(0);
        receiverRpc = rpcManager.getRpc(1);
        this.config = bitmapConfig;
        this.xPublic = xPublic;
        this.yPublic = yPublic;
    }


    @Test
    public void test2() {
        BitmapParty server = BitmapFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BitmapParty client = BitmapFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testBitmap(server, client, 2);
    }

    @Test
    public void test5() {
        BitmapParty server = BitmapFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BitmapParty client = BitmapFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testBitmap(server, client, 5);
    }

    @Test
    public void testLarge() {
        BitmapParty server = BitmapFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        BitmapParty client = BitmapFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        testBitmap(server, client, BIT_LENGTH / 2);
    }

    @Test
    public void testTriple() {

        Kdf kdf = KdfFactory.createInstance(EnvType.STANDARD);
        byte[] a0Key = kdf.deriveKey(new byte[100]);
        System.out.println(123);
    }


    private void testBitmap(BitmapParty sender, BitmapParty receiver, int num) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);

        // 生成随机num个元素，并插入roaringBitmap
        RoaringBitmap xPlain = new RoaringBitmap();
        RoaringBitmap yPlain = new RoaringBitmap();
        xPlain.add(IntStream.range(0, num).map(i -> SECURE_RANDOM.nextInt(BIT_LENGTH)).toArray());
        yPlain.add(IntStream.range(0, num).map(i -> SECURE_RANDOM.nextInt(BIT_LENGTH)).toArray());

        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            BitmapSenderThread senderThread = new BitmapSenderThread(sender, xPlain, xPublic, yPublic ? yPlain : null, yPublic);
            BitmapReceiverThread receiverThread = new BitmapReceiverThread(receiver, xPublic ? xPlain : null, xPublic, yPlain, yPublic);

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

            RoaringBitmap zPlain = receiverThread.getOutput();

            // 验证结果
            assertOutput(xPlain, yPlain, zPlain);
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
        zPlain.and(yPlain);
        System.out.println("zResult(top100 elements):" + Arrays.toString(zResult.stream().limit(100).toArray()));
        System.out.println("zPlain(top100 elements):" + Arrays.toString(zPlain.stream().limit(100).toArray()));
        Assert.assertEquals(zResult.toString(), zPlain.toString());
        Assert.assertEquals(zResult, zResult);
        System.out.println(123);
    }

}
