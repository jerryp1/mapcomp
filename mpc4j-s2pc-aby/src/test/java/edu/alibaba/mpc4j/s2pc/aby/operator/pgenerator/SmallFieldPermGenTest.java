package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenFactory.PermGenTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.smallfield.ahi22.Ahi22SmallFieldPermGenConfig;
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
 * Permutable sorter test.
 *
 * @author Li Peng
 * @date 2023/10/12
 */
@RunWith(Parameterized.class)
public class SmallFieldPermGenTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmallFieldPermGenTest.class);
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
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Integer.SIZE);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // AHI+22 default zl
        Bit2aConfig bit2aConfig = new Kvh21Bit2aConfig.Builder(DEFAULT_ZL, true).build();
        configurations.add(new Object[]{
            PermGenTypes.AHI22_SMALL_FIELD.name(), new Ahi22SmallFieldPermGenConfig.Builder(bit2aConfig, true).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final PermGenConfig config;

    public SmallFieldPermGenTest(String name, PermGenConfig config) {
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
        // specified for the l <= 3 bit case.
        // create inputs
        for (int bitNum = 1; bitNum <= 3; bitNum++) {
            SquareZ2Vector[] x0Share = IntStream.range(0, bitNum).mapToObj(i ->
                    SquareZ2Vector.create(BitVectorFactory.createRandom(num, SECURE_RANDOM), false))
                .toArray(SquareZ2Vector[]::new);
            SquareZ2Vector[] x1Share = IntStream.range(0, bitNum).mapToObj(i ->
                    SquareZ2Vector.create(BitVectorFactory.createRandom(num, SECURE_RANDOM), false))
                .toArray(SquareZ2Vector[]::new);
            // init the protocol
            PermGenParty sender = PermGenFactory.createSender(firstRpc, secondRpc.ownParty(), config);
            PermGenParty receiver = PermGenFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
            sender.setParallel(parallel);
            receiver.setParallel(parallel);
            try {
                LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
                PermGenSenderThread senderThread = new PermGenSenderThread(sender, x0Share, bitNum);
                PermGenReceiverThread receiverThread = new PermGenReceiverThread(receiver, x1Share, bitNum);
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
                LOGGER.info("-----verifying start for bitNum:{}-----", bitNum);
                SquareZlVector shareZ0 = senderThread.getZ0();
                SquareZlVector shareZ1 = receiverThread.getZ1();
                BitVector[] x0 = Arrays.stream(x0Share).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new);
                BitVector[] x1 = Arrays.stream(x1Share).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new);
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
    }

    private void assertOutput(BitVector[] x0, BitVector[] x1, SquareZlVector z0, SquareZlVector z1) {
        int num = x0[0].bitNum();
        Assert.assertEquals(num, z0.getNum());
        Assert.assertEquals(num, z1.getNum());

        byte[][] x = IntStream.range(0, x0.length).mapToObj(i -> x0[i].xor(x1[i]).getBytes()).toArray(byte[][]::new);
        BigInteger[] elements0 = z0.getZlVector().getElements();
        BigInteger[] elements1 = z1.getZlVector().getElements();
        BigInteger[] resultOrder = IntStream.range(0, num).mapToObj(i -> config.getZl().add(elements0[i], (elements1[i]))).toArray(BigInteger[]::new);

        // obtain ture order
        BitVector[] transRes = ZlDatabase.create(num, x).bitPartition(EnvType.STANDARD, true);
        BigInteger[] tureValue = IntStream.range(0, num).mapToObj(j -> transRes[j].getBigInteger()).toArray(BigInteger[]::new);
        Tuple[] tuples = IntStream.range(0, num).mapToObj(j -> new Tuple(tureValue[j], BigInteger.valueOf(j))).toArray(Tuple[]::new);
        Arrays.sort(tuples);
        BigInteger[] tureOrder = IntStream.range(0, num).mapToObj(j -> tuples[j].getValue()).toArray(BigInteger[]::new);
        BigInteger[] reverseTureOrder = new BigInteger[num];
        for (int j = 0; j < num; j++) {
            reverseTureOrder[tureOrder[j].intValue()] = BigInteger.valueOf(j);
        }
        // verify
        for (int j = 0; j < num; j++) {
            Assert.assertEquals(resultOrder[j], reverseTureOrder[j]);
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
