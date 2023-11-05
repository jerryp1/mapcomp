package edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory.PlainBitMuxType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.Xxx23.Xxx23PlainBitMuxConfig;
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
 * Plain bit mux test.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
@RunWith(Parameterized.class)
public class PlainBitMuxTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlainBitMuxTest.class);
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
            PlainBitMuxType.Xxx23.name(), new Xxx23PlainBitMuxConfig.Builder(DEFAULT_ZL).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final PlainBitMuxConfig config;

    public PlainBitMuxTest(String name, PlainBitMuxConfig config) {
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
        BitVector x = BitVectorFactory.createRandom(num, SECURE_RANDOM);
//        BitVector x1 = BitVectorFactory.createRandom(num, SECURE_RANDOM);
//        SquareZ2Vector shareX0 = SquareZ2Vector.create(x, false);
//        SquareZ2Vector shareX1 = SquareZ2Vector.create(x1, false);

        long[] y = IntStream.range(0, num).map(i -> SECURE_RANDOM.nextInt(Integer.MAX_VALUE)).mapToLong(i -> i).toArray();
        SquareZlVector y0 = SquareZlVector.createRandom(config.getZl(), num, SECURE_RANDOM);
        SquareZlVector y1 = SquareZlVector.create(config.getZl(), IntStream.range(0, num)
            .mapToObj(i -> config.getZl().sub(BigInteger.valueOf(y[i]), y0.getZlVector().getElement(i))).toArray(BigInteger[]::new), false);
        // init the protocol
        PlainBitMuxParty sender = PlainBitMuxFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PlainBitMuxParty receiver = PlainBitMuxFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PlainBitMuxSenderThread senderThread = new PlainBitMuxSenderThread(sender, null, y0);
            PlainBitMuxReceiverThread receiverThread = new PlainBitMuxReceiverThread(receiver, x, y1);
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
            assertOutput(x, y, shareZ0, shareZ1);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(BitVector x, long[] y, SquareZlVector z0, SquareZlVector z1) {
        int num = x.bitNum();
        Assert.assertEquals(num, z0.getNum());
        Assert.assertEquals(num, z1.getNum());
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
