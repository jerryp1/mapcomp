package edu.alibaba.mpc4j.common.circuit.z2;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.operator.Z2IntegerOperator;
import edu.alibaba.mpc4j.common.circuit.z2.psorter.PsorterFactory;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.Zl64Database;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import org.apache.commons.lang3.StringUtils;
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
import java.util.stream.LongStream;

import static edu.alibaba.mpc4j.common.circuit.z2.psorter.PSorterUtils.extendBitsWithSkip;

@RunWith(Parameterized.class)
public class Z2PsorterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2SorterTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default num of payload to be sorted
     */
    private static final int DEFAULT_PAYLOAD_NUM = 0;
    /**
     * large num of payload to be sorted
     */
    private static final int LARGE_PAYLOAD_NUM = 5;
    /**
     * default num of elements to be sorted
     */
    private static final int DEFAULT_SORTED_NUM = 9227;
    /**
     * large num of elements to be sorted
     */
    private static final int LARGE_SORTED_NUM = 1<<20;
    /**
     * default l
     */
    private static final int DEFAULT_L = IntUtils.MAX_L;
    /**
     * large l
     */
    private static final int LARGE_L = LongUtils.MAX_L;
    /**
     * the config
     */
    private final Z2CircuitConfig config;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Bitonic sorter.
        configurations.add(new Object[]{
            PsorterFactory.SorterTypes.BITONIC + " (bitonic sorter)",
            new Z2CircuitConfig.Builder().setPsorterType(PsorterFactory.SorterTypes.BITONIC).build()
        });
        return configurations;
    }

    public Z2PsorterTest(String name, Z2CircuitConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.config = config;
    }

    @Test
    public void test1SortedNum() {
        testRandom(1, DEFAULT_PAYLOAD_NUM);
        testRandom(1, LARGE_PAYLOAD_NUM);
    }

    @Test
    public void test4SortedNum() {
        testRandom(4, DEFAULT_PAYLOAD_NUM);
        testRandom(4, LARGE_PAYLOAD_NUM);
    }

    @Test
    public void test8SortedNum() {
        testRandom(8, DEFAULT_PAYLOAD_NUM);
        testRandom(8, LARGE_PAYLOAD_NUM);
    }

    @Test
    public void testDefaultSortedNum() {
        testRandom(DEFAULT_SORTED_NUM, DEFAULT_PAYLOAD_NUM);
        testRandom(DEFAULT_SORTED_NUM, LARGE_PAYLOAD_NUM);
    }

    @Test
    public void testLargeSortedNum() {
        testRandom(LARGE_SORTED_NUM, DEFAULT_PAYLOAD_NUM);
        testRandom(LARGE_SORTED_NUM, LARGE_PAYLOAD_NUM);
    }

    @Test
    public void print() {
        byte x = 12;
        byte y = (byte) 0b11000000;
        LOGGER.info("{}, {}, {}", x, y, x^ y);
        byte[] z = new byte[]{(byte) 0b11000000};
        BitVector bitVector = BitVectorFactory.create(8, z);
        byte[] res = extendBitsWithSkip(PlainZ2Vector.create(bitVector), 16, 4);
        LOGGER.info("{}, {}", z[0], Arrays.toString(res));


        LOGGER.info("{}", (((byte)0b11000000) & 112)>>-2);
    }

    private void testRandom(int numOfSorted, int payloadNum) {
        testRandom(DEFAULT_L, numOfSorted, payloadNum);
        testRandom(LARGE_L, numOfSorted, payloadNum);
    }

    private void testRandom(int l, int numOfSorted, int payloadNum) {
        long[] longXs = LongStream.range(0, numOfSorted).map(i ->
            LongUtils.randomNonNegative(1L << (l - 1), SECURE_RANDOM)).toArray();
//        long[] longXs = new long[]{293444041, 439314484, 522936364, 336724549, 365459336, 427195624, 504998883, 442033789, 187036780, 383737104, 425645647, 225121242, 157430306, 249357844, 512034818, 504478767};
//        long[] longXs = new long[] {293444041, 336724549, 365459336, 383737104, 249357844, 225121242, 187036780, 157430306};
//        long[] longXs = new long[]{196523038, 334458079, 140606183, 325977581, 468430235, 451846663, 447049194};
//        long[] longXs = new long[]{277050142, 439990206, 389952731, 180042966, 87871128, 378820280, 354665502, 523167145, 19095359, 190713120, 218836599, 369378989, 11568877, 330169217, 11963738, 66435795, 256240879, 526036727, 149220251};
        long[][] payload = payloadNum == 0 ? null : IntStream.range(0, payloadNum).mapToObj(i ->
            LongStream.range(0, numOfSorted).map(j ->
                    LongUtils.randomNonNegative(1L << (l - 1), SECURE_RANDOM))
                .toArray()).toArray(long[][]::new);
        testPto(l, longXs, payload);
    }


    private void testPto(int l, long[] longXs, long[][] payload) {
        int numOfSorted = longXs.length;
        LOGGER.info("test random ({}), l = {}, num of sorted elements = {}, num of payload = {}", Z2IntegerOperator.P_SORT, l, numOfSorted, payload == null ? 0 : payload.length);
//        LOGGER.info("original data:{}", Arrays.toString(longXs));
        // partition
        PlainZ2Vector[][] xPlainZ2Vectors = new PlainZ2Vector[][]{Arrays.stream(Zl64Database.create(l, longXs).bitPartition(EnvType.STANDARD, true))
            .map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new)};
        PlainZ2Vector[][] pPlainZ2Vectors = payload == null ? null : Arrays.stream(payload).map(p ->
            Arrays.stream(Zl64Database.create(l, p).bitPartition(EnvType.STANDARD, true))
            .map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new)).toArray(PlainZ2Vector[][]::new);

        // init the protocol
        PlainZ2cParty party = new PlainZ2cParty();
        Z2IntegerCircuitParty partyThread = new Z2IntegerCircuitParty(party, Z2IntegerOperator.P_SORT, xPlainZ2Vectors, pPlainZ2Vectors, config);
        partyThread.setPsorterConfig(PlainZ2Vector.createOnes(1), true);
        StopWatch stopWatch = new StopWatch();
        // execute the circuit
        LOGGER.info("sorting start");
        stopWatch.start();
        partyThread.run();
        stopWatch.stop();
        long sortTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        LOGGER.info("sort time:{}", sortTime);
        LOGGER.info("------------------------------");
        // verify
        int[] permutation = Arrays.stream(transport(partyThread.getZ())).mapToInt(x -> (int) x).toArray();
        long[] xSorted = transport(xPlainZ2Vectors[0]);
        long[][] payloadSorted = pPlainZ2Vectors == null ? null : Arrays.stream(pPlainZ2Vectors).map(Z2PsorterTest::transport).toArray(long[][]::new);

        Z2CircuitTestUtils.assertPsortOutput(l, longXs, payload, permutation, xSorted, payloadSorted);
    }

    public static long[] transport(MpcZ2Vector[] data){
        BitVector[] permutationVec = Arrays.stream(data).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new);
        BigInteger[] permutationVecTrans = ZlDatabase.create(EnvType.STANDARD, false, permutationVec).getBigIntegerData();
        return Arrays.stream(permutationVecTrans).mapToLong(BigInteger::longValue).toArray();
    }
}
