package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * plain z2 vector test.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
public class PlainZ2VectorTest {
    /**
     * min bit num
     */
    private static final int MIN_BIT_NUM = 1;
    /**
     * max bit num
     */
    private static final int MAX_BIT_NUM = 128;
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testIllegalInputs() {
        // create vector with 0 length
        Assert.assertThrows(AssertionError.class, () -> PlainZ2Vector.create(0, new byte[0]));
        // create vector with mismatch bit num
        Assert.assertThrows(AssertionError.class, () -> PlainZ2Vector.create(1, new byte[] { (byte)0x0F, }));
        // create vector with mismatch byte num
        Assert.assertThrows(AssertionError.class, () -> PlainZ2Vector.create(1, new byte[2]));
        Assert.assertThrows(AssertionError.class, () -> PlainZ2Vector.create(9, new byte[1]));
    }

    @Test
    public void testOnesMerge() {
        for (int num1 = MIN_BIT_NUM; num1 < MAX_BIT_NUM; num1++) {
            for (int num2 = MIN_BIT_NUM; num2 < MAX_BIT_NUM; num2++) {
                testOnesMerge(num1, num2);
            }
        }
    }

    private void testOnesMerge(int num1, int num2) {
        PlainZ2Vector vector1 = PlainZ2Vector.createOnes(num1);
        PlainZ2Vector vector2 = PlainZ2Vector.createOnes(num2);
        // manually merge
        int expectMergeByteLength = CommonUtils.getByteLength(num1 + num2);
        byte[] expectMerge = new byte[expectMergeByteLength];
        Arrays.fill(expectMerge, (byte) 0xFF);
        BytesUtils.reduceByteArray(expectMerge, num1 + num2);
        // merge and verify
        vector1.merge(vector2);
        Assert.assertTrue(vector1.isPlain());
        Assert.assertEquals(num1 + num2, vector1.getNum());
        Assert.assertArrayEquals(expectMerge, vector1.getBitVector().getBytes());
    }

    @Test
    public void testRandomMerge() {
        for (int num1 = MIN_BIT_NUM; num1 < MAX_BIT_NUM; num1++) {
            for (int num2 = MIN_BIT_NUM; num2 < MAX_BIT_NUM; num2++) {
                testRandomMerge(num1, num2);
            }
        }
    }

    private void testRandomMerge(int num1, int num2) {
        int num1Offset = CommonUtils.getByteLength(num1) * Byte.SIZE - num1;
        int num2Offset = CommonUtils.getByteLength(num2) * Byte.SIZE - num2;
        PlainZ2Vector vector1 = PlainZ2Vector.createRandom(num1, SECURE_RANDOM);
        PlainZ2Vector vector2 = PlainZ2Vector.createRandom(num2, SECURE_RANDOM);
        // manually merge
        byte[] vector1Bytes = vector1.getBitVector().getBytes();
        byte[] vector2Bytes = vector2.getBitVector().getBytes();
        int expectMergeByteLength = CommonUtils.getByteLength(num1 + num2);
        int expectMergeOffset = expectMergeByteLength * Byte.SIZE - (num1 + num2);
        byte[] expectMerge = new byte[expectMergeByteLength];
        for (int index = 0; index < num1; index++) {
            if (BinaryUtils.getBoolean(vector1Bytes, num1Offset + index)) {
                BinaryUtils.setBoolean(expectMerge, expectMergeOffset + index, true);
            }
        }
        for (int index = 0; index < num2; index++) {
            if (BinaryUtils.getBoolean(vector2Bytes, num2Offset + index)) {
                BinaryUtils.setBoolean(expectMerge, expectMergeOffset + num1 + index, true);
            }
        }
        // merge and verify
        vector1.merge(vector2);
        Assert.assertEquals(num1 + num2, vector1.getNum());
        Assert.assertArrayEquals(expectMerge, vector1.getBitVector().getBytes());
    }
}
