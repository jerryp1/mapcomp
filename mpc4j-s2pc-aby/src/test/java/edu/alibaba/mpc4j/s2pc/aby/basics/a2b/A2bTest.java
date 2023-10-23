package edu.alibaba.mpc4j.s2pc.aby.basics.a2b;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.crypto.matrix.database.Zl64Database;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.dsz15.Dsz15A2bConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory.B2aTypes;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * A2b test.
 *
 * @author Li Peng
 * @date 2023/10/20
 */
@RunWith(Parameterized.class)
public class A2bTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(A2bTest.class);
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
            B2aTypes.DSZ15.name(), new Dsz15A2bConfig.Builder(SMALL_ZL).build()
        });

        // DSZ15 default zl
        configurations.add(new Object[]{
            B2aTypes.DSZ15.name(), new Dsz15A2bConfig.Builder(DEFAULT_ZL).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final A2bConfig config;

    public A2bTest(String name, A2bConfig config) {
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
        assert l <= Long.SIZE;

        ZlVector x0 = ZlVector.createRandom(config.getZl(), num, SECURE_RANDOM);
        ZlVector x1 = ZlVector.createRandom(config.getZl(), num, SECURE_RANDOM);

        SquareZlVector x0Share = SquareZlVector.create(x0, false);
        SquareZlVector x1Share = SquareZlVector.create(x1, false);

        // init the protocol
        A2bParty sender = A2bFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        A2bParty receiver = A2bFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            A2bSenderThread senderThread = new A2bSenderThread(sender, x0Share, config.getZl().getL());
            A2bReceiverThread receiverThread = new A2bReceiverThread(receiver, x1Share, config.getZl().getL());
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
            SquareZ2Vector[] shareZ0 = senderThread.getZ0();
            SquareZ2Vector[] shareZ1 = receiverThread.getZ1();
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

    private void assertOutput(ZlVector x0, ZlVector x1, SquareZ2Vector[] z0, SquareZ2Vector[] z1) {
        int num = x0.getNum();
        int l = x0.getZl().getL();

        long[] x = Arrays.stream(x0.add(x1).getElements()).mapToLong(BigInteger::longValue).toArray();
        Zl64Database zl64Database = Zl64Database.create(l, x);
        BitVector[] tureZ = zl64Database.bitPartition(EnvType.INLAND_JDK, true);
        BitVector[] resultZ = IntStream.range(0, l).mapToObj(i -> z0[i].getBitVector().xor(z1[i].getBitVector())).toArray(BitVector[]::new);
        // assert
        BitVector zeros = BitVectorFactory.createZeros(num);
        IntStream.range(0, l).forEach(i -> Assert.assertEquals(tureZ[i].xor(resultZ[i]), zeros));
    }
}
