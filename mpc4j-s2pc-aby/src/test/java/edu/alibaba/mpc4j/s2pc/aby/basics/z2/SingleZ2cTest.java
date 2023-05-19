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
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.rrg21.Rrg21Z2cConfig;
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

/**
 * single Boolean circuit test.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
@RunWith(Parameterized.class)
public class SingleZ2cTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleZ2cTest.class);
    /**
     * random status
     */
    private static final SecureRandom SECURE_RANDOM = AbyTestUtils.SECURE_RANDOM;
    /**
     * default number of bits
     */
    private static final int DEFAULT_BIT_NUM = 1001;
    /**
     * large number of bits
     */
    private static final int LARGE_BIT_NUM = (1 << 18) - 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRG+21
        configurations.add(new Object[] {
            Z2cFactory.BcType.RRG21.name(), new Rrg21Z2cConfig.Builder().build()
        });
        // Bea91
        configurations.add(new Object[] {
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

    public SingleZ2cTest(String name, Z2cConfig config) {
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
        testPto(15, false);
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

    private void testDyadicOperator(Z2cParty sender, Z2cParty receiver, DyadicBcOperator operator, int bitNum) {
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        BitVector xVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        // generate y
        BitVector yVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            SingleDyadicZ2cSenderThread senderThread = new SingleDyadicZ2cSenderThread(sender, operator, xVector, yVector);
            SingleDyadicZ2cReceiverThread receiverThread = new SingleDyadicZ2cReceiverThread(receiver, operator, xVector, yVector);
            StopWatch stopWatch = new StopWatch();

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
            BitVector zVector = senderThread.getExpectVector();
            // (plain, plain)
            Assert.assertEquals(zVector, senderThread.getSenPlainPlainVector());
            Assert.assertEquals(zVector, receiverThread.getRecvPlainPlainVector());
            // (plain, secret)
            Assert.assertEquals(zVector, senderThread.getSendPlainSecretVector());
            Assert.assertEquals(zVector, receiverThread.getRecvPlainSecretVector());
            // (secret, plain)
            Assert.assertEquals(zVector, senderThread.getSendSecretPlainVector());
            Assert.assertEquals(zVector, receiverThread.getRecvSecretPlainVector());
            // (secret, secret)
            Assert.assertEquals(zVector, senderThread.getSendSecretSecretVector());
            Assert.assertEquals(zVector, receiverThread.getRecvSecretSecretVector());

            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} ({}) end-----", sender.getPtoDesc().getPtoName(), operator.name());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void testUnaryOperator(Z2cParty sender, Z2cParty receiver, UnaryBcOperator operator, int bitNum) {
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // generate x
        BitVector xVector = BitVectorFactory.createRandom(bitNum, SECURE_RANDOM);
        try {
            LOGGER.info("-----test {} ({}) start-----", sender.getPtoDesc().getPtoName(), operator.name());
            SingleUnaryZ2cSenderThread senderThread = new SingleUnaryZ2cSenderThread(sender, operator, xVector);
            SingleUnaryZ2cReceiverThread receiverThread = new SingleUnaryZ2cReceiverThread(receiver, operator, xVector);
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
            BitVector zVector = senderThread.getExpectVector();
            // (plain)
            Assert.assertEquals(zVector, senderThread.getSendPlainVector());
            Assert.assertEquals(zVector, receiverThread.getRecvPlainVector());
            // (secret)
            Assert.assertEquals(zVector, senderThread.getSendSecretVector());
            Assert.assertEquals(zVector, receiverThread.getRecvSecretVector());

            LOGGER.info("Sender sends {}B, Receiver sends {}B, time = {}ms",
                senderByteLength, receiverByteLength, time
            );
            LOGGER.info("-----test {} ({}) end-----", sender.getPtoDesc().getPtoName(), operator.name());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
