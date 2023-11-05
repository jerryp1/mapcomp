package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainmux;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainmux.PlainMuxFactory.PlainMuxType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainmux.rrg21.Xxx23PlainMuxConfig;
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
 * Plain mux test.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
@RunWith(Parameterized.class)
public class PlainMuxTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlainMuxTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Long.SIZE);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Xxx23 default zl
        configurations.add(new Object[]{
            PlainMuxType.Xxx23.name(), new Xxx23PlainMuxConfig.Builder(DEFAULT_ZL).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final PlainMuxConfig config;

    public PlainMuxTest(String name, PlainMuxConfig config) {
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

        long[] y = IntStream.range(0, num).map(i -> SECURE_RANDOM.nextInt(Integer.MAX_VALUE)).mapToLong(i -> i).toArray();
        // init the protocol
        PlainMuxParty sender = PlainMuxFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PlainMuxParty receiver = PlainMuxFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PlainMuxSenderThread senderThread = new PlainMuxSenderThread(sender, shareX0, y);
            PlainMuxReceiverThread receiverThread = new PlainMuxReceiverThread(receiver, shareX1);
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
            assertOutput(x0, x1, y, shareZ0, shareZ1);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(BitVector x0, BitVector x1, long[] y, SquareZlVector z0, SquareZlVector z1) {
        int num = x0.bitNum();
        Assert.assertEquals(num, z0.getNum());
        Assert.assertEquals(num, z1.getNum());
        BitVector x = x0.xor(x1);
        BigInteger[] z = z0.getZlVector().add(z1.getZlVector()).getElements();
        for (int i = 0; i < num; i++) {
            boolean xi = x.get(i);
            if (xi) {
                Assert.assertEquals(z[i].longValue(), y[i]);
            } else {
                Assert.assertEquals(z[i].longValue(), 0);
            }
        }
    }
}
