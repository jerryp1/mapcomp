package edu.alibaba.mpc4j.s2pc.opf.spermutation;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory.PermutationTypes;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleUtils;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.xxx23.Xxx23SharedPermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.xxx23b.Xxx23bSharedPermutationConfig;
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
 * Shared permutation test.
 *
 * @author Li Peng
 * @date 2023/10/25
 */
@RunWith(Parameterized.class)
public class SharedPermutationTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedPermutationTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 100;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 15;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Xxx23 no silent
        configurations.add(new Object[]{
            PermutationTypes.XXX23.name(), new Xxx23SharedPermutationConfig.Builder(false).build()
        });

        // Xxx23 silent
        configurations.add(new Object[]{
            PermutationTypes.XXX23.name() + "_silent", new Xxx23SharedPermutationConfig.Builder(true).build()
        });

        // Xxx23b no silent
        configurations.add(new Object[]{
            PermutationTypes.XXX23B.name(), new Xxx23bSharedPermutationConfig.Builder(false).build()
        });

        // Xxx23b silent
        configurations.add(new Object[]{
            PermutationTypes.XXX23B.name() + "_silent", new Xxx23bSharedPermutationConfig.Builder(true).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final SharedPermutationConfig config;

    public SharedPermutationTest(String name, SharedPermutationConfig config) {
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
        Zl zl = ZlFactory.createInstance(EnvType.STANDARD, Math.max(secureRandom.nextInt(BLOCK_BIT_LENGTH), 32));

        // generate random permutation
        int[] perms = ShuffleUtils.generateRandomPerm(num);
        Vector<byte[]> permsShare0 = IntStream.range(0, num).mapToObj(j -> zl.createRandom(SECURE_RANDOM))
            .map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, zl.getByteL())).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> permsShare1 = IntStream.range(0, num).mapToObj(j ->
            BytesUtils.xor(permsShare0.get(j), BigIntegerUtils.nonNegBigIntegerToByteArray(BigInteger.valueOf(perms[j]), zl.getByteL())))
            .collect(Collectors.toCollection(Vector::new));
        // generate random inputs
        int[] x = ShuffleUtils.generateRandomPerm(num);
        Vector<byte[]> xShare0 = IntStream.range(0, num).mapToObj(j -> zl.createRandom(SECURE_RANDOM))
            .map(v -> BigIntegerUtils.nonNegBigIntegerToByteArray(v, zl.getByteL())).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> xShare1 = IntStream.range(0, num).mapToObj(j ->
            BytesUtils.xor(xShare0.get(j), BigIntegerUtils.nonNegBigIntegerToByteArray(BigInteger.valueOf(x[j]), zl.getByteL())))
            .collect(Collectors.toCollection(Vector::new));

        // init the protocol
        SharedPermutationParty sender = SharedPermutationFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        SharedPermutationParty receiver = SharedPermutationFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            SharedPermutationSenderThread senderThread = new SharedPermutationSenderThread(sender, permsShare0, xShare0);
            SharedPermutationReceiverThread receiverThread = new SharedPermutationReceiverThread(receiver, permsShare1, xShare1);
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
            Vector<byte[]> shareZ0 = senderThread.getZ0();
            Vector<byte[]> shareZ1 = receiverThread.getZ1();
            assertOutput(perms, x, shareZ0, shareZ1, config.isReverse());
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(int[] perms, int[] xs, Vector<byte[]> z0, Vector<byte[]> z1, boolean isReverse) {
        int num = perms.length;
        Assert.assertEquals(num, xs.length);
        Assert.assertEquals(num, z0.size());
        Assert.assertEquals(num, z1.size());
        if (isReverse) {
            perms = ShuffleUtils.reversePermutation(perms);
        }
        Vector<Integer> xsVector = Arrays.stream(xs).boxed().collect(Collectors.toCollection(Vector::new));
        Vector<Integer> permutedXs = BenesNetworkUtils.permutation(perms, xsVector);
        Vector<Integer> result = IntStream.range(0, num).mapToObj(i -> BytesUtils.xor(z0.get(i), z1.get(i)))
            .mapToInt(v -> BigIntegerUtils.byteArrayToNonNegBigInteger(v).intValue()).boxed().collect(Collectors.toCollection(Vector::new));
        for (int i = 0; i < num; i++) {
            Assert.assertEquals(result.get(i), permutedXs.get(i));
        }
    }
}
