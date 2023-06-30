package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.test.AbstractThreePartyPtoTest;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.aid.AiderThread;
import edu.alibaba.mpc4j.s2pc.pcg.aid.TrustDealAider;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgFactory.ZlMtgType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.impl.offline.OfflineZlMtgConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Zl multiplication triple generation aid test.
 *
 * @author Weiran Liu
 * @date 2023/6/26
 */
@RunWith(Parameterized.class)
public class ZlMtgAidTest extends AbstractThreePartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZlMtgAidTest.class);
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

        Zl[] zls = new Zl[]{
            ZlFactory.createInstance(EnvType.STANDARD, 1),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L - 1),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L),
            ZlFactory.createInstance(EnvType.STANDARD, LongUtils.MAX_L + 1),
            ZlFactory.createInstance(EnvType.STANDARD, CommonConstants.BLOCK_BIT_LENGTH),
        };

        for (Zl zl : zls) {
            int l = zl.getL();
            // OFFLINE
            configurations.add(new Object[]{
                ZlMtgType.OFFLINE.name() + " (l = " + l + ", " + SecurityModel.TRUSTED_DEALER + ")",
                new OfflineZlMtgConfig.Builder(SecurityModel.TRUSTED_DEALER, zl).build(),
            });
            // CACHE
            configurations.add(new Object[]{
                ZlMtgType.CACHE.name() + " (l = " + l + ", " + SecurityModel.TRUSTED_DEALER + ")",
                new OfflineZlMtgConfig.Builder(SecurityModel.TRUSTED_DEALER, zl).build(),
            });
        }

        return configurations;
    }

    /**
     * config
     */
    private final ZlMtgConfig config;

    public ZlMtgAidTest(String name, ZlMtgConfig config) {
        super(name);
        this.config = config;
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
        ZlMtgParty sender = ZlMtgFactory.createSender(firstRpc, secondRpc.ownParty(), thirdRpc.ownParty(), config);
        ZlMtgParty receiver = ZlMtgFactory.createReceiver(secondRpc, firstRpc.ownParty(), thirdRpc.ownParty(), config);
        TrustDealAider aider = new TrustDealAider(thirdRpc, firstRpc.ownParty(), secondRpc.ownParty());
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        aider.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        aider.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ZlMtgPartyThread senderThread = new ZlMtgPartyThread(sender, num);
            ZlMtgPartyThread receiverThread = new ZlMtgPartyThread(receiver, num);
            AiderThread aiderThread = new AiderThread(aider);
            STOP_WATCH.start();
            // start
            senderThread.start();
            receiverThread.start();
            aiderThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            ZlTriple senderOutput = senderThread.getOutput();
            ZlTriple receiverOutput = receiverThread.getOutput();
            ZlMtgTestUtils.assertOutput(config.getZl(), num, senderOutput, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
            aiderThread.join();
            new Thread(aider::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
