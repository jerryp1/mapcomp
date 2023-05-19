package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.operator.DyadicBcOperator;
import edu.alibaba.mpc4j.common.circuit.operator.UnaryBcOperator;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.aby.AbyTestUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91.Bea91Z2cConfig;
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
 * batch Boolean circuit test.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
@RunWith(Parameterized.class)
public class BatchZ2cTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchZ2cTest.class);
    /**
     * random status
     */
    private static final SecureRandom SECURE_RANDOM = AbyTestUtils.SECURE_RANDOM;
    /**
     * default number of bits
     */
    private static final int DEFAULT_BIT_NUM = 999;
    /**
     * large number of bits
     */
    private static final int LARGE_BIT_NUM = (1 << 16) + 1;
    /**
     * vector length
     */
    private static final int VECTOR_LENGTH = 13;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRG+21
        configurations.add(new Object[]{
            Z2cFactory.BcType.RRG21.name(), new Bea91Z2cConfig.Builder().build()
        });
        // Bea91
        configurations.add(new Object[]{
            Z2cFactory.BcType.BEA91.name(), new Bea91Z2cConfig.Builder().build()
        });

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
    private final Z2cConfig config;

    public BatchZ2cTest(String name, Z2cConfig config) {
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
    public void test1BitNum() {
        testPto(1, false);
    }

    @Test
    public void test2BitNum() {
        testPto(2, false);
    }

    @Test
    public void test8BitNum() {
        testPto(8, false);
    }

    @Test
    public void test15BitNum() {
        testPto(15, true);
    }

    @Test
    public void testDefaultBitNum() {
        testPto(DEFAULT_BIT_NUM, false);
    }

    @Test
    public void testParallelDefaultBitNum() {
        testPto(DEFAULT_BIT_NUM, true);
    }

    @Test
    public void testLargeBitNum() {
        testPto(LARGE_BIT_NUM, false);
    }

    @Test
    public void testParallelLargeBitNum() {
        testPto(LARGE_BIT_NUM, true);
    }

    private void testPto(int bitNum, boolean parallel) {
        Z2cParty sender = Z2cFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        Z2cParty receiver = Z2cFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        for (DyadicBcOperator operator : DyadicBcOperator.values()) {
            testDyadicOperator(sender, receiver, operator, bitNum);
        }
        for (UnaryBcOperator operator : UnaryBcOperator.values()) {
            testUnaryOperator(sender, receiver, operator, bitNum);
        }
        sender.destroy();
        receiver.destroy();
    }

    private void testDyadicOperator(Z2cParty sender, Z2cParty receiver, DyadicBcOperator operator, int maxBitNum) {
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate xs
        BitVector[] xVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> {
                // sample bitNum in [1, maxBitNum]
                int bitNum = SECURE_RANDOM.nextInt(maxBitNum) + 1;
                return BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
            })
            .toArray(BitVector[]::new);
        // generate ys
        BitVector[] yVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> {
                int bitNum = xVectors[index].bitNum();
                return BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
            })
            .toArray(BitVector[]::new);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            BatchDyadicZ2cSenderThread senderThread
                = new BatchDyadicZ2cSenderThread(sender, operator, xVectors, yVectors);
            BatchDyadicZ2cReceiverThread receiverThread
                = new BatchDyadicZ2cReceiverThread(receiver, operator, xVectors, yVectors);
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
            BitVector[] zVectors = senderThread.getExpectVectors();
            // (plain, plain)
            Assert.assertArrayEquals(zVectors, senderThread.getSendPlainPlainVectors());
            Assert.assertArrayEquals(zVectors, receiverThread.getRecvPlainPlainVectors());
            // (plain, secret)
            Assert.assertArrayEquals(zVectors, senderThread.getSendPlainSecretVectors());
            Assert.assertArrayEquals(zVectors, receiverThread.getRecvPlainSecretVectors());
            // (secret, plain)
            Assert.assertArrayEquals(zVectors, senderThread.getSendSecretPlainVectors());
            Assert.assertArrayEquals(zVectors, receiverThread.getRecvSecretPlainVectors());
            // (secret, secret)
            Assert.assertArrayEquals(zVectors, senderThread.getSendSecretSecretVectors());
            Assert.assertArrayEquals(zVectors, receiverThread.getRecvSecretSecretVectors());

            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} ({}) end-----", sender.getPtoDesc().getPtoName(), operator.name());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void testUnaryOperator(Z2cParty sender, Z2cParty receiver, UnaryBcOperator operator, int maxBitNum) {
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate xs
        BitVector[] xVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> {
                // sample bitNum in [1, maxBitNum]
                int bitNum = SECURE_RANDOM.nextInt(maxBitNum) + 1;
                return BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
            })
            .toArray(BitVector[]::new);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            BatchUnaryZ2cSenderThread senderThread = new BatchUnaryZ2cSenderThread(sender, operator, xVectors);
            BatchUnaryZ2cReceiverThread receiverThread = new BatchUnaryZ2cReceiverThread(receiver, operator, xVectors);
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
            BitVector[] zVectors = senderThread.getExpectVectors();
            // (plain)
            Assert.assertArrayEquals(zVectors, senderThread.getSendPlainVectors());
            Assert.assertArrayEquals(zVectors, receiverThread.getRecvPlainVectors());
            // (secret)
            Assert.assertArrayEquals(zVectors, senderThread.getSendSecretVectors());
            Assert.assertArrayEquals(zVectors, receiverThread.getRecvSecretVectors());

            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} ({}) end-----", sender.getPtoDesc().getPtoName(), operator.name());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
