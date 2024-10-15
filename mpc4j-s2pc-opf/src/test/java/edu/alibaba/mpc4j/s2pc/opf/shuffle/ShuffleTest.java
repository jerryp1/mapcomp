package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory.ShuffleTypes;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.php24.Php24ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.php24b.Php24bShuffleConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.CommonConstants.BLOCK_BIT_LENGTH;

/**
 * Shuffle test.
 *
 */
@RunWith(Parameterized.class)
public class ShuffleTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShuffleTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 100;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 14;
    /**
     * default number of input vectors
     */
    private static final int DEFAULT_LENGTH = 10;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Php+24 large zl
        configurations.add(new Object[]{
            ShuffleTypes.PHP24.name(), new Php24ShuffleConfig.Builder(false).build()
        });

        // Php+24 large zl
        configurations.add(new Object[]{
            ShuffleTypes.PHP24.name() + "_silent", new Php24ShuffleConfig.Builder(true).build()
        });

        // Php+24b large zl
        configurations.add(new Object[]{
            ShuffleTypes.PHP24b.name(), new Php24bShuffleConfig.Builder(false).build()
        });

        // Php+24b large zl
        configurations.add(new Object[]{
            ShuffleTypes.PHP24b.name() + "_silent", new Php24bShuffleConfig.Builder(true).build()
        });
        return configurations;
    }

    /**
     * the config
     */
    private final ShuffleConfig config;

    public ShuffleTest(String name, ShuffleConfig config) {
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
        SecureRandom secureRandom = new SecureRandom();
        Zl[] zl = new Zl[DEFAULT_LENGTH];
        // generate input
        List<int[]> inputs = new ArrayList<>();
        List<Vector<byte[]>> shareX0 = new ArrayList<>();
        List<Vector<byte[]>> shareX1 = new ArrayList<>();
        int[] randomPerms0 = ShuffleUtils.generateRandomPerm(num);
        int[] randomPerms1 = ShuffleUtils.generateRandomPerm(num);
        for (int i = 0; i < DEFAULT_LENGTH; i++) {
            zl[i] = ZlFactory.createInstance(EnvType.STANDARD, Math.max(secureRandom.nextInt(BLOCK_BIT_LENGTH), 32));
            Zl currentZl = zl[i];
            int[] input = ShuffleUtils.generateRandomPerm(num);
            Vector<byte[]> share0 = IntStream.range(0, num).mapToObj(j -> currentZl.createRandom(SECURE_RANDOM))
                .map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, currentZl.getByteL())).collect(Collectors.toCollection(Vector::new));
            Vector<byte[]> share1 = IntStream.range(0, num).mapToObj(j ->
                BytesUtils.xor(share0.get(j), BigIntegerUtils.nonNegBigIntegerToByteArray(BigInteger.valueOf(input[j]), currentZl.getByteL())))
                .collect(Collectors.toCollection(Vector::new));
            inputs.add(input);
            shareX0.add(share0);
            shareX1.add(share1);
        }

        // init the protocol
        ShuffleParty sender = ShuffleFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        ShuffleParty receiver = ShuffleFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            ShuffleSenderThread senderThread = new ShuffleSenderThread(sender, shareX0, randomPerms0);
            ShuffleReceiverThread receiverThread = new ShuffleReceiverThread(receiver, shareX1, randomPerms1);
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
            List<Vector<byte[]>> shareZ0 = senderThread.getZ0();
            List<Vector<byte[]>> shareZ1 = receiverThread.getZ1();
            assertOutput(randomPerms0, randomPerms1, inputs, shareZ0, shareZ1, config.isReverse());
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(int[] perms0, int[] perms1, List<int[]> xs, List<Vector<byte[]>> z0,
                              List<Vector<byte[]>> z1, boolean isReverse) {
        int length = xs.size();
        // true composition
        int[] perms = ShuffleUtils.composePerms(perms1, perms0);
        // whether current pto is doing un-shuffling
        if (isReverse) {
            perms = ShuffleUtils.reversePermutation(perms);
        }
        for (int i = 0; i < length; i++) {
            Vector<byte[]> z0i = z0.get(i);
            Vector<byte[]> z1i = z1.get(i);
            int num = z0i.size();
            int[] z = IntStream.range(0, num).mapToObj(j -> BytesUtils.xor(z0i.get(j), z1i.get(j)))
                .mapToInt(v -> BigIntegerUtils.byteArrayToNonNegBigInteger(v).intValue()).toArray();
            int[] x = BenesNetworkUtils.permutation(perms, Arrays.stream(xs.get(i))
                .boxed().collect(Collectors.toCollection(Vector::new)))
                .stream().mapToInt(j -> j).toArray();
            IntStream.range(0, num).forEach(j -> Assert.assertEquals(z[j], x[j]));
        }
    }
}
