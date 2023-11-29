package edu.alibaba.mpc4j.s2pc.aby.basics.bit2a;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory.Bit2aTypes;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.tuple.TupleBit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Bit2a test.
 *
 * @author Li Peng
 * @date 2023/10/12
 */
@RunWith(Parameterized.class)
public class Bit2aTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bit2aTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;
    /**
     * small Zl
     */
    private static final Zl SMALL_ZL = ZlFactory.createInstance(EnvType.STANDARD, 1);
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Integer.SIZE);
    /**
     * silent
     */
    private static final boolean silent = true;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // KVH+21 small zl
        configurations.add(new Object[]{
            Bit2aTypes.KVH21.name(), new Kvh21Bit2aConfig.Builder(SMALL_ZL, silent).build()
        });

        // KVH+21 default zl
        configurations.add(new Object[]{
            Bit2aTypes.KVH21.name(), new Kvh21Bit2aConfig.Builder(DEFAULT_ZL, silent).build()
        });

        // Tuple small zl
        configurations.add(new Object[]{
            Bit2aTypes.TUPLE.name(), new TupleBit2aConfig.Builder(SMALL_ZL, silent).build()
        });

        // Tuple default zl
        configurations.add(new Object[]{
            Bit2aTypes.TUPLE.name(), new TupleBit2aConfig.Builder(DEFAULT_ZL, silent).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final Bit2aConfig config;

    public Bit2aTest(String name, Bit2aConfig config) {
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
    public void test8Num() {
        testPto(8, false);
    }

    @Test
    public void test7Num() {
        testPto(7, false);
    }

    @Test
    public void test9Num() {
        testPto(9, false);
    }

    @Test
    public void test19Num() {
        testPto(19, false);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, false);
    }

    @Test
    public void testParallelDefaultNum() {
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
        // create inputs
        BitVector x0 = BitVectorFactory.createRandom(num, SECURE_RANDOM);
        BitVector x1 = BitVectorFactory.createRandom(num, SECURE_RANDOM);
        SquareZ2Vector shareX0 = SquareZ2Vector.create(x0, false);
        SquareZ2Vector shareX1 = SquareZ2Vector.create(x1, false);
        // init the protocol
        Bit2aParty sender = Bit2aFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Bit2aParty receiver = Bit2aFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            Bit2aSenderThread senderThread = new Bit2aSenderThread(sender, shareX0, config.getZl().getL());
            Bit2aReceiverThread receiverThread = new Bit2aReceiverThread(receiver, shareX1, config.getZl().getL());
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
            // verify
            SquareZlVector shareZ0 = senderThread.getZ0();
            SquareZlVector shareZ1 = receiverThread.getZ1();
            assertOutput(x0, x1, shareZ0, shareZ1);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(BitVector x0, BitVector x1, SquareZlVector z0, SquareZlVector z1) {
        int num = x0.bitNum();
        Assert.assertEquals(num, z0.getNum());
        Assert.assertEquals(num, z1.getNum());
        BitVector x = x0.xor(x1);
        BigInteger[] z = z0.getZlVector().add(z1.getZlVector()).getElements();
        for (int i = 0; i < num; i++) {
            boolean xi = x.get(i);
            if (xi) {
                Assert.assertEquals(z[i], BigInteger.ONE);
            } else {
                Assert.assertEquals(z[i], BigInteger.ZERO);
            }
        }
    }
}
