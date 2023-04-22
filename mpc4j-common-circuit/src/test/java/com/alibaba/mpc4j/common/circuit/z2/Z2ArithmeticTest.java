package com.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.EnvType.STANDARD_JDK;

/**
 * Z2 Plain Arithmetic Test.
 * Plain inputs are created in [0, 2^(arithLength-1)) to avoid overflow in subtraction, which is suggested in
 * https://www.doc.ic.ac.uk/~eedwards/compsys/arithmetic/index.html
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/21
 */
public class Z2ArithmeticTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z2ArithmeticTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1024;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 18;
    /**
     * default arithmetic length
     */
    private static final int DEFAULT_ARITH_LENGTH = 64;
    /**
     * large arithmetic length
     */
    private static final int LARGE_ARITH_LENGTH = 128;
    /**
     * environment type
     */
    private static final EnvType envType = STANDARD_JDK;

    @Test
    public void test1Num() {
        testAllOperators(DEFAULT_ARITH_LENGTH, 1);
    }

    @Test
    public void test2Num() {
        testAllOperators(DEFAULT_ARITH_LENGTH, 2);
    }

    @Test
    public void test8Num() {
        testAllOperators(DEFAULT_ARITH_LENGTH, 8);
    }

    @Test
    public void testDefaultNum() {
        testAllOperators(DEFAULT_ARITH_LENGTH, DEFAULT_NUM);
    }

    @Test
    public void testParallelDefaultNum() {
        testAllOperators(DEFAULT_ARITH_LENGTH, DEFAULT_NUM);
    }

    @Test
    public void testSmallZl() {
        testAllOperators(DEFAULT_ARITH_LENGTH, DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        testAllOperators(DEFAULT_ARITH_LENGTH, LARGE_NUM);
    }

    @Test
    public void testParallelLargeNum() {
        testAllOperators(DEFAULT_ARITH_LENGTH, LARGE_NUM);
    }

    public void testAllOperators(int arithLength, int num) {
        // all the operator to be tested
        testCircuit(ArithmeticOperator.LEQ, arithLength, num);
        testCircuit(ArithmeticOperator.BIT_ADD, arithLength, num);
        testCircuit(ArithmeticOperator.ADD, arithLength, num);
        testCircuit(ArithmeticOperator.SUB, arithLength, num);
        testCircuit(ArithmeticOperator.NOT, arithLength, num);
    }

    private void testCircuit(ArithmeticOperator operator, int arithLength, int num) {
        byte[][] xBytes = IntStream.range(0, num)
                .mapToObj(i -> new BigInteger(arithLength - 1, SECURE_RANDOM))
                .map(BigIntegerUtils::bigIntegerToByteArray).toArray(byte[][]::new);
        byte[][] yBytes = IntStream.range(0, num)
                .mapToObj(i -> new BigInteger(arithLength - 1, SECURE_RANDOM))
                .map(BigIntegerUtils::bigIntegerToByteArray).toArray(byte[][]::new);

        ZlDatabase zlDatabaseX = ZlDatabase.create(arithLength, xBytes);
        ZlDatabase zlDatabaseY = ZlDatabase.create(arithLength, yBytes);

        BitVector[] xBitVector = zlDatabaseX.bitPartition(envType, false);
        BitVector[] yBitVector = zlDatabaseY.bitPartition(envType, false);
        PlainZ2Vector[] x = Arrays.stream(xBitVector).map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);

        PlainZ2Vector[] y = Arrays.stream(yBitVector).map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
        // init the protocol
        PlainBcParty party = new PlainBcParty();
        try {
            LOGGER.info("-----test {} start-----", party.getType());
            PlainZ2ArithmeticPartyThread partyThread = new PlainZ2ArithmeticPartyThread(party, operator, x, y);
            StopWatch stopWatch = new StopWatch();
            // execute the circuit
            stopWatch.start();
            partyThread.start();
            partyThread.join();
            stopWatch.stop();
            stopWatch.reset();

            PlainZ2Vector[] z = partyThread.getZ();
            assertOutput(operator, zlDatabaseX, zlDatabaseY, z);

            LOGGER.info("-----test {} end-----", party.getType());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(ArithmeticOperator operator, ZlDatabase zlDatabaseX, ZlDatabase zlDatabaseY,
                              PlainZ2Vector[] z) {
        switch (operator) {
            case LEQ:
                assertLeq(zlDatabaseX, zlDatabaseY, z[0]);
                break;
            case BIT_ADD:
                assertBitAdd(zlDatabaseX, zlDatabaseY, z[0]);
                break;
            case ADD:
                assertAdd(zlDatabaseX, zlDatabaseY, z);
                break;
            case SUB:
                assertSub(zlDatabaseX, zlDatabaseY, z);
                break;
            case NOT:
                assertNot(zlDatabaseX, z);
                break;
            default:
                throw new IllegalStateException("Invalid arithmetic operator: " + operator.name());
        }
    }

    private void assertLeq(ZlDatabase zlDatabaseX, ZlDatabase zlDatabaseY,
                           PlainZ2Vector z) {
        int rowNum = zlDatabaseX.rows();

        BitVector zBitVector = z.getBitVector();

        BigInteger[] xData = Arrays.stream(zlDatabaseX.getBigIntegerData())
                .map(v -> v.compareTo(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH - 1)) >= 0
                        ? v.subtract(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH)) : v)
                .toArray(BigInteger[]::new);
        BigInteger[] yData = Arrays.stream(zlDatabaseY.getBigIntegerData())
                .map(v -> v.compareTo(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH - 1)) >= 0
                        ? v.subtract(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH)) : v)
                .toArray(BigInteger[]::new);

        byte[] zBytes = zBitVector.getBytes();
        for (int i = 0; i < rowNum; i++) {
            boolean result = BinaryUtils.getBoolean(zBytes, i);
            boolean plainResult = xData[i].compareTo(yData[i]) < 0;
            Assert.assertEquals(result, plainResult);
        }
    }

    private void assertAdd(ZlDatabase zlDatabaseX, ZlDatabase zlDatabaseY,
                           PlainZ2Vector[] z) {

        BitVector[] zBitVector = IntStream.range(0, zlDatabaseX.getL()).mapToObj(i -> z[i].getBitVector()).toArray(BitVector[]::new);

        ZlDatabase zlDatabaseZ = ZlDatabase.create(envType, false, zBitVector);

        BigInteger[] xData = zlDatabaseX.getBigIntegerData();
        BigInteger[] yData = zlDatabaseY.getBigIntegerData();
        BigInteger[] xAddYData = IntStream.range(0, xData.length).mapToObj(i -> xData[i].add(yData[i]).mod(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH))).toArray(BigInteger[]::new);
        BigInteger[] zData = zlDatabaseZ.getBigIntegerData();

        for (int i = 0; i < zlDatabaseX.rows(); i++) {
            Assert.assertEquals(xAddYData[i], zData[i]);
        }
    }

    private void assertSub(ZlDatabase zlDatabaseX, ZlDatabase zlDatabaseY,
                           PlainZ2Vector[] z) {

        BitVector[] zBitVector = IntStream.range(0, zlDatabaseX.getL()).mapToObj(i -> z[i].getBitVector()).toArray(BitVector[]::new);
        ZlDatabase zlDatabaseZ = ZlDatabase.create(envType, false, zBitVector);

        BigInteger[] xData = Arrays.stream(zlDatabaseX.getBigIntegerData())
                .map(v -> v.compareTo(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH - 1)) >= 0
                        ? v.subtract(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH)) : v)
                .toArray(BigInteger[]::new);
        BigInteger[] yData = Arrays.stream(zlDatabaseY.getBigIntegerData())
                .map(v -> v.compareTo(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH - 1)) >= 0
                        ? v.subtract(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH)) : v)
                .toArray(BigInteger[]::new);
        BigInteger[] xSubYData = IntStream.range(0, xData.length)
                .mapToObj(i -> xData[i].subtract(yData[i]).mod(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH)))
                .map(v -> v.compareTo(BigInteger.ZERO) < 0 ? v.add(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH)) : v).toArray(BigInteger[]::new);
        BigInteger[] zData = zlDatabaseZ.getBigIntegerData();

        for (int i = 0; i < zlDatabaseX.rows(); i++) {
            Assert.assertEquals(xSubYData[i], zData[i]);
        }
    }

    private void assertNot(ZlDatabase zlDatabaseX,
                           PlainZ2Vector[] z) {
        BitVector[] zBitVector = IntStream.range(0, zlDatabaseX.getL())
                .mapToObj(i -> z[i].getBitVector()).toArray(BitVector[]::new);

        ZlDatabase zlDatabaseZ = ZlDatabase.create(envType, false, zBitVector);

        BigInteger[] notXData = Arrays.stream(zlDatabaseX.getBigIntegerData())
                .map(xDatum -> xDatum.negate().subtract(BigInteger.ONE).mod(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH)))
                .map(v -> v.compareTo(BigInteger.ZERO) < 0 ? v.add(BigInteger.ONE.shiftLeft(DEFAULT_ARITH_LENGTH)) : v)
                .toArray(BigInteger[]::new);
        BigInteger[] zData = zlDatabaseZ.getBigIntegerData();

        for (int i = 0; i < notXData.length; i++) {
            Assert.assertEquals(notXData[i], zData[i]);
        }
    }

    private void assertBitAdd(ZlDatabase zlDatabaseX, ZlDatabase zlDatabaseY,
                              PlainZ2Vector z) {
        int rowNum = zlDatabaseX.rows();

        BitVector x = zlDatabaseX.bitPartition(envType, false)[0];
        BitVector y = zlDatabaseY.bitPartition(envType, false)[0];
        BitVector result = z.getBitVector();
        BitVector plainResult = x.xor(y);
        for (int i = 0; i < rowNum; i++) {
            Assert.assertEquals(result.get(i), plainResult.get(i));
        }
    }
}
