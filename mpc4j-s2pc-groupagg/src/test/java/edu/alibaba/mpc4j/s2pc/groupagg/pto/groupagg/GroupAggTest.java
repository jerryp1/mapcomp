package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.crypto.matrix.TransposeUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bitmap.BitmapGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bsorting.BitmapSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.mix.MixGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.omix.OptimizedMixGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.oneside.OneSideGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.osorting.OptimizedSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.sorting.SortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.tsorting.TrivialSortingGroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;
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

import static edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.CommonConstants.*;
import static edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.hardcode.HardcodeZ2MtgSender.TRIPLE_NUM;

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
    private static final int DEFAULT_NUM = 1 << 4;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Long.SIZE);
    /**
     * Sender group bit length
     */
    private static final int SENDER_GROUP_BIT_LEN = 6;
    /**
     * Receiver group bit length
     */
    private static final int RECEIVER_GROUP_BIT_LEN = 2;

    private static final boolean parallel = true;

    private static final boolean silent = false;

    private static final boolean senderAgg = false;

    private static final boolean havingState = false;

    private static final boolean dummyPayload = false;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

//        // bitmap && sum
//        configurations.add(new Object[]{
//            "BITMAP_" + PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new BitmapGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.SUM).build()
//        });
//
//        // bitmap && max
//        configurations.add(new Object[]{
//            "BITMAP_" + PrefixAggTypes.MAX.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new BitmapGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.MAX).build()
//        });
//
//        // mix && sum
//        configurations.add(new Object[]{
//            "MIX_" + PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new MixGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.SUM).build()
//        });
//
//        // mix && max
//        configurations.add(new Object[]{
//            "MIX_" + PrefixAggTypes.MAX.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new MixGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.MAX).build()
//        });
//
//        // o_mix && sum
//        configurations.add(new Object[]{
//            "O_MIX_" + PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new OptimizedMixGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.SUM).build()
//        });
//
//        // o_mix && max
//        configurations.add(new Object[]{
//            "O_MIX_" + PrefixAggTypes.MAX.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new OptimizedMixGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.MAX).build()
//        });
//
//        // sort && sum
//        configurations.add(new Object[]{
//            "SORT_" + PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new SortingGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.SUM).build()
//        });
//
//        // sort && max
//        configurations.add(new Object[]{
//            "SORT_" + PrefixAggTypes.MAX.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new SortingGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.MAX).build()
//        });
//
//        //  o_sort && sum
//        configurations.add(new Object[]{
//            "O_SORT_" + PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new OptimizedSortingGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.SUM).build()
//        });
//
//        // o_sort && max
//        configurations.add(new Object[]{
//            "O-SORT_" + PrefixAggTypes.MAX.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new OptimizedSortingGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.MAX).build()
//        });
//
        // b_sort && sum
        configurations.add(new Object[]{
            "B_SORT_" + PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new BitmapSortingGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.SUM).build()
        });

        // b_sort && max
        configurations.add(new Object[]{
            "B-SORT_" + PrefixAggTypes.MAX.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new BitmapSortingGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.MAX).build()
        });
//
//        // t_sort && sum
//        configurations.add(new Object[]{
//            "T_SORT_" + PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new TrivialSortingGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.SUM).build()
//        });
//
//        // t_sort && max
//        configurations.add(new Object[]{
//            "T-SORT_" + PrefixAggTypes.MAX.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//            new TrivialSortingGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.MAX).build()
//        });

//        // one-side && sum
//         configurations.add(new Object[]{
//            "ONE-SIDE_"+PrefixAggTypes.SUM.name() + " (l = " + DEFAULT_ZL.getL() + ")",
//             new OneSideGroupAggConfig.Builder(DEFAULT_ZL, silent, PrefixAggTypes.SUM).build()
//         });

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

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM);
    }

    private void testPto(int num) {
        // input
        String[] senderGroup = genRandomInputGroup(SENDER_GROUP_BIT_LEN, num);
        String[] receiverGroup = genRandomInputGroup(RECEIVER_GROUP_BIT_LEN, num);
        long[] agg = IntStream.range(0, num).mapToLong(i -> i).toArray();
        long[] sAgg = senderAgg ? agg : null;
        long[] rAgg = senderAgg ? null : agg;

        // e
        BitVector e = BitVectorFactory.createOnes(num);
        SquareZ2Vector e0 = SquareZ2Vector.create(BitVectorFactory.createRandom(num, SECURE_RANDOM), false);
        SquareZ2Vector e1 = SquareZ2Vector.create(e.xor(e0.getBitVector()), false);

        Properties properties = genProperties(num);

        // init the protocol
        GroupAggParty sender = GroupAggFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        GroupAggParty receiver = GroupAggFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);

        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            GroupAggPartyThread senderThread = new GroupAggPartyThread(sender, senderGroup, sAgg, e0, properties);
            GroupAggPartyThread receiverThread = new GroupAggPartyThread(receiver, receiverGroup, rAgg, e1, properties);
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
            assertOutput(senderGroup, receiverGroup, agg, e, output);
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
        System.out.println("## total time of transpose：" + TransposeUtils.TRANSPORT_TIME + "ms");
        System.out.println("## triple num needed：" + TRIPLE_NUM);
        TRIPLE_NUM = 0;

    }

    private Map<String, BigInteger> getAggResultMap(List<String> group, List<BigInteger> agg) {
        int num = agg.size();
        Map<String, BigInteger> map = new HashMap<>();
        for (int i = 0; i < num; i++) {
            String key = group.get(i);
            if (map.containsKey(key)) {
                // sum
                if (type.equals(PrefixAggTypes.SUM)) {
                    map.put(key, map.get(key).add(agg.get(i)));
                } else {
                    // max
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
        if (groupBitLength == 0) {
            return IntStream.range(0, num).mapToObj(i -> "").toArray(String[]::new);
        }
        String[] groups = GroupAggUtils.genStringSetFromRange(groupBitLength);
        return IntStream.range(0, num).mapToObj(i ->
            groups[SECURE_RANDOM.nextInt((1 << groupBitLength))]).toArray(String[]::new);
    }

    private Properties genProperties(int n) {
        Properties properties = new Properties();
        properties.setProperty(SENDER_GROUP_BIT_LENGTH, String.valueOf(SENDER_GROUP_BIT_LEN));
        properties.setProperty(RECEIVER_GROUP_BIT_LENGTH, String.valueOf(RECEIVER_GROUP_BIT_LEN));
        properties.setProperty(SENDER_AGG, String.valueOf(senderAgg));
        properties.setProperty(HAVING_STATE, String.valueOf(havingState));
        properties.setProperty(DUMMY_PAYLOAD, String.valueOf(dummyPayload));
        properties.setProperty(MAX_L, String.valueOf(zl.getL()));
        properties.setProperty(MAX_NUM, String.valueOf(n));
        return properties;
    }
}
