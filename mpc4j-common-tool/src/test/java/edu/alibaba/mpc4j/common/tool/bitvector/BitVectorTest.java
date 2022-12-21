package edu.alibaba.mpc4j.common.tool.bitvector;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory.BitVectorType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * BitVector tests.
 *
 * @author Weiran Liu
 * @date 2022/12/16
 */
@RunWith(Parameterized.class)
public class BitVectorTest {
    /**
     * the minimum test bit num
     */
    private static final int MIN_BIT_NUM = 1;
    /**
     * the maximum test bit num
     */
    private static final int MAX_BIT_NUM = 128;
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * the test type
     */
    private final BitVectorType type;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configuration = new ArrayList<>();
        // BIGINTEGER_BIT_VECTOR
        configuration.add(new Object[]{BitVectorType.BIGINTEGER_BIT_VECTOR.name(), BitVectorType.BIGINTEGER_BIT_VECTOR,});
        // BYTES_BIT_VECTOR
        configuration.add(new Object[]{BitVectorType.BYTES_BIT_VECTOR.name(), BitVectorType.BYTES_BIT_VECTOR,});

        return configuration;
    }

    public BitVectorTest(String name, BitVectorType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        // create vector with 0 length
        Assert.assertThrows(AssertionError.class, () -> BitVectorFactory.create(type, 0, new byte[0]));
        // create vector with a mismatch bit length
        Assert.assertThrows(AssertionError.class, () -> BitVectorFactory.create(type, 1, new byte[]{(byte) 0x0F,}));
        Assert.assertThrows(AssertionError.class, () -> BitVectorFactory.create(type, 1, BigInteger.valueOf(2)));
        // create vector with a mismatch byte length
        Assert.assertThrows(AssertionError.class, () -> BitVectorFactory.create(type, 1, new byte[2]));
        Assert.assertThrows(AssertionError.class, () -> BitVectorFactory.create(type, 9, new byte[1]));
        // create vector with negative input
        Assert.assertThrows(AssertionError.class, () -> BitVectorFactory.create(type, 1, BigInteger.valueOf(-1)));
    }

    @Test
    public void testIllegalUpdate() {
        // split vector with 0 length
        Assert.assertThrows(AssertionError.class, () -> {
            BitVector bitVector = BitVectorFactory.createOnes(type, 4);
            bitVector.split(0);
        });
        // split vector with large length
        Assert.assertThrows(AssertionError.class, () -> {
            BitVector bitVector = BitVectorFactory.createOnes(type, 4);
            bitVector.split(5);
        });
        // reduce vector with 0 length
        Assert.assertThrows(AssertionError.class, () -> {
            BitVector bitVector = BitVectorFactory.createOnes(type, 4);
            bitVector.reduce(0);
        });
        // reduce vector with large length
        Assert.assertThrows(AssertionError.class, () -> {
            BitVector bitVector = BitVectorFactory.createOnes(type, 4);
            bitVector.reduce(5);
        });
    }

    @Test
    public void testIllegalOperate() {
        // XOR vector with mismatch bit lengths
        Assert.assertThrows(AssertionError.class, () -> {
            BitVector vector0 = BitVectorFactory.createOnes(type, 4);
            BitVector vector1 = BitVectorFactory.createOnes(type, 5);
            vector0.xor(vector1);
        });
        // inner XOR vector with mismatch bit lengths
        Assert.assertThrows(AssertionError.class, () -> {
            BitVector vector0 = BitVectorFactory.createOnes(type, 4);
            BitVector vector1 = BitVectorFactory.createOnes(type, 5);
            vector0.xori(vector1);
        });
        // AND vector with mismatch bit lengths
        Assert.assertThrows(AssertionError.class, () -> {
            BitVector vector0 = BitVectorFactory.createOnes(type, 4);
            BitVector vector1 = BitVectorFactory.createOnes(type, 5);
            vector0.and(vector1);
        });
        // inner AND vector with mismatch bit lengths
        Assert.assertThrows(AssertionError.class, () -> {
            BitVector vector0 = BitVectorFactory.createOnes(type, 4);
            BitVector vector1 = BitVectorFactory.createOnes(type, 5);
            vector0.andi(vector1);
        });
        // OR vector with mismatch bit lengths
        Assert.assertThrows(AssertionError.class, () -> {
            BitVector vector0 = BitVectorFactory.createOnes(type, 4);
            BitVector vector1 = BitVectorFactory.createOnes(type, 5);
            vector0.or(vector1);
        });
        // inner OR vector with mismatch bit lengths
        Assert.assertThrows(AssertionError.class, () -> {
            BitVector vector0 = BitVectorFactory.createOnes(type, 4);
            BitVector vector1 = BitVectorFactory.createOnes(type, 5);
            vector0.ori(vector1);
        });
    }

    @Test
    public void testCreate() {
        for (int bitNum = MIN_BIT_NUM; bitNum < MAX_BIT_NUM; bitNum++) {
            // create with assigned bytes
            testCreateFromBytes(bitNum);
            // create with assigned BigInteger
            testCreateFromBigInteger(bitNum);
            // create random
            testCreateRandom(bitNum);
            // create ones
            testCreateOnes(bitNum);
            // create zeros
            testCreateZeros(bitNum);
        }
        // create empty
        BitVector emptyBitVector = BitVectorFactory.createEmpty(type);
        assertEmptyCorrectness(emptyBitVector);
    }

    private void testCreateFromBytes(int bitNum) {
        // create all 1
        int byteNum = CommonUtils.getByteLength(bitNum);
        byte[] onesBytes = new byte[byteNum];
        Arrays.fill(onesBytes, (byte) 0xFF);
        BytesUtils.reduceByteArray(onesBytes, bitNum);
        BitVector onesBitVector = BitVectorFactory.create(type, bitNum, onesBytes);
        assertOnesCorrectness(onesBitVector, bitNum);
        // create all 0
        byte[] zerosBytes = new byte[byteNum];
        BitVector zerosBitVector = BitVectorFactory.create(type, bitNum, zerosBytes);
        assertZerosCorrectness(zerosBitVector, bitNum);
    }

    private void testCreateFromBigInteger(int bitNum) {
        // create all 1
        BigInteger onesBigInteger = BigInteger.ONE.shiftLeft(bitNum).subtract(BigInteger.ONE);
        BitVector onesBitVector = BitVectorFactory.create(type, bitNum, onesBigInteger);
        assertOnesCorrectness(onesBitVector, bitNum);
        // create all 0
        BigInteger zerosBigInteger = BigInteger.ZERO;
        BitVector zerosBitVector = BitVectorFactory.create(type, bitNum, zerosBigInteger);
        assertZerosCorrectness(zerosBitVector, bitNum);
    }

    private void testCreateRandom(int bitNum) {
        BitVector randomBitVector = BitVectorFactory.createRandom(type, bitNum, SECURE_RANDOM);
        assertRandomCorrectness(randomBitVector, bitNum);
    }

    private void testCreateOnes(int bitNum) {
        BitVector onesBitVector = BitVectorFactory.createOnes(type, bitNum);
        assertOnesCorrectness(onesBitVector, bitNum);
    }

    private void testCreateZeros(int bitNum) {
        BitVector zerosBitVector = BitVectorFactory.createZeros(type, bitNum);
        assertZerosCorrectness(zerosBitVector, bitNum);
    }

    @Test
    public void testReduce() {
        for (int bitNum = MIN_BIT_NUM; bitNum < MAX_BIT_NUM; bitNum++) {
            testReduce(bitNum);
        }
    }

    private void testReduce(int bitNum) {
        // reduce 1
        BitVector bitVector1 = BitVectorFactory.createOnes(type, bitNum);
        bitVector1.reduce(1);
        assertOnesCorrectness(bitVector1, 1);
        // reduce all
        BitVector bitVectorAll = BitVectorFactory.createOnes(type, bitNum);
        bitVectorAll.reduce(bitNum);
        assertOnesCorrectness(bitVectorAll, bitNum);
        if (bitNum > 1) {
            // reduce num - 1
            BitVector bitVectorNum = BitVectorFactory.createOnes(type, bitNum);
            bitVectorNum.reduce(bitNum - 1);
            assertOnesCorrectness(bitVectorNum, bitNum - 1);
            // reduce half
            BitVector bitVectorHalf = BitVectorFactory.createOnes(type, bitNum);
            bitVectorHalf.reduce(bitNum / 2);
            assertOnesCorrectness(bitVectorHalf, bitNum / 2);
        }
    }

    @Test
    public void testAllEmptyMerge() {
        BitVector bitVector = BitVectorFactory.createEmpty(type);
        BitVector mergeBitVector = BitVectorFactory.createEmpty(type);
        bitVector.merge(mergeBitVector);
        assertEmptyCorrectness(bitVector);
    }

    @Test
    public void testLeftEmptyMerge() {
        for (int bitNum = MIN_BIT_NUM; bitNum < MAX_BIT_NUM; bitNum++) {
            testLeftEmptyMerge(bitNum);
        }
    }

    private void testLeftEmptyMerge(int bitNum) {
        BitVector bitVector = BitVectorFactory.createEmpty(type);
        BitVector mergeBitVector = BitVectorFactory.createOnes(type, bitNum);
        bitVector.merge(mergeBitVector);
        assertOnesCorrectness(bitVector, bitNum);
    }

    @Test
    public void testRightEmptyMerge() {
        for (int bitNum = MIN_BIT_NUM; bitNum < MAX_BIT_NUM; bitNum++) {
            testRightEmptyMerge(bitNum);
        }
    }

    private void testRightEmptyMerge(int num) {
        BitVector bitVector = BitVectorFactory.createOnes(type, num);
        BitVector mergeBitVector = BitVectorFactory.createEmpty(type);
        bitVector.merge(mergeBitVector);
        assertOnesCorrectness(bitVector, num);
    }

    @Test
    public void testMerge() {
        for (int num1 = MIN_BIT_NUM; num1 < MAX_BIT_NUM; num1++) {
            for (int num2 = MIN_BIT_NUM; num2 < MAX_BIT_NUM; num2++) {
                testMerge(num1, num2);
            }
        }
    }

    private void testMerge(int num1, int num2) {
        BitVector bitVector = BitVectorFactory.createOnes(type, num1);
        BitVector mergeBitVector = BitVectorFactory.createOnes(type, num2);
        bitVector.merge(mergeBitVector);
        assertOnesCorrectness(bitVector, num1 + num2);
    }

    @Test
    public void testSplit() {
        for (int bitNum = MIN_BIT_NUM; bitNum < MAX_BIT_NUM; bitNum++) {
            testSplit(bitNum);
        }
    }

    private void testSplit(int bitNum) {
        // split 1
        BitVector bitVector1 = BitVectorFactory.createOnes(type, bitNum);
        BitVector splitBitVector1 = bitVector1.split(1);
        assertOnesCorrectness(bitVector1, bitNum - 1);
        assertOnesCorrectness(splitBitVector1, 1);
        // split all
        BitVector bitVectorAll = BitVectorFactory.createOnes(type, bitNum);
        BitVector splitBitVectorAll = bitVectorAll.split(bitNum);
        assertOnesCorrectness(bitVectorAll, 0);
        assertOnesCorrectness(splitBitVectorAll, bitNum);
        if (bitNum > 1) {
            // split num - 1
            BitVector bitVectorNum = BitVectorFactory.createOnes(type, bitNum);
            BitVector splitBitVectorNum = bitVectorNum.split(bitNum - 1);
            assertOnesCorrectness(bitVectorNum, 1);
            assertOnesCorrectness(splitBitVectorNum, bitNum - 1);
            // split half
            BitVector bitVectorHalf = BitVectorFactory.createOnes(type, bitNum);
            BitVector splitBitVectorHalf = bitVectorHalf.split(bitNum / 2);
            assertOnesCorrectness(bitVectorHalf, bitNum - bitNum / 2);
            assertOnesCorrectness(splitBitVectorHalf, bitNum / 2);
        }
    }

    @Test
    public void testGet() {
        testGet(new byte[]{0b01000011,},
            new boolean[]{false, true, false, false, false, false, true, true,}
        );
        testGet(
            new byte[]{0b00111010, 0b01000011,},
            new boolean[]{
                false, false, true, true, true, false, true, false,
                false, true, false, false, false, false, true, true,
            }
        );
        testGet(new byte[]{0b01000011,},
            new boolean[]{true, false, false, false, false, true, true,}
        );
        testGet(
            new byte[]{0b00111010, 0b01000011,},
            new boolean[]{
                true, true, true, false, true, false,
                false, true, false, false, false, false, true, true,
            }
        );
    }

    private void testGet(byte[] byteArray, boolean[] binary) {
        int bitNum = binary.length;
        BitVector bitVector = BitVectorFactory.create(type, bitNum, byteArray);
        IntStream.range(0, bitNum).forEach(binaryIndex ->
            Assert.assertEquals(binary[binaryIndex], bitVector.get(binaryIndex))
        );
    }

    @Test
    public void testSet() {
        testSet(8, new byte[]{0b01000011,});
        testSet(16, new byte[]{0b00111010, 0b01000011,});
        testSet(7, new byte[]{0b01000011,});
        testSet(14, new byte[]{0b00111010, 0b01000011,});
    }

    private void testSet(int bitNum, byte[] byteArray) {
        // set every position to 1
        byte[] trueByteArray = Arrays.copyOf(byteArray, byteArray.length);
        BitVector trueBitVector = BitVectorFactory.create(type, bitNum, trueByteArray);
        IntStream.range(0, bitNum).forEach(binaryIndex -> {
            trueBitVector.set(binaryIndex, true);
            Assert.assertTrue(trueBitVector.get(binaryIndex));
        });
        // set every position to 0
        byte[] falseByteArray = Arrays.copyOf(byteArray, byteArray.length);
        BitVector falseBitVector = BitVectorFactory.create(type, bitNum, falseByteArray);
        IntStream.range(0, bitNum).forEach(binaryIndex -> {
            falseBitVector.set(binaryIndex, false);
            Assert.assertFalse(falseBitVector.get(binaryIndex));
        });
    }

    @Test
    public void testXor() {
        BitVector bitVector1;
        BitVector bitVector2;
        BitVector bitVector;
        // empty XOR
        bitVector1 = BitVectorFactory.createEmpty(type);
        bitVector2 = BitVectorFactory.createEmpty(type);
        bitVector = bitVector1.xor(bitVector2);
        assertEmptyCorrectness(bitVector);
        // empty inner XOR
        bitVector1.xori(bitVector2);
        assertEmptyCorrectness(bitVector1);
        // XOR
        int bitNum = 4;
        bitVector1 = BitVectorFactory.create(type, bitNum, new byte[]{0b00000011});
        bitVector2 = BitVectorFactory.create(type, bitNum, new byte[]{0b00000101});
        bitVector = bitVector1.xor(bitVector2);
        assertCorrectness(bitVector, bitNum, new byte[]{0b00000110});
        // inner XOR
        bitVector1.xori(bitVector2);
        assertCorrectness(bitVector1, bitNum, new byte[]{0b00000110});
    }

    @Test
    public void testAnd() {
        BitVector bitVector1;
        BitVector bitVector2;
        BitVector bitVector;
        // empty AND
        bitVector1 = BitVectorFactory.createEmpty(type);
        bitVector2 = BitVectorFactory.createEmpty(type);
        bitVector = bitVector1.and(bitVector2);
        assertEmptyCorrectness(bitVector);
        // empty inner AND
        bitVector1.andi(bitVector2);
        assertEmptyCorrectness(bitVector1);
        // AND
        int bitNum = 4;
        bitVector1 = BitVectorFactory.create(type, bitNum, new byte[]{0b00000011});
        bitVector2 = BitVectorFactory.create(type, bitNum, new byte[]{0b00000101});
        bitVector = bitVector1.and(bitVector2);
        assertCorrectness(bitVector, bitNum, new byte[]{0b00000001});
        // inner AND
        bitVector1.andi(bitVector2);
        assertCorrectness(bitVector1, bitNum, new byte[]{0b00000001});
    }

    @Test
    public void testOr() {
        BitVector bitVector1;
        BitVector bitVector2;
        BitVector bitVector;
        // empty OR
        bitVector1 = BitVectorFactory.createEmpty(type);
        bitVector2 = BitVectorFactory.createEmpty(type);
        bitVector = bitVector1.or(bitVector2);
        assertEmptyCorrectness(bitVector);
        // empty inner OR
        bitVector1.ori(bitVector2);
        assertEmptyCorrectness(bitVector1);
        // OR
        int bitNum = 4;
        bitVector1 = BitVectorFactory.create(type, bitNum, new byte[]{0b00000011});
        bitVector2 = BitVectorFactory.create(type, bitNum, new byte[]{0b00000101});
        bitVector = bitVector1.or(bitVector2);
        assertCorrectness(bitVector, bitNum, new byte[]{0b00000111});
        // inner OR
        bitVector1.ori(bitVector2);
        assertCorrectness(bitVector1, bitNum, new byte[]{0b00000111});
    }

    @Test
    public void testNot() {
        BitVector bitVector1;
        BitVector bitVector;
        // empty NOT
        bitVector1 = BitVectorFactory.createEmpty(type);
        bitVector = bitVector1.not();
        assertEmptyCorrectness(bitVector);
        // empty inner NOT
        bitVector1.noti();
        assertEmptyCorrectness(bitVector1);
        // NOT
        int bitNum = 2;
        bitVector1 = BitVectorFactory.create(type, bitNum, new byte[]{0b00000001});
        bitVector = bitVector1.not();
        assertCorrectness(bitVector, bitNum, new byte[]{0b00000010});
        // inner NOT
        bitVector1.noti();
        assertCorrectness(bitVector1, bitNum, new byte[]{0b00000010});
    }

    private void assertEmptyCorrectness(BitVector bitVector) {
        Assert.assertEquals(type, bitVector.getType());
        Assert.assertEquals(0, bitVector.bitNum());
        Assert.assertEquals(0, bitVector.byteNum());
        Assert.assertEquals(0, bitVector.getBytes().length);
        Assert.assertEquals("", bitVector.toString());
    }

    private void assertZerosCorrectness(BitVector bitVector, int bitNum) {
        if (bitNum == 0) {
            assertEmptyCorrectness(bitVector);
        } else {
            Assert.assertEquals(type, bitVector.getType());
            Assert.assertEquals(bitNum, bitVector.bitNum());
            Assert.assertEquals(CommonUtils.getByteLength(bitNum), bitVector.byteNum());
            String fullZeroString = BigInteger.ONE.shiftLeft(bitNum).toString(2).substring(1);
            Assert.assertEquals(fullZeroString, bitVector.toString());
        }
    }

    private void assertOnesCorrectness(BitVector bitVector, int bitNum) {
        if (bitNum == 0) {
            assertEmptyCorrectness(bitVector);
        } else {
            Assert.assertEquals(type, bitVector.getType());
            Assert.assertEquals(bitNum, bitVector.bitNum());
            Assert.assertEquals(CommonUtils.getByteLength(bitNum), bitVector.byteNum());
            String fullOneString = BigInteger.ONE.shiftLeft(bitNum).subtract(BigInteger.ONE).toString(2);
            Assert.assertEquals(fullOneString, bitVector.toString());
        }
    }

    private void assertRandomCorrectness(BitVector bitVector, int bitNum) {
        if (bitNum == 0) {
            assertEmptyCorrectness(bitVector);
        } else {
            Assert.assertEquals(type, bitVector.getType());
            Assert.assertEquals(bitNum, bitVector.bitNum());
            Assert.assertEquals(CommonUtils.getByteLength(bitNum), bitVector.byteNum());
            // only test length the valid bits
            byte[] bytes = bitVector.getBytes();
            Assert.assertEquals(bitVector.byteNum(), bytes.length);
            Assert.assertTrue(BytesUtils.isReduceByteArray(bytes, bitNum));
        }
    }

    private void assertCorrectness(BitVector bitVector, int bitNum, byte[] expectBytes) {
        if (bitNum == 0) {
            assertEmptyCorrectness(bitVector);
        } else {
            Assert.assertEquals(type, bitVector.getType());
            Assert.assertEquals(bitNum, bitVector.bitNum());
            Assert.assertEquals(expectBytes.length, bitVector.byteNum());
            Assert.assertArrayEquals(expectBytes, bitVector.getBytes());
        }
    }
}
