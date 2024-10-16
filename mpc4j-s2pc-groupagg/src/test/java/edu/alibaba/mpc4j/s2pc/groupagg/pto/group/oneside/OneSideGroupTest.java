package edu.alibaba.mpc4j.s2pc.groupagg.pto.group.oneside;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.GroupTypes.AggTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.oneside.OneSideGroupFactory.OneSideGroupType;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.oneside.amos22.Amos22OneSideGroupConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * group aggregation test, where the group flag is plaintext to receiver
 *
 */
@RunWith(Parameterized.class)
public class OneSideGroupTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OneSideGroupTest.class);
    /**
     * the number of attributes
     */
    private static final int ATTR_NUM = 3;
    /**
     * bitLen
     */
    private static final int[] bitLens = new int[]{8, 64};
    /**
     * default small size
     */
    private static final int DEFAULT_SMALL_SIZE = 99;
    /**
     * 较大数量
     */
    private static final int LARGE_SIZE = 1 << 14;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        for (int bitLen : bitLens) {
            configurations.add(new Object[]{
                OneSideGroupType.AMOS22_ONE_SIDE.name() + "_bitLen_" + bitLen,
                bitLen,
                new Amos22OneSideGroupConfig.Builder(false).build(),
            });
            configurations.add(new Object[]{
                OneSideGroupType.AMOS22_ONE_SIDE.name() + "_silent_bitLen_" + bitLen,
                bitLen,
                new Amos22OneSideGroupConfig.Builder(true).build(),
            });
        }
        return configurations;
    }

    /**
     * the config
     */
    private final OneSideGroupConfig config;

    /**
     * the domainBitLen
     */
    private final int domainBitLen;

    public OneSideGroupTest(String name, int domainBitLen, OneSideGroupConfig config) {
        super(name);
        this.domainBitLen = domainBitLen;
        this.config = config;
    }

    @Test
    public void test2() {
        testPto(1, 2, domainBitLen, false);
        testPto(ATTR_NUM, 2, domainBitLen, false);
    }

    @Test
    public void test7() {
        testPto(1, 7, domainBitLen, false);
        testPto(ATTR_NUM, 7, domainBitLen, false);
    }

    @Test
    public void test8() {
        testPto(1, 8, domainBitLen, false);
        testPto(ATTR_NUM, 8, domainBitLen, false);
    }

    @Test
    public void test10() {
        testPto(1, 10, domainBitLen, false);
        testPto(ATTR_NUM, 10, domainBitLen, false);
    }

    @Test
    public void testDefault() {
        testPto(1, DEFAULT_SMALL_SIZE, domainBitLen, false);
        testPto(ATTR_NUM, DEFAULT_SMALL_SIZE, domainBitLen, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(1, DEFAULT_SMALL_SIZE, domainBitLen, true);
        testPto(ATTR_NUM, DEFAULT_SMALL_SIZE, domainBitLen, true);
    }

    @Test
    public void testLarge() {
        testPto(1, LARGE_SIZE, domainBitLen, false);
        testPto(ATTR_NUM, LARGE_SIZE, domainBitLen, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(1, LARGE_SIZE, domainBitLen, true);
        testPto(ATTR_NUM, LARGE_SIZE, domainBitLen, true);
    }

    private void testPto(int attrNum, int listSize, int domainBitLen, boolean parallel) {
        OneSideGroupParty sender = OneSideGroupFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        OneSideGroupParty receiver = OneSideGroupFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，attrNum = {}, listSize = {}，domainBitLen = {}-----",
                sender.getPtoDesc().getPtoName(), attrNum, listSize, domainBitLen
            );
            SecureRandom secureRandom = new SecureRandom();
            AggTypes[] aggTypes = new AggTypes[attrNum];
            BitVector[][] data = new BitVector[attrNum][];
            SquareZ2Vector[][] s0 = new SquareZ2Vector[attrNum][], s1 = new SquareZ2Vector[attrNum][];
            BitVector[] validFlag = new BitVector[attrNum];
            BitVector[] f0 = new BitVector[attrNum], f1 = new BitVector[attrNum];
            IntStream.range(0, attrNum).forEach(i -> {
                aggTypes[i] = secureRandom.nextBoolean() ? AggTypes.MIN : AggTypes.MAX;
                data[i] = IntStream.range(0, domainBitLen).mapToObj(j ->
                    BitVectorFactory.createRandom(listSize, secureRandom)).toArray(BitVector[]::new);
                validFlag[i] = BitVectorFactory.createRandom(listSize, secureRandom);
                f0[i] = BitVectorFactory.createRandom(listSize, secureRandom);
                f1[i] = f0[i].xor(validFlag[i]);
                BitVector[] tmp = IntStream.range(0, domainBitLen).mapToObj(j ->
                    BitVectorFactory.createRandom(listSize, secureRandom)).toArray(BitVector[]::new);
                s0[i] = Arrays.stream(tmp).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
                BitVector[] tmp1 = IntStream.range(0, domainBitLen).mapToObj(j ->
                    data[i][j].xor(tmp[j])).toArray(BitVector[]::new);
                s1[i] = Arrays.stream(tmp1).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
            });
            BitVector groupFlag = BitVectorFactory.createZeros(listSize);
            // set group flag, make sure the first bit is 1
            int possibleGroupNum = Math.max(listSize >> 2, 1);
            IntStream.range(0, possibleGroupNum).forEach(i -> groupFlag.set(secureRandom.nextInt(listSize), true));
            groupFlag.set(0, true);
            OneSideGroupPartyThread senderThread = new OneSideGroupPartyThread(sender, s0,
                Arrays.stream(f0).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new), aggTypes, null);
            OneSideGroupPartyThread receiverThread = new OneSideGroupPartyThread(receiver, s1,
                Arrays.stream(f1).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new), aggTypes, groupFlag);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            int[] pos = receiver.getResPosFlag(groupFlag);
            SquareZ2Vector[][] senderOutput = senderThread.getGroupRes();
            SquareZ2Vector[][] receiverOutput = receiverThread.getGroupRes();
            BitVector[][] res = IntStream.range(0, attrNum).mapToObj(attrIndex -> IntStream.range(0, domainBitLen).mapToObj(i ->
                senderOutput[attrIndex][i].getBitVector().xor(receiverOutput[attrIndex][i].getBitVector())).toArray(BitVector[]::new)).toArray(BitVector[][]::new);
            assertOutput(data, groupFlag, validFlag, res, aggTypes, pos);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void assertOutput(BitVector[][] data, BitVector groupFlag, BitVector[] validFlag, BitVector[][] res, AggTypes[] type, int[] targetIndexes) {
        boolean[] gFlag = BinaryUtils.byteArrayToBinary(groupFlag.getBytes(), groupFlag.bitNum());
        BigInteger[] nullValue = Arrays.stream(type).map(x -> x.equals(AggTypes.MAX) ? BigInteger.ZERO : BigInteger.ONE.shiftLeft(data[0].length).subtract(BigInteger.ONE)).toArray(BigInteger[]::new);
        BigInteger[][] origin = IntStream.range(0, data.length).mapToObj(i -> {
            BitVector[] x = data[i];
            BigInteger[] tmp = ZlDatabase.create(EnvType.STANDARD, true, x).getBigIntegerData();
            for (int j = 0; j < gFlag.length; j++) {
                tmp[j] = validFlag[i].get(j) ? tmp[j] : nullValue[i];
            }
            return tmp;
        }).toArray(BigInteger[][]::new);
        BigInteger[][] resData = Arrays.stream(res).map(x -> ZlDatabase.create(EnvType.STANDARD, true, x).getBigIntegerData()).toArray(BigInteger[][]::new);
        int groupIndex = 0;
        BigInteger[] tmp = Arrays.copyOf(nullValue, data.length);
        for (int i = 0; i < groupFlag.bitNum(); i++) {
            for (int j = 0; j < data.length; j++) {
                if (type[j].equals(AggTypes.MAX)) {
                    tmp[j] = tmp[j].compareTo(origin[j][i]) > 0 ? tmp[j] : origin[j][i];
                } else {
                    tmp[j] = tmp[j].compareTo(origin[j][i]) < 0 ? tmp[j] : origin[j][i];
                }
            }
            if (i + 1 == groupFlag.bitNum() || gFlag[i + 1]) {
                for (int j = 0; j < data.length; j++) {
                    BigInteger compRes = resData[j][targetIndexes[groupIndex]];
                    assert compRes.compareTo(tmp[j]) == 0;
                }
                groupIndex++;
                tmp = Arrays.copyOf(nullValue, data.length);
            }
        }
        assert groupIndex == targetIndexes.length;
    }
}
