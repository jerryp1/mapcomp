package edu.alibaba.mpc4j.s2pc.opf.groupagg;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.bitmap.BitmapGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.bitmap.BitmapGroupAggSender;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.bsorting.BitmapSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggConfig;
//import edu.alibaba.mpc4j.s2pc.opf.groupagg.mix.MixGroupAggSender;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.omix.OptimizedMixGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.osorting.OptimizedSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting.SortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.tsorting.TrivialSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;
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

import static edu.alibaba.mpc4j.s2pc.opf.groupagg.CommonConstants.*;

/**
 * Group aggregation Test.
 *
 * @author Li Peng
 * @date 2023/11/9
 */
@RunWith(Parameterized.class)
public class GroupAggTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupAggTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 6;
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, 64);

    private static final int DEFAULT_GROUP_BIT_LENGTH = 2;

    private static final int LARGE_GROUP_BIT_LENGTH = 8;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

//        // bitmap && sum
//        configurations.add(new Object[]{
//            "BITMAP_"+PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new BitmapGroupAggConfig.Builder(DEFAULT_ZL, true, PrefixAggTypes.SUM).build()
//        });
//
//        // bitmap && max
//        configurations.add(new Object[]{
//            "BITMAP_"+PrefixAggTypes.MAX.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new BitmapGroupAggConfig.Builder(DEFAULT_ZL, true, PrefixAggTypes.MAX).build()
//        });

        // Bitmap-assisted sort && sum
        configurations.add(new Object[]{
            "Bitmap-assisted SORT_"+PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new BitmapSortingGroupAggConfig.Builder(DEFAULT_ZL, true, PrefixAggTypes.SUM).build()
        });

        // Bitmap-assisted sort && max
        configurations.add(new Object[]{
            "Bitmap-assisted SORT_"+PrefixAggTypes.MAX.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new BitmapSortingGroupAggConfig.Builder(DEFAULT_ZL, true, PrefixAggTypes.MAX).build()
        });

//        // Optimized Mix sort && sum
//        configurations.add(new Object[]{
//            "Optimized Mix SORT_"+PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new OptimizedMixGroupAggConfig.Builder(DEFAULT_ZL, true, PrefixAggTypes.SUM).build()
//        });
//
//        // Optimized Mix sort && max
//        configurations.add(new Object[]{
//            "Optimized Mix SORT_"+PrefixAggTypes.MAX.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new OptimizedMixGroupAggConfig.Builder(DEFAULT_ZL, true, PrefixAggTypes.MAX).build()
//        });

        // Optimized sort && sum
        configurations.add(new Object[]{
            "Optimized SORT_"+PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new OptimizedSortingGroupAggConfig.Builder(DEFAULT_ZL, true, PrefixAggTypes.SUM).build()
        });

        // Optimized sort && max
        configurations.add(new Object[]{
            "Optimized SORT_"+PrefixAggTypes.MAX.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new OptimizedSortingGroupAggConfig.Builder(DEFAULT_ZL, true, PrefixAggTypes.MAX).build()
        });

        // sort && sum
        configurations.add(new Object[]{
            "SORT_"+PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new SortingGroupAggConfig.Builder(DEFAULT_ZL, true, PrefixAggTypes.SUM).build()
        });

        // sort && max
        configurations.add(new Object[]{
            "SORT_"+PrefixAggTypes.MAX.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new SortingGroupAggConfig.Builder(DEFAULT_ZL, true, PrefixAggTypes.MAX).build()
        });

        // Trivial sort && sum
        configurations.add(new Object[]{
            "Trivial SORT_"+PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new TrivialSortingGroupAggConfig.Builder(DEFAULT_ZL, true, PrefixAggTypes.SUM).build()
        });

        // Trivial sort && max
        configurations.add(new Object[]{
            "Trivial SORT_"+PrefixAggTypes.MAX.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new TrivialSortingGroupAggConfig.Builder(DEFAULT_ZL, true, PrefixAggTypes.MAX).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final GroupAggConfig config;
    /**
     * the zl.
     */
    private final Zl zl;
    /**
     * prefix aggregation type.
     */
    private final PrefixAggTypes type;

    public GroupAggTest(String name, GroupAggConfig config) {
        super(name);
        this.config = config;
        this.zl = config.getZl();
        this.type = config.getAggType();
    }

//    @Test
//    public void test2Num() {
//        testPto(2, false);
//    }
//
//    @Test
//    public void test8Num() {
//        testPto(8, false);
//    }
//
//    @Test
//    public void test7Num() {
//        testPto(7, false);
//    }
//
//    @Test
//    public void test9Num() {
//        testPto(9, false);
//    }
//
//    @Test
//    public void test19Num() {
//        testPto(19, false);
//    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM, false);
    }

//    @Test
//    public void testParallelDefaultNum() {
//        testPto(DEFAULT_NUM, true);
//    }
//
//    @Test
//    public void testLargeNum() {
//        testPto(LARGE_NUM, false);
//    }
//
//    @Test
//    public void testParallelLargeNum() {
//        testPto(LARGE_NUM, true);
//    }

    private void testPto(int num, boolean parallel) {
        testPto(num, DEFAULT_GROUP_BIT_LENGTH, parallel);
//        testPto(num, LARGE_GROUP_BIT_LENGTH, parallel);
    }

    private void testPto(int num, int groupBitLength, boolean parallel) {
        // input
        String[] senderGroup = genRandomInputGroup(groupBitLength, num);
        String[] receiverGroup = genRandomInputGroup(groupBitLength, num);
        long[] receiverAgg = IntStream.range(0, num).mapToLong(i -> i).toArray();

//        long[] receiverAgg = IntStream.range(0, num).mapToLong(i -> SECURE_RANDOM.nextInt(32) + 1).toArray();

        SquareZ2Vector e0 = SquareZ2Vector.create(BitVectorFactory.createRandom(num, SECURE_RANDOM), false);
        SquareZ2Vector e1 = SquareZ2Vector.create(BitVectorFactory.createRandom(num, SECURE_RANDOM), false);
        BitVector e = e0.getBitVector().xor(e1.getBitVector());

//        BitVector e = BitVectorFactory.createOnes(num);
//        SquareZ2Vector e0 = SquareZ2Vector.create(BitVectorFactory.createRandom(num, SECURE_RANDOM), false);
//        SquareZ2Vector e1 = SquareZ2Vector.create(e.xor(e0.getBitVector()), false);

        Properties properties = genProperties(num, groupBitLength, groupBitLength);

        // init the protocol
        GroupAggParty sender = GroupAggFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        GroupAggParty receiver = GroupAggFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);

        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            GroupAggPartyThread senderThread = new GroupAggPartyThread(sender, senderGroup, null, e0, properties);
            GroupAggPartyThread receiverThread = new GroupAggPartyThread(receiver, receiverGroup, receiverAgg, e1, properties);
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
            GroupAggOut output = receiverThread.getOutput();
            assertOutput(senderGroup, receiverGroup, receiverAgg, e, output);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(String[] trueGroupSender, String[] trueGroupReceiver, long[] trueAgg, BitVector e, GroupAggOut groupAggOut) {
        int num = trueGroupSender.length;
        String[] trueTotalGroup = IntStream.range(0, num).mapToObj(i -> trueGroupSender[i].concat(trueGroupReceiver[i])).toArray(String[]::new);
        IntStream.range(0, num).forEach(i -> trueAgg[i] = e.get(i) ? trueAgg[i] : 0);
        // true
        Map<String, BigInteger> trueMap = getAggResultMap(Arrays.asList(trueTotalGroup),
            Arrays.stream(trueAgg).mapToObj(BigInteger::valueOf).collect(Collectors.toList()));
        // result
        Map<String, BigInteger> resultMap = getAggResultMap(Arrays.asList(groupAggOut.getGroupField()),
            Arrays.asList(groupAggOut.getAggregationResult()));
        // verify
        Assert.assertEquals(trueMap, resultMap);
        System.out.println("##mix_矩阵转置总时间：" + TransposeUtils.TRANSPORT_TIME+"ms");
//        System.out.println("##mix_agg总时间：" + MixGroupAggSender.AGG_TIME+"ms");
//        System.out.println("##mix_osn总时间：" + MixGroupAggSender.OSN_TIME+"ms");
//        System.out.println("##mix_MUX总时间：" + MixGroupAggSender.MUX_TIME+"ms");
        System.out.println("##bitmap_agg总时间：" + BitmapGroupAggSender.AGG_TIME+"ms");

    }

    private Map<String, BigInteger> getAggResultMap(List<String> group, List<BigInteger> agg) {
        int num = agg.size();
        Map<String, BigInteger> map = new HashMap<>();
        for (int i = 0; i < num; i++) {
            String key = group.get(i);
            if (map.containsKey(key)) {
                if (type.equals(PrefixAggTypes.SUM)) {
                    map.put(key, map.get(key).add(agg.get(i)));
                } else {
                    map.put(key, map.get(key).compareTo(agg.get(i)) < 0 ? agg.get(i) : map.get(key));
                }

            } else {
                map.put(key, agg.get(i));
            }
        }
        // remove dummy entries.
        List<String> toBeRemoved = new ArrayList<>();
        for (String key : map.keySet()) {
            if (map.get(key).equals(BigInteger.ZERO)) {
                toBeRemoved.add(key);
            }
        }
        for (String s : toBeRemoved) {
            map.remove(s);
        }
        return map;
    }

    private String[] genRandomInputGroup(int groupBitLength, int num) {
        String[] groups = GroupAggUtils.genStringSetFromRange(groupBitLength);
        return IntStream.range(0, num).mapToObj(i -> groups[SECURE_RANDOM.nextInt((1 << groupBitLength))]).toArray(String[]::new);
    }

    private Properties genProperties(int n, int senderGroupBitLength, int receiverGroupBitLength) {
        Properties properties = new Properties();
        properties.setProperty(SENDER_GROUP_BIT_LENGTH, String.valueOf(senderGroupBitLength));
        properties.setProperty(RECEIVER_GROUP_BIT_LENGTH, String.valueOf(receiverGroupBitLength));
        properties.setProperty(MAX_L, String.valueOf(zl.getL()));
        properties.setProperty(MAX_NUM, String.valueOf(n));
        return properties;
    }
}
