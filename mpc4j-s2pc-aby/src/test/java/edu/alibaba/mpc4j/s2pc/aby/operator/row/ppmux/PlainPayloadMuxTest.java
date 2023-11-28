package edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory.PlainMuxType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.rrg21.Xxx23PlainPayloadMuxConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
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
public class PlainPayloadMuxTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlainPayloadMuxTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Xxx23 default zl
        configurations.add(new Object[]{
            PlainMuxType.Xxx23.name(), new Xxx23PlainPayloadMuxConfig.Builder(false).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final PlainPayloadMuxConfig config;

    public PlainPayloadMuxTest(String name, PlainPayloadMuxConfig config) {
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
        PlainPayloadMuxParty sender = PlainPlayloadMuxFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PlainPayloadMuxParty receiver = PlainPlayloadMuxFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PlainPayloadMuxSenderThread senderThread = new PlainPayloadMuxSenderThread(sender, shareX0, y);
            PlainPayloadMuxReceiverThread receiverThread = new PlainPayloadMuxReceiverThread(receiver, shareX1, 64, false);
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

        SecureRandom secureRandom = new SecureRandom();
        int randomBitLen = secureRandom.nextInt(64) + 1;
        BitVector[] value = IntStream.range(0, randomBitLen).mapToObj(i -> BitVectorFactory.createRandom(num, secureRandom)).toArray(BitVector[]::new);
        // init the protocol
        sender = PlainPlayloadMuxFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        receiver = PlainPlayloadMuxFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PlainPayloadMuxSenderThread senderThread = new PlainPayloadMuxSenderThread(sender, shareX0, value);
            PlainPayloadMuxReceiverThread receiverThread = new PlainPayloadMuxReceiverThread(receiver, shareX1, randomBitLen, true);
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
            SquareZ2Vector[] shareZ0 = senderThread.getZ0Binary();
            SquareZ2Vector[] shareZ1 = receiverThread.getZ1Binary();
            assertOutput(x0, x1, value, shareZ0, shareZ1);
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

    private void assertOutput(BitVector x0, BitVector x1, BitVector[] y, SquareZ2Vector[] z0, SquareZ2Vector[] z1) {
        BitVector[] res = IntStream.range(0, z0.length).mapToObj(i -> z0[i].getBitVector().xor(z1[i].getBitVector())).toArray(BitVector[]::new);
        BitVector x = x0.xor(x1);
        for(int i = 0; i < y.length; i++){
            assert Arrays.equals(res[i].getBytes(), (y[i].and(x)).getBytes());
        }
    }
}
