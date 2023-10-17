package edu.alibaba.mpc4j.s2pc.aby.operator.psorter;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.PermutableSorterFactory.PermutableSorterTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.ahi22.Ahi22PermutableSorterConfig;
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
 * Bit2a test.
 *
 * @author Li Peng
 * @date 2023/10/12
 */
@RunWith(Parameterized.class)
public class PermutableSorterTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermutableSorterTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1;
    /**
     * default num of sorted elements
     */
    private static final int DEFAULT_NUM_SORTED = 1 << 19;
    /**
     * large num of sorted elements
     */
    private static final int LARGE_NUM_SORTED = 1 << 18;
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

//        // AHI+22 small zl
//        Bit2aConfig bit2aConfig = new Kvh21Bit2aConfig.Builder(SMALL_ZL).build();
//        configurations.add(new Object[]{
//            PermutableSorterTypes.AHI22.name(), new Ahi22PermutableSorterConfig.Builder(bit2aConfig).build()
//        });

        // AHI+22 default zl
        Bit2aConfig bit2aConfig = new Kvh21Bit2aConfig.Builder(DEFAULT_ZL).build();
        configurations.add(new Object[]{
            PermutableSorterTypes.AHI22.name(), new Ahi22PermutableSorterConfig.Builder(bit2aConfig).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final PermutableSorterConfig config;

    public PermutableSorterTest(String name, PermutableSorterConfig config) {
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
        testPto(DEFAULT_NUM_SORTED, false);
    }

    @Test
    public void testParallelDefaultNum() {
        testPto(DEFAULT_NUM_SORTED, true);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM_SORTED, false);
    }

    @Test
    public void testParallelLargeNum() {
        testPto(LARGE_NUM_SORTED, true);
    }

    private void testPto(int numSorted, boolean parallel) {
        // specified for the 1 bit case.

        // create inputs
        BitVector[] x0 = IntStream.range(0, numSorted).mapToObj(i ->
            BitVectorFactory.createRandom(DEFAULT_NUM, SECURE_RANDOM)).toArray(BitVector[]::new);
        BitVector[] x1 = IntStream.range(0, numSorted).mapToObj(i ->
            BitVectorFactory.createRandom(DEFAULT_NUM, SECURE_RANDOM)).toArray(BitVector[]::new);
        SquareZ2Vector[][] x0Share = Arrays.stream(x0).map(x -> new SquareZ2Vector[]{SquareZ2Vector.create(x, false)}).toArray(SquareZ2Vector[][]::new);
        SquareZ2Vector[][] x1Share = Arrays.stream(x1).map(x -> new SquareZ2Vector[]{SquareZ2Vector.create(x, false)}).toArray(SquareZ2Vector[][]::new);

        // init the protocol
        PermutableSorterParty sender = PermutableSorterFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PermutableSorterParty receiver = PermutableSorterFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PermutableSorterSenderThread senderThread = new PermutableSorterSenderThread(sender, x0Share, config.getZl().getL());
            PermutableSorterReceiverThread receiverThread = new PermutableSorterReceiverThread(receiver, x1Share, config.getZl().getL());
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
            SquareZlVector[] shareZ0 = senderThread.getZ0();
            SquareZlVector[] shareZ1 = receiverThread.getZ1();
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

    private void assertOutput(BitVector[] x0, BitVector[] x1, SquareZlVector[] z0, SquareZlVector[] z1) {
        int num = x0[0].bitNum();
        int numSort = x0.length;
        Assert.assertEquals(num, z0[0].getNum());
        Assert.assertEquals(num, z1[0].getNum());

        BitVector[] x = IntStream.range(0, numSort).mapToObj(i -> x0[i].xor(x1[i])).toArray(BitVector[]::new);
        BigInteger[][] resultOrder = IntStream.range(0, numSort).mapToObj(i -> {
            BigInteger[] elements0 = z0[i].getZlVector().getElements();
            BigInteger[] elements1 = z1[i].getZlVector().getElements();
            return IntStream.range(0, num).mapToObj(j -> config.getZl().add(elements0[j],(elements1[j]))).toArray(BigInteger[]::new);
        }).toArray(BigInteger[][]::new);

        for (int i = 0; i < num; i++) {
            // obtain ture order
            int finalI = i;
            BigInteger[] tureValue = IntStream.range(0, numSort).mapToObj(j -> x[j].get(finalI) ? BigInteger.ONE : BigInteger.ZERO).toArray(BigInteger[]::new);
            Tuple[] tuples = IntStream.range(0, numSort).mapToObj(j -> new Tuple(tureValue[j], BigInteger.valueOf(j))).toArray(Tuple[]::new);
            Arrays.sort(tuples);
            BigInteger[] tureOrder = IntStream.range(0, numSort).mapToObj(j -> tuples[j].getValue()).toArray(BigInteger[]::new);
            BigInteger[] reverseTureOrder = new BigInteger[numSort];
            for (int j = 0; j < numSort; j ++) {
                reverseTureOrder[tureOrder[j].intValue()] = BigInteger.valueOf(j).add(BigInteger.ONE);
            }
            // verify
            for (int j = 0; j < numSort; j++) {
                Assert.assertEquals(resultOrder[j][i], reverseTureOrder[j]);
            }
        }
    }

    private static class Tuple implements Comparable<Tuple> {
        private final BigInteger key;
        private final BigInteger value;

        public Tuple(BigInteger key, BigInteger value) {
            this.key = key;
            this.value = value;
        }

        public BigInteger getKey() {
            return key;
        }

        public BigInteger getValue() {
            return value;
        }

        @Override
        public int compareTo(Tuple o) {
            return key.subtract(o.getKey()).signum();
        }
    }
}
