package edu.alibaba.mpc4j.s2pc.opf.permutation;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory.Bit2aTypes;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory.PermutationTypes;
import edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23.Xxx23PermutationConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Permutation test.
 *
 * @author Li Peng
 * @date 2023/10/12
 */
@RunWith(Parameterized.class)
public class PermutationTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermutationTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 10;
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
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Long.SIZE);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // KVH+21 small zl
//        configurations.add(new Object[]{
//            PermutationTypes.XXX23.name(), new Xxx23PermutationConfig.Builder(SMALL_ZL).build()
//        });

        // KVH+21 default zl
        configurations.add(new Object[]{
            PermutationTypes.XXX23.name(), new Xxx23PermutationConfig.Builder(DEFAULT_ZL).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final PermutationConfig config;

    public PermutationTest(String name, PermutationConfig config) {
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
        Zl zl = config.getZl();
        // generate random permutation
        List<Integer> randomPermList = IntStream.range(0, num)
            .boxed()
            .collect(Collectors.toList());
        Collections.shuffle(randomPermList, SECURE_RANDOM);
        int[] randomPerm = randomPermList.stream().mapToInt(permutation -> permutation).toArray();

        // create inputs
        SquareZlVector perm0 = SquareZlVector.create(config.getZl(), IntStream.range(0, num).mapToObj(i -> new BigInteger(zl.getL(), SECURE_RANDOM)).toArray(BigInteger[]::new), false);
        SquareZlVector perm1 = SquareZlVector.create(config.getZl(), IntStream.range(0, num).mapToObj(i -> zl.sub(BigInteger.valueOf(randomPerm[i]), perm0.getZlVector().getElement(i))).toArray(BigInteger[]::new), false);

        ZlVector x = ZlVector.create(config.getZl(), IntStream.range(0, num).mapToObj(i -> new BigInteger(config.getZl().getL(), SECURE_RANDOM)).toArray(BigInteger[]::new));

//
//        BitVector x0 = BitVectorFactory.createRandom(num, SECURE_RANDOM);
//        BitVector x1 = BitVectorFactory.createRandom(num, SECURE_RANDOM);
//        SquareZ2Vector shareX0 = SquareZ2Vector.create(x0, false);
//        SquareZ2Vector shareX1 = SquareZ2Vector.create(x1, false);
        // init the protocol
        PermutationSender sender = PermutationFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        PermutationReceiver receiver = PermutationFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PermutationSenderThread senderThread = new PermutationSenderThread(sender, perm0, x);
            PermutationReceiverThread receiverThread = new PermutationReceiverThread(receiver, perm1);
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
            assertOutput(perm0.getZlVector(), perm1.getZlVector(), x, shareZ0.getZlVector(), shareZ1.getZlVector());
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

//    private void assertOutput(BitVector x0, BitVector x1, SquareZlVector z0, SquareZlVector z1) {
//        int num = x0.bitNum();
//        Assert.assertEquals(num, z0.getNum());
//        Assert.assertEquals(num, z1.getNum());
//        BitVector x = x0.xor(x1);
//        BigInteger[] z = z0.getZlVector().add(z1.getZlVector()).getElements();
//        for (int i = 0; i < num; i++) {
//            boolean xi = x.get(i);
//            if (xi) {
//                Assert.assertEquals(z[i], BigInteger.ONE);
//            } else {
//                Assert.assertEquals(z[i], BigInteger.ZERO);
//            }
//        }
//    }

    private void assertOutput(ZlVector perm0, ZlVector perm1, ZlVector x, ZlVector x0, ZlVector x1) {
        int num = perm0.getNum();
//        Zl zl = perm0.getZl();
        Assert.assertEquals(num, perm1.getNum());
        Assert.assertEquals(num, x.getNum());
        Assert.assertEquals(num, x0.getNum());
        Assert.assertEquals(num, x1.getNum());
        int[] perm = Arrays.stream(perm0.add(perm1).getElements())
            .mapToInt(BigInteger::intValue).toArray();

        ZlVector result = applyPermutation(x, perm);
        ZlVector ture = x0.add(x1);
        for (int i = 0; i < num; i++) {
            Assert.assertEquals(result.getElement(i), ture.getElement(i));
        }
    }

    private ZlVector applyPermutation(ZlVector x, int[] perm) {
        int num = perm.length;
        BigInteger[] xBigInt = x.getElements();
        BigInteger[] result = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            result[i] = xBigInt[perm[i]];
        }
        return ZlVector.create(x.getZl(), result);
    }
}
