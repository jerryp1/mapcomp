package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory.AggTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory.OneSideGroupType;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22.Amos22OneSideGroupConfig;
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

@RunWith(Parameterized.class)
public class OneSideGroupTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OneSideGroupTest.class);
    /**
     * bitLen
     */
    private static final int[] bitLens = new int[]{8, 53};
    /**
     * default small size
     */
    private static final int DEFAULT_SMALL_SIZE = 99;
    /**
     * 较大数量
     */
    private static final int LARGE_SIZE = 1 << 16;

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
        testPto(2, domainBitLen, false);
    }

    @Test
    public void test8() {
        testPto(8, domainBitLen, false);
    }

    @Test
    public void test10() {
        testPto(10, domainBitLen, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_SMALL_SIZE, domainBitLen, true);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_SMALL_SIZE, domainBitLen, true);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_SIZE, domainBitLen, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_SIZE, domainBitLen, true);
    }


    private void testPto(int listSize, int domainBitLen, boolean parallel) {
        OneSideGroupParty sender = OneSideGroupFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        OneSideGroupParty receiver = OneSideGroupFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，listSize = {}，domainBitLen = {}-----",
                sender.getPtoDesc().getPtoName(), listSize, domainBitLen
            );
            // generate the inputs
            SecureRandom secureRandom = new SecureRandom();
            BitVector[] data = IntStream.range(0, domainBitLen).mapToObj(i ->
                BitVectorFactory.createRandom(listSize, secureRandom)).toArray(BitVector[]::new);
            // 先不测试validFlag的影响
            BitVector validFlag = BitVectorFactory.createRandom(listSize, secureRandom);
            BitVector groupFlag = BitVectorFactory.create(8, new byte[]{17});
//            BitVector groupFlag = BitVectorFactory.createZeros(listSize);
//            // 设置group flag，保证最后一个bit是1
//            int possibleGroupNum = Math.max(listSize>>2, 1);
//            IntStream.range(0, possibleGroupNum).forEach(i -> groupFlag.set(secureRandom.nextInt(listSize), true));
//            groupFlag.set(listSize - 1, true);

            BitVector[] s0 = IntStream.range(0, domainBitLen).mapToObj(i ->
                BitVectorFactory.createRandom(listSize, secureRandom)).toArray(BitVector[]::new);
            BitVector[] s1 = IntStream.range(0, domainBitLen).mapToObj(i ->
                data[i].xor(s0[i])).toArray(BitVector[]::new);

            OneSideGroupPartyThread senderThread = new OneSideGroupPartyThread(sender,
                Arrays.stream(s0).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new), null, AggTypes.MAX, null);
            OneSideGroupPartyThread receiverThread = new OneSideGroupPartyThread(receiver,
                Arrays.stream(s1).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new), null, AggTypes.MAX, groupFlag);
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
            SquareZ2Vector[] senderOutput = senderThread.getGroupRes();
            SquareZ2Vector[] receiverOutput = receiverThread.getGroupRes();
            BitVector[] res = IntStream.range(0, domainBitLen).mapToObj(i ->
                senderOutput[i].getBitVector().xor(receiverOutput[i].getBitVector())).toArray(BitVector[]::new);
            assertOutput(data, groupFlag, validFlag, res, AggTypes.MAX);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void assertOutput(BitVector[] data, BitVector groupFlag, BitVector validFlag, BitVector[] res, AggTypes type){
        boolean[] gFlag = BinaryUtils.byteArrayToBinary(groupFlag.getBytes(), groupFlag.bitNum());
        LOGGER.info(Arrays.toString(gFlag));
        int[] targetIndexes = OneSideGroupUtils.getResPosFlag(gFlag);
        BigInteger[] origin = ZlDatabase.create(EnvType.STANDARD, true, data).getBigIntegerData();
        BigInteger[] resData = ZlDatabase.create(EnvType.STANDARD, true, res).getBigIntegerData();
        int groupIndex = 0;
        BigInteger nullValue = type.equals(AggTypes.MAX) ? BigInteger.ZERO : BigInteger.ONE.shiftLeft(data.length).subtract(BigInteger.ONE);
        BigInteger tmp = nullValue;
        for(int i = 0; i < groupFlag.bitNum(); i++){
            if(type.equals(AggTypes.MAX)){
                tmp = tmp.compareTo(origin[i]) > 0 ? tmp : origin[i];
            }else{
                tmp = tmp.compareTo(origin[i]) < 0 ? tmp : origin[i];
            }
            if(gFlag[i]){
                BigInteger compRes = resData[groupIndex++];
                assert compRes.compareTo(tmp) == 0;
                tmp = nullValue;
            }
        }
        assert groupIndex == targetIndexes.length;
    }


}
