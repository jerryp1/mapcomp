package edu.alibaba.mpc4j.crypto.matrix.vector;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * bytes vector test.
 *
 * @author Weiran Liu
 * @date 2023/3/31
 */
public class BytesVectorTest {
    /**
     * default vector length
     */
    private static final int DEFAULT_VECTOR_LENGTH = 1 << 16;
    /**
     * partition bit length array
     */
    private static final int[] BIT_LENGTH_ARRAY = new int[] {1, 5, 7, 9, 15, 16, 17};
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testPartition() {
        for (int bitLength : BIT_LENGTH_ARRAY) {
            for (int partitionBitLength : BIT_LENGTH_ARRAY) {
                testPartition(bitLength, partitionBitLength);
            }
        }
    }

    private void testPartition(int bitLength, int partitionBitLength) {
        BytesVector bytesVector = BytesVector.createRandom(bitLength, DEFAULT_VECTOR_LENGTH, SECURE_RANDOM);
        BytesVector[] partitionBytesVectors = bytesVector.partition(partitionBitLength);
        BytesVector combinedBytesVector = BytesVector.create(bitLength, partitionBytesVectors);
        Assert.assertEquals(bytesVector, combinedBytesVector);
    }

    @Test
    public void testBitPartition() {
        for (int bitLength : BIT_LENGTH_ARRAY) {
            testBitPartition(bitLength);
        }
    }

    private void testBitPartition(int bitLength) {
        BytesVector bytesVector = BytesVector.createRandom(bitLength, DEFAULT_VECTOR_LENGTH, SECURE_RANDOM);
        BitVector[] partitionBitVectors = bytesVector.bitPartition(EnvType.STANDARD, true);
        BytesVector combinedBytesVector = BytesVector.create(EnvType.STANDARD, true, partitionBitVectors);
        Assert.assertEquals(bytesVector, combinedBytesVector);
    }
}
