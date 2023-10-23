package edu.alibaba.mpc4j.s2pc.opf.permutation;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory.PermutationTypes;
import edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23.Xxx23PermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23b.Xxx23bPermutationConfig;
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

import static edu.alibaba.mpc4j.common.tool.CommonConstants.BLOCK_BIT_LENGTH;

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
    private static final int DEFAULT_NUM = 100;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 12;
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Long.SIZE);
    /**
     * default Zl
     */
    private static final Zl LARGE_ZL = ZlFactory.createInstance(EnvType.STANDARD, BLOCK_BIT_LENGTH);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Xxx23 default zl
        configurations.add(new Object[]{
            PermutationTypes.XXX23.name(), new Xxx23PermutationConfig.Builder(DEFAULT_ZL).build()
        });

        // Xxx23 large zl
        configurations.add(new Object[]{
            PermutationTypes.XXX23B.name(), new Xxx23PermutationConfig.Builder(LARGE_ZL).build()
        });

        // Xxx23b default zl
        configurations.add(new Object[]{
            PermutationTypes.XXX23B.name(), new Xxx23bPermutationConfig.Builder(DEFAULT_ZL).build()
        });

        // Xxx23b large zl
        configurations.add(new Object[]{
            PermutationTypes.XXX23B.name(), new Xxx23bPermutationConfig.Builder(LARGE_ZL).build()
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
        SquareZlVector perm0 = SquareZlVector.create(config.getZl(), IntStream.range(0, num)
            .mapToObj(i -> new BigInteger(zl.getL(), SECURE_RANDOM)).toArray(BigInteger[]::new), false);
        SquareZlVector perm1 = SquareZlVector.create(config.getZl(), IntStream.range(0, num)
            .mapToObj(i -> zl.sub(BigInteger.valueOf(randomPerm[i]), perm0.getZlVector().getElement(i))).toArray(BigInteger[]::new), false);

        ZlVector x = ZlVector.create(config.getZl(), IntStream.range(0, num)
            .mapToObj(i -> new BigInteger(config.getZl().getL(), SECURE_RANDOM)).toArray(BigInteger[]::new));

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
            assertOutput(perm0.getZlVector(), perm1.getZlVector(), x, shareZ0.getZlVector(), shareZ1.getZlVector(), config.isReverse());
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(ZlVector perm0, ZlVector perm1, ZlVector x, ZlVector x0, ZlVector x1, boolean isReverse) {
        int num = perm0.getNum();
        Assert.assertEquals(num, perm1.getNum());
        Assert.assertEquals(num, x.getNum());
        Assert.assertEquals(num, x0.getNum());
        Assert.assertEquals(num, x1.getNum());
        int[] perm = Arrays.stream(perm0.add(perm1).getElements())
            .mapToInt(BigInteger::intValue).toArray();
        if (isReverse) {
            perm = reversePermutation(perm);
        }
        ZlVector result = applyPermutation(x, perm);
        ZlVector ture = x0.add(x1);
        for (int i = 0; i < num; i++) {
            Assert.assertEquals(result.getElement(i), ture.getElement(i));
        }
    }

    /**
     * Apply permutation to inputs.
     *
     * @param x    inputs.
     * @param perm permutation.
     * @return permuted inputs.
     */
    private ZlVector applyPermutation(ZlVector x, int[] perm) {
        int num = perm.length;
        BigInteger[] xBigInt = x.getElements();
        BigInteger[] result = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            result[i] = xBigInt[perm[i]];
        }
        return ZlVector.create(x.getZl(), result);
    }

    /**
     * Reverse the permutation.
     *
     * @param perm permutation.
     * @return reversed permutation.
     */
    protected int[] reversePermutation(int[] perm) {
        int[] result = new int[perm.length];
        for (int i = 0; i < perm.length; i++) {
            result[perm[i]] = i;
        }
        return result;
    }
}
