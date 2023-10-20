package edu.alibaba.mpc4j.s2pc.aby.basics.b2a;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory.B2aTypes;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.dsz15.Dsz15B2aConfig;
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
import java.util.stream.IntStream;

/**
 * B2a test.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
@RunWith(Parameterized.class)
public class B2aTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(B2aTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * small Zl
     */
    private static final Zl SMALL_ZL = ZlFactory.createInstance(EnvType.STANDARD, 1);
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Integer.SIZE);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // DSZ15 small zl
        configurations.add(new Object[]{
            B2aTypes.DSZ15.name(), new Dsz15B2aConfig.Builder(SMALL_ZL).build()
        });

        // DSZ15 default zl
        configurations.add(new Object[]{
            B2aTypes.DSZ15.name(), new Dsz15B2aConfig.Builder(DEFAULT_ZL).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final B2aConfig config;

    public B2aTest(String name, B2aConfig config) {
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
        int l = config.getZl().getL();
        // create inputs
        BitVector[] x0s = IntStream.range(0, l).mapToObj(i -> BitVectorFactory.createRandom(num, SECURE_RANDOM)).toArray(BitVector[]::new);
        BitVector[] x1s = IntStream.range(0, l).mapToObj(i -> BitVectorFactory.createRandom(num, SECURE_RANDOM)).toArray(BitVector[]::new);

        SquareZ2Vector[] shareX0s = IntStream.range(0, l).mapToObj(i -> SquareZ2Vector.create(x0s[i], false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] shareX1s = IntStream.range(0, l).mapToObj(i -> SquareZ2Vector.create(x1s[i], false)).toArray(SquareZ2Vector[]::new);

        // init the protocol
        B2aParty sender = B2aFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        B2aParty receiver = B2aFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            B2aSenderThread senderThread = new B2aSenderThread(sender, shareX0s, config.getZl().getL());
            B2aReceiverThread receiverThread = new B2aReceiverThread(receiver, shareX1s, config.getZl().getL());
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
            assertOutput(x0s, x1s, shareZ0, shareZ1);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(BitVector[] x0, BitVector[] x1, SquareZlVector z0, SquareZlVector z1) {
        int num = x0[0].bitNum();
        int l = z0.getZl().getL();

        BitVector[] x = IntStream.range(0, l).mapToObj(i -> x0[i].xor(x1[i])).toArray(BitVector[]::new);
        ZlDatabase database = ZlDatabase.create(EnvType.INLAND_JDK, true, x);
        BigInteger[] trueZ = database.getBigIntegerData();
        BigInteger[] resultZ = z0.getZlVector().add(z1.getZlVector()).getElements();
        // assert
        IntStream.range(0, num).forEach(i -> Assert.assertEquals(trueZ[i], resultZ[i]));
    }
}
