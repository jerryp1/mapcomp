package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.PlainPeqtFactory.PlainPeqtType;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.cgs22.Cgs22PlainPeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.naive.NaivePlainPeqtConfig;
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
 * plain private equality test.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
@RunWith(Parameterized.class)
public class PlainPeqtTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlainPeqtTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;
    /**
     * small l
     */
    private static final int SMALL_L = 1;
    /**
     * default l
     */
    private static final int DEFAULT_L = Integer.SIZE;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CGS22 (semi-honest)
        configurations.add(new Object[]{
            PlainPeqtType.CGS22.name() + " (semi-honest)",
            new Cgs22PlainPeqtConfig.Builder(SecurityModel.SEMI_HONEST).build()
        });
        // NAIVE (semi-honest)
        configurations.add(new Object[]{
            PlainPeqtType.NAIVE.name() + " (semi-honest)",
            new NaivePlainPeqtConfig.Builder(SecurityModel.SEMI_HONEST).build()
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
    private final PlainPeqtConfig config;

    public PlainPeqtTest(String name, PlainPeqtConfig config) {
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
    public void test1Num() {
        testPto(DEFAULT_L, 1, false);
    }

    @Test
    public void test2Num() {
        testPto(DEFAULT_L, 2, false);
    }

    @Test
    public void test8Num() {
        testPto(DEFAULT_L, 8, false);
    }

    @Test
    public void test7Num() {
        testPto(DEFAULT_L, 7, false);
    }

    @Test
    public void test9Num() {
        testPto(DEFAULT_L, 9, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_L, DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_L, DEFAULT_NUM, true);
    }

    @Test
    public void testSmallL() {
        testPto(SMALL_L, DEFAULT_NUM, false);
    }

    @Test
    public void test7L() {
        testPto(7, DEFAULT_NUM, false);
    }

    @Test
    public void test9L() {
        testPto(9, DEFAULT_NUM, false);
    }

    @Test
    public void testLargeNum() {
        testPto(DEFAULT_L, LARGE_NUM, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(DEFAULT_L, LARGE_NUM, false);
    }

    private void testPto(int l, int num, boolean parallel) {
        int byteL = CommonUtils.getByteLength(l);
        // create inputs
        byte[][] xs = IntStream.range(0, num)
            .parallel()
            .mapToObj(index -> BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM))
            .toArray(byte[][]::new);
        byte[][] ys = IntStream.range(0, num)
            .parallel()
            .mapToObj(index -> {
                boolean equal = (index % 2 == 0);
                if (equal) {
                    return BytesUtils.clone(xs[index]);
                } else {
                    return BytesUtils.randomByteArray(byteL, l, SECURE_RANDOM);
                }
            })
            .toArray(byte[][]::new);
        // init the protocol
        PlainPeqtParty sender = PlainPeqtFactory.createSender(senderRpc, receiverRpc.ownParty(), config);
        PlainPeqtParty receiver = PlainPeqtFactory.createReceiver(receiverRpc, senderRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PlainPeqtPartyThread senderThread = new PlainPeqtPartyThread(sender, l, xs);
            PlainPeqtPartyThread receiverThread = new PlainPeqtPartyThread(receiver, l, ys);
            StopWatch stopWatch = new StopWatch();
            // execute the protocol
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            long senderByteLength = senderRpc.getSendByteLength();
            long senderRound = senderRpc.getSendDataPacketNum();
            long receiverByteLength = receiverRpc.getSendByteLength();
            long receiverRound = receiverRpc.getSendDataPacketNum();
            senderRpc.reset();
            receiverRpc.reset();
            SquareShareZ2Vector z0 = senderThread.getZi();
            SquareShareZ2Vector z1 = receiverThread.getZi();
            BitVector z = z0.xor(z1, true).getBitVector();
            // verify
            assertOutput(num, xs, ys, z);
            LOGGER.info("Sender sends {}B / {} rounds, Receiver sends {}B / {} rounds, time = {}ms",
                senderByteLength, senderRound, receiverByteLength, receiverRound, time
            );
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.destroy();
        receiver.destroy();
    }

    private void assertOutput(int num, byte[][] xs, byte[][] ys, BitVector z) {
        Assert.assertEquals(num, z.bitNum());
        for (int index = 0; index < num; index++) {
            boolean xi = Arrays.equals(xs[index], ys[index]);
            if (!xi) {
                // not equal
                Assert.assertFalse(z.get(index));
            } else {
                // equal
                Assert.assertTrue(z.get(index));
            }
        }
    }
}
