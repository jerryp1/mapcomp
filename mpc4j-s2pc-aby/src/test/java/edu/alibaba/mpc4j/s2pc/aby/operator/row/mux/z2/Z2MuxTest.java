package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory.Z2MuxType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.rrk20.Rrk20Z2MuxConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Z2 mux test.
 *
 * @author Feng Han
 * @date 2023/11/28
 */
@RunWith(Parameterized.class)
public class Z2MuxTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2MuxTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;
    /**
     * bit length
     */
    private static final int[] bitLens = new int[]{17, 64};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (int bitLen : bitLens) {
            // RRK+20 no-silent
            configurations.add(new Object[]{
                Z2MuxType.RRK20.name() + "_" + bitLen, new Rrk20Z2MuxConfig.Builder(false).build(), bitLen
            });
            // RRK+20 silent
            configurations.add(new Object[]{
                Z2MuxType.RRK20.name() + "_silent_" + bitLen, new Rrk20Z2MuxConfig.Builder(true).build(), bitLen
            });
        }

        return configurations;
    }

    /**
     * the config
     */
    private final Z2MuxConfig config;
    private final int bitLen;

    public Z2MuxTest(String name, Z2MuxConfig config, int bitLen) {
        super(name);
        this.config = config;
        this.bitLen = bitLen;
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
        BitVector[] y0 = IntStream.range(0, bitLen).mapToObj(i ->
            BitVectorFactory.createRandom(num, SECURE_RANDOM)).toArray(BitVector[]::new);
        BitVector[] y1 = IntStream.range(0, bitLen).mapToObj(i ->
            BitVectorFactory.createRandom(num, SECURE_RANDOM)).toArray(BitVector[]::new);
        SquareZ2Vector[] shareY0 = Arrays.stream(y0).map(each ->
            SquareZ2Vector.create(each, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] shareY1 = Arrays.stream(y1).map(each ->
            SquareZ2Vector.create(each, false)).toArray(SquareZ2Vector[]::new);
        // init the protocol
        Z2MuxParty sender = Z2MuxFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        Z2MuxParty receiver = Z2MuxFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            Z2MuxSenderThread senderThread = new Z2MuxSenderThread(sender, shareX0, shareY0);
            Z2MuxReceiverThread receiverThread = new Z2MuxReceiverThread(receiver, shareX1, shareY1);
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
            SquareZ2Vector[] shareZ0 = senderThread.getShareZ0();
            SquareZ2Vector[] shareZ1 = receiverThread.getShareZ1();
            assertOutput(x0, x1, y0, y1, shareZ0, shareZ1);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(BitVector x0, BitVector x1, BitVector[] y0, BitVector[] y1,
                              SquareZ2Vector[] shareZ0, SquareZ2Vector[] shareZ1) {
        int num = x0.bitNum();
        Assert.assertEquals(num, shareZ0[0].getNum());
        Assert.assertEquals(num, shareZ1[0].getNum());
        assert y0.length == shareZ0.length && y1.length == shareZ1.length && y0.length == y1.length;
        BitVector x = x0.xor(x1);
        BitVector[] y = IntStream.range(0, y0.length).mapToObj(i -> y0[i].xor(y1[i])).toArray(BitVector[]::new);
        BitVector[] z = IntStream.range(0, y0.length).mapToObj(i ->
            shareZ0[i].getBitVector().xor(shareZ1[i].getBitVector())).toArray(BitVector[]::new);
        for (int i = 0; i < y0.length; i++) {
            BitVector baseline = y[i].and(x);
            assert Arrays.equals(baseline.getBytes(), z[i].getBytes());
        }
    }
}
