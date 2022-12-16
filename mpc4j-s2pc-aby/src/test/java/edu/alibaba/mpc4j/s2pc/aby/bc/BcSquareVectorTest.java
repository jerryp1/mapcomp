package edu.alibaba.mpc4j.s2pc.aby.bc;

import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 布尔方括向量测试。
 *
 * @author Weiran Liu
 * @date 2022/12/13
 */
public class BcSquareVectorTest {
    /**
     * 最小比特长度
     */
    private static final int MIN_BIT_LENGTH = 1;
    /**
     * 最大比特长度
     */
    private static final int MAX_BIT_LENGTH = 128;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testIllegalInputs() {
        // create vector with 0 length
        Assert.assertThrows(AssertionError.class, () -> BcSquareVector.create(new byte[0], 0, true));
        Assert.assertThrows(AssertionError.class, () -> BcSquareVector.create(new byte[0], 0, false));
        // create vector with mismatch bit length
        Assert.assertThrows(AssertionError.class, () -> BcSquareVector.create(new byte[] { (byte)0x0F, }, 1, true));
        Assert.assertThrows(AssertionError.class, () -> BcSquareVector.create(new byte[] { (byte)0x0F, }, 1, false));
        // create vector with mismatch byte length
        Assert.assertThrows(AssertionError.class, () -> BcSquareVector.create(new byte[2], 1, true));
        Assert.assertThrows(AssertionError.class, () -> BcSquareVector.create(new byte[2], 1, false));
        Assert.assertThrows(AssertionError.class, () -> BcSquareVector.create(new byte[1], 9, true));
        Assert.assertThrows(AssertionError.class, () -> BcSquareVector.create(new byte[1], 9, false));
        // merge vector with mismatch byte length
        Assert.assertThrows(AssertionError.class, () -> {
            BcSquareVector vector0 = BcSquareVector.create(new byte[] {(byte) 0x0F, }, 4, true);
            BcSquareVector vector1 = BcSquareVector.create(new byte[] {(byte) 0x0F, }, 4, false);
            vector0.merge(vector1);
        });
    }

    @Test
    public void testOnesMerge() {
        for (int num1 = MIN_BIT_LENGTH; num1 < MAX_BIT_LENGTH; num1++) {
            for (int num2 = MIN_BIT_LENGTH; num2 < MAX_BIT_LENGTH; num2++) {
                testOnesMerge(num1, num2);
            }
        }
    }

    private void testOnesMerge(int num1, int num2) {
        BcSquareVector vector1 = BcSquareVector.createOnes(num1);
        BcSquareVector vector2 = BcSquareVector.createOnes(num2);
        // manually merge
        int expectMergeByteLength = CommonUtils.getByteLength(num1 + num2);
        byte[] expectMerge = new byte[expectMergeByteLength];
        Arrays.fill(expectMerge, (byte) 0xFF);
        BytesUtils.reduceByteArray(expectMerge, num1 + num2);
        // merge and verify
        vector1.merge(vector2);
        Assert.assertTrue(vector1.isPlain());
        Assert.assertEquals(num1 + num2, vector1.bitLength());
        Assert.assertArrayEquals(expectMerge, vector1.getBytes());
    }

    @Test
    public void testRandomMerge() {
        for (int num1 = MIN_BIT_LENGTH; num1 < MAX_BIT_LENGTH; num1++) {
            for (int num2 = MIN_BIT_LENGTH; num2 < MAX_BIT_LENGTH; num2++) {
                testRandomMerge(num1, num2);
            }
        }
    }

    private void testRandomMerge(int num1, int num2) {
        int num1Offset = CommonUtils.getByteLength(num1) * Byte.SIZE - num1;
        int num2Offset = CommonUtils.getByteLength(num2) * Byte.SIZE - num2;
        BcSquareVector vector1 = BcSquareVector.createRandom(num1, SECURE_RANDOM);
        BcSquareVector vector2 = BcSquareVector.createRandom(num2, SECURE_RANDOM);
        // manually merge
        byte[] vector1Bytes = vector1.getBytes();
        byte[] vector2Bytes = vector2.getBytes();
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
        Assert.assertEquals(num1 + num2, vector1.bitLength());
        Assert.assertArrayEquals(expectMerge, vector1.getBytes());
    }
}
