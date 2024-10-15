package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixsum;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggParty;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax.PrefixMaxConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax.PrefixMaxFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax.PrefixMaxFactory.PrefixMaxTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax.amos22.Amos22PrefixMaxConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax.php24.Php24PrefixMaxConfig;
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

/**
 * Prefix max Test.
 *
 */
@RunWith(Parameterized.class)
public class PrefixMaxTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrefixMaxTest.class);
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1 << 4;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 14;
    /**
     * default Zl
     */
    private static final Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, Long.SIZE);
    /**
     * current Zl
     */
    private final Zl zl;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // php24 default zl
        configurations.add(new Object[]{
            PrefixMaxTypes.PHP24.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new Php24PrefixMaxConfig.Builder(DEFAULT_ZL, false).build()
        });

        // amos22 default zl
        configurations.add(new Object[]{
            PrefixMaxTypes.AMOS22.name() + " (l = " + DEFAULT_ZL.getL() + ")",
            new Amos22PrefixMaxConfig.Builder(DEFAULT_ZL, false).build()
        });

        return configurations;
    }

    /**
     * the config
     */
    private final PrefixMaxConfig config;

    public PrefixMaxTest(String name, PrefixMaxConfig config) {
        super(name);
        this.config = config;
        this.zl = config.getZl();
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
        // each group has around 10 elements.
        int groupNum = num / 10 + 1;
        int groupSize = num / groupNum;
        // generate inputs
        byte[][] groupings = new byte[num][];
        BigInteger[] aggs = new BigInteger[num];
        for (int i = 0; i < num; i++) {
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

        SecureRandom secureRandom = new SecureRandom();
        BitVector[] originDataVec = ZlDatabase.create(zl.getL(), aggs).bitPartition(EnvType.STANDARD, true);
        BitVector[] agg0 = IntStream.range(0, originDataVec.length).mapToObj(i -> BitVectorFactory.createRandom(num, secureRandom)).toArray(BitVector[]::new);
        SquareZ2Vector[] aggShares0 = Arrays.stream(agg0).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] aggShares1 = IntStream.range(0, agg0.length).mapToObj(i -> SquareZ2Vector.create(agg0[i].xor(originDataVec[i]), false)).toArray(SquareZ2Vector[]::new);

        // init the protocol
        PrefixAggParty sender = PrefixMaxFactory.createPrefixMaxSender(firstRpc, secondRpc.ownParty(), config);
        PrefixAggParty receiver = PrefixMaxFactory.createPrefixMaxReceiver(secondRpc, firstRpc.ownParty(), config);

        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            LOGGER.info("-----test {} start-----", sender.getPtoDesc().getPtoName());
            PrefixAggPartyThread senderThread = new PrefixAggPartyThread(sender, groupShares0, aggShares0);
            PrefixAggPartyThread receiverThread = new PrefixAggPartyThread(receiver, groupShares1, aggShares1);
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
            PrefixAggOutput shareZ0 = senderThread.getShareZ();
            PrefixAggOutput shareZ1 = receiverThread.getShareZ();
            assertOutput(groupings, aggs, shareZ0, shareZ1);
            printAndResetRpc(time);
            LOGGER.info("-----test {} end-----", sender.getPtoDesc().getPtoName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // destroy
        new Thread(sender::destroy).start();
        new Thread(receiver::destroy).start();
    }

    private void assertOutput(byte[][] groupings, BigInteger[] aggs, PrefixAggOutput shareZ0, PrefixAggOutput shareZ1) {
        int num = aggs.length;
        // true
        List<BigInteger> trueGroup = IntStream.range(0, num).mapToObj(i -> BigIntegerUtils.byteArrayToNonNegBigInteger(groupings[i])).collect(Collectors.toList());
        List<BigInteger> trueAgg = Arrays.asList(aggs);
        Map<BigInteger, BigInteger> trueMap = genTrue(trueGroup, trueAgg);
        // result
        List<BigInteger> resultGroup = IntStream.range(0, num).mapToObj(i -> BytesUtils.xor(shareZ0.getGroupings().elementAt(i), shareZ1.getGroupings().elementAt(i)))
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger).collect(Collectors.toList());
        BitVector[] aggVec = IntStream.range(0, zl.getL()).mapToObj(i -> shareZ0.getAggsBinary()[i].getBitVector().xor(shareZ1.getAggsBinary()[i].getBitVector())).toArray(BitVector[]::new);
        List<BigInteger> resultAgg = Arrays.stream(ZlDatabase.create(EnvType.STANDARD, true, aggVec).getBigIntegerData()).collect(Collectors.toList());
        BitVector flag = shareZ0.getIndicator().getBitVector().xor(shareZ1.getIndicator().getBitVector());
        int[] indexes = IntStream.range(0, flag.bitNum()).filter(flag::get).toArray();
        Map<BigInteger, BigInteger> resultMap = new HashMap<>();
        for (int index : indexes) {
            resultMap.put(resultGroup.get(index), resultAgg.get(index));
        }

        // result
        Assert.assertEquals(trueMap, resultMap);
    }

    private Map<BigInteger, BigInteger> genTrue(List<BigInteger> trueGroup, List<BigInteger> trueAgg) {
        int num = trueAgg.size();
        Map<BigInteger, BigInteger> map = new HashMap<>();
        for (int i = 0; i < num; i++) {
            BigInteger key = trueGroup.get(i);
            if (map.containsKey(key)) {
                map.put(key, map.get(key).compareTo(trueAgg.get(i)) < 0 ? trueAgg.get(i) : map.get(key));
            } else {
                map.put(key, trueAgg.get(i));
            }
        }
        return map;
    }

}
