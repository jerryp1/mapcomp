package edu.alibaba.mpc4j.s2pc.opf.opprf.rb;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.opprf.OpprfTestUtils;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfFactory.RbopprfType;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.cgs22.Cgs22RbopprfConfig;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Related-Batch OPPRF test.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
@RunWith(Parameterized.class)
public class RbopprfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RbopprfTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default l
     */
    private static final int DEFAULT_L = CommonConstants.STATS_BIT_LENGTH;
    /**
     * large l
     */
    private static final int LARGE_L = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * default batch size
     */
    private static final int DEFAULT_BATCH_NUM = 1000;
    /**
     * large batch size
     */
    private static final int LARGE_BATCH_NUM = 1 << 16;
    /**
     * default point num
     */
    private static final int DEFAULT_POINT_NUM = DEFAULT_BATCH_NUM * 3;
    /**
     * large point num
     */
    private static final int LARGE_POINT_NUM = LARGE_BATCH_NUM * 3;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            RbopprfType.CGS22.name(), new Cgs22RbopprfConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * the sender RPC
     */
    private final Rpc senderRpc;
    /**
     * the receiver RPC
     */
    private final Rpc receiverRpc;
    /**
     * the config
     */
    private final RbopprfConfig config;

    public RbopprfTest(String name, RbopprfConfig config) {
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
    public void test2Batch() {
        testPto(DEFAULT_L, 2, DEFAULT_POINT_NUM, false);
    }

    @Test
    public void test1Point() {
        testPto(DEFAULT_L, DEFAULT_BATCH_NUM, 1, false);
    }

    @Test
    public void test2Point() {
        testPto(DEFAULT_L, DEFAULT_BATCH_NUM, 2, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_L, DEFAULT_BATCH_NUM, DEFAULT_POINT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_L, DEFAULT_BATCH_NUM, DEFAULT_POINT_NUM, true);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_L, LARGE_BATCH_NUM, LARGE_POINT_NUM, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_L, LARGE_BATCH_NUM, LARGE_POINT_NUM, true);
    }

    private void testPto(int l, int batchNum, int pointNum, boolean parallel) {
        // create the sender and the receiver
        RbopprfSender sender = RbopprfFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        RbopprfReceiver receiver = RbopprfFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info(
                "-----test {}, l = {}, batch_num = {}, point_num = {}, parallel = {}-----",
                sender.getPtoDesc().getPtoName(), l, batchNum, pointNum, parallel
            );
            // generate the sender input
            byte[][][] senderInputArrays = OpprfTestUtils.generateSenderInputArrays(batchNum, pointNum, SECURE_RANDOM);
            byte[][][] senderTargetArrays = OpprfTestUtils.generateSenderTargetArrays(l, senderInputArrays, SECURE_RANDOM);
            // generate the receiver input
            byte[][] receiverInputArray = OpprfTestUtils.generateReceiverInputArray(senderInputArrays, SECURE_RANDOM);
            RbopprfSenderThread senderThread = new RbopprfSenderThread(sender, l, senderInputArrays, senderTargetArrays);
            RbopprfReceiverThread receiverThread = new RbopprfReceiverThread(receiver, l, receiverInputArray, pointNum);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            byte[][][] receiverTargetArray = receiverThread.getReceiverTargetArray();
            // verify the correctness
            assertOutput(l, senderInputArrays, senderTargetArrays, receiverInputArray, receiverTargetArray);
            LOGGER.info("Sender data_packet_num = {}, payload_bytes = {}B, send_bytes = {}B, time = {}ms",
                senderRpc.getSendDataPacketNum(), senderRpc.getPayloadByteLength(), senderRpc.getSendByteLength(),
                time
            );
            LOGGER.info("Receiver data_packet_num = {}, payload_bytes = {}B, send_bytes = {}B, time = {}ms",
                receiverRpc.getSendDataPacketNum(), receiverRpc.getPayloadByteLength(), receiverRpc.getSendByteLength(),
                time
            );
            senderRpc.reset();
            receiverRpc.reset();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
    }

    private void assertOutput(int l, byte[][][] senderInputArrays, byte[][][] senderTargetArrays,
                              byte[][] receiverInputArray, byte[][][] receiverTargetArray) {
        int d = config.getD();
        int byteL = CommonUtils.getByteLength(l);
        int batchNum = senderInputArrays.length;
        Assert.assertEquals(batchNum, senderTargetArrays.length);
        Assert.assertEquals(batchNum, receiverInputArray.length);
        Assert.assertEquals(batchNum, receiverTargetArray.length);
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            int batchPointNum = senderInputArrays[batchIndex].length;
            Assert.assertEquals(batchPointNum, senderTargetArrays[batchIndex].length);
            byte[][] senderInputArray = senderInputArrays[batchIndex];
            byte[][] senderTargetArray = senderTargetArrays[batchIndex];
            byte[] receiverInput = receiverInputArray[batchIndex];
            byte[][] receiverTargets = receiverTargetArray[batchIndex];
            Assert.assertEquals(d, receiverTargets.length);
            // the receiver output must have l-bit length
            for (byte[] receiverTarget : receiverTargets) {
                Assert.assertTrue(BytesUtils.isFixedReduceByteArray(receiverTarget, byteL, l));
            }
            for (int pointIndex = 0; pointIndex < batchPointNum; pointIndex++) {
                byte[] senderInput = senderInputArray[pointIndex];
                byte[] senderTarget = senderTargetArray[pointIndex];
                // the sender target must have l-bit length
                Assert.assertTrue(BytesUtils.isFixedReduceByteArray(senderTarget, byteL, l));
                if (Arrays.equals(senderInput, receiverInput)) {
                    int targetEqualNum = 0;
                    for (int b = 0; b < d; b++) {
                        if (Arrays.equals(receiverTargets[b], senderTarget)) {
                            targetEqualNum++;
                        }
                    }
                    Assert.assertEquals(1, targetEqualNum);
                } else {
                    for (int b = 0; b < d; b++) {
                        Assert.assertFalse(Arrays.equals(receiverTargets[b], senderTarget));
                    }
                }
            }
        });
    }
}
