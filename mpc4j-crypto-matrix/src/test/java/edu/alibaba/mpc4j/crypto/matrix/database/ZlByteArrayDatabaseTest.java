package edu.alibaba.mpc4j.crypto.matrix.database;

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
public class ZlByteArrayDatabaseTest {
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
        ZlByteArrayDatabase zlByteArrayDatabase = ZlByteArrayDatabase.createRandom(bitLength, DEFAULT_VECTOR_LENGTH, SECURE_RANDOM);
        ZlByteArrayDatabase[] partitionZlByteArrayDatabases = zlByteArrayDatabase.partition(partitionBitLength);
        ZlByteArrayDatabase combinedZlByteArrayDatabase = ZlByteArrayDatabase.create(bitLength, partitionZlByteArrayDatabases);
        Assert.assertEquals(zlByteArrayDatabase, combinedZlByteArrayDatabase);
    }

    @Test
    public void testBitPartition() {
        for (int bitLength : BIT_LENGTH_ARRAY) {
            testBitPartition(bitLength);
        }
    }

    private void testBitPartition(int bitLength) {
        ZlByteArrayDatabase zlByteArrayDatabase = ZlByteArrayDatabase.createRandom(bitLength, DEFAULT_VECTOR_LENGTH, SECURE_RANDOM);
        BitVector[] partitionBitVectors = zlByteArrayDatabase.bitPartition(EnvType.STANDARD, true);
        ZlByteArrayDatabase combinedZlByteArrayDatabase = ZlByteArrayDatabase.create(EnvType.STANDARD, true, partitionBitVectors);
        Assert.assertEquals(zlByteArrayDatabase, combinedZlByteArrayDatabase);
    }
}
