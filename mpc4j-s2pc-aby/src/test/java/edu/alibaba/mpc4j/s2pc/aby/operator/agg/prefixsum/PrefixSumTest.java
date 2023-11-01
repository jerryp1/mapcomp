package edu.alibaba.mpc4j.s2pc.aby.operator.agg.prefixsum;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.BinaryGf2eDokvs;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20.Rrk20ZlMaxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.prefixsum.PrefixSumFactory.PrefixSumTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.prefixsum.xxx23.Xxx23PrefixSumConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.checkerframework.checker.units.qual.Prefix;
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
 * Prefix sum Test.
 *
 * @author Li Peng
 * @date 2023/10/30
 */
@RunWith(Parameterized.class)
public class PrefixSumTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrefixSumTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 100;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 14;
    /**
     * small Zl
     */
    private static final Zl SMALL_ZL = ZlFactory.createInstance(EnvType.STANDARD, 3);
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Integer.SIZE);
    /**
     * current Zl
     */
    private final Zl zl;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RRK+20, default zl
        configurations.add(new Object[]{
                PrefixSumTypes.Xxx23.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new Xxx23PrefixSumConfig.Builder(DEFAULT_ZL, true).build()
        });
        // RRK+20, small zl
//        configurations.add(new Object[]{
//                PrefixSumTypes.Xxx23.RRK20.name() + " (l = " + SMALL_ZL.getL() + ")",
//            new Xxx23PrefixSumConfig.Builder(SMALL_ZL).build()
//        });

        return configurations;
    }

    /**
     * the config
     */
    private final PrefixSumConfig config;

    public PrefixSumTest(String name, PrefixSumConfig config) {
        super(name);
        this.config = config;
        this.zl = config.getZl();
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
        int groupNum = 10;
        int groupSize = num / groupNum;
        // generate inputs
        byte[][] groupings = new byte[num][];
        BigInteger[] aggs = new BigInteger[num];
        for (int i = 0; i < num ;i++) {
            groupings[i] = BigIntegerUtils.nonNegBigIntegerToByteArray(BigInteger.valueOf(i / groupSize), zl.getByteL());
            aggs[i] = BigInteger.valueOf(i);
        }
        // generate shares
        Vector<byte[]> groupShares0 = IntStream.range(0, num).mapToObj(i -> {
            byte[] shares = new byte[zl.getByteL()];
            SECURE_RANDOM.nextBytes(shares);
            return shares;
        }).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> groupShares1 = IntStream.range(0, num).mapToObj(i ->
            BytesUtils.xor(groupings[i], groupShares0.elementAt(i))).collect(Collectors.toCollection(Vector::new));

        SquareZlVector aggShares0 = SquareZlVector.create(zl, IntStream.range(0,num).mapToObj(i ->
            new BigInteger(zl.getL(), SECURE_RANDOM)).toArray(BigInteger[]::new), false);
        SquareZlVector aggShares1 = SquareZlVector.create(zl, IntStream.range(0,num).mapToObj(i ->
            zl.sub(aggs[i], aggShares0.getZlVector().getElement(i))).toArray(BigInteger[]::new), false);

        // init the protocol
        PrefixSumParty sender = PrefixSumFactory.createPrefixSumSender(firstRpc, secondRpc.ownParty(), config);
        PrefixSumParty receiver = PrefixSumFactory.createPrefixSumReceiver(secondRpc, firstRpc.ownParty(), config);

        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PrefixSumPartyThread senderThread = new PrefixSumPartyThread(sender, groupShares0, aggShares0);
            PrefixSumPartyThread receiverThread = new PrefixSumPartyThread(receiver, groupShares1, aggShares1);
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
            SquareZlVector shareZ0 = senderThread.getShareZ();
            SquareZlVector shareZ1 = receiverThread.getShareZ();
            assertOutput(groupings, aggs,  shareZ0, shareZ1);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(byte[][] groupings, BigInteger[] aggs, SquareZlVector shareZ0, SquareZlVector shareZ1) {
        int num = aggs.length;
        BigInteger[] groupingsBigInt = IntStream.range(0, num).mapToObj(i -> BigIntegerUtils.byteArrayToNonNegBigInteger(groupings[i])).toArray(BigInteger[]::new);

        BigInteger[] result = shareZ0.getZlVector().add(shareZ1.getZlVector()).getElements();
        System.out.println(123);
//        List<BigInteger> xElements = Arrays.asList(x0.add(x1).getElements());
//        BigInteger z = shareZ0.getZlVector().add(shareZ1.getZlVector()).getElement(0);
//        Collections.sort(xElements);
//        Assert.assertEquals(z, xElements.get(xElements.size() - 1));
    }
}
