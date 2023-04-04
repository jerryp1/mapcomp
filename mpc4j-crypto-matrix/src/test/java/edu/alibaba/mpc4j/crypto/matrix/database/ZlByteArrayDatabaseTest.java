package edu.alibaba.mpc4j.crypto.matrix.database;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * Zl byte array database test.
 *
 * @author Weiran Liu
 * @date 2023/3/31
 */
public class ZlByteArrayDatabaseTest {
    /**
     * default vector length
     */
    private static final int DEFAULT_ROWS = 1 << 16;
    /**
     * l array
     */
    private static final int[] L_ARRAY = new int[]{
        1, 5, 7, 9, 15, 16, 17, LongUtils.MAX_L - 1, LongUtils.MAX_L, Long.SIZE, CommonConstants.BLOCK_BIT_LENGTH,
    };
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testPartition() {
        for (int l : L_ARRAY) {
            for (int partitionL : L_ARRAY) {
                testPartition(l, partitionL);
            }
        }
    }

    private void testPartition(int bitLength, int partitionBitLength) {
        ZlByteArrayDatabase database = ZlByteArrayDatabase.createRandom(bitLength, DEFAULT_ROWS, SECURE_RANDOM);
        ZlByteArrayDatabase[] partitionDatabases = database.partition(partitionBitLength);
        ZlByteArrayDatabase combinedDatabase = ZlByteArrayDatabase.create(bitLength, partitionDatabases);
        Assert.assertEquals(database, combinedDatabase);
    }

    @Test
    public void testBitPartition() {
        for (int l : L_ARRAY) {
            testBitPartition(l);
        }
    }

    private void testBitPartition(int bitLength) {
        ZlByteArrayDatabase database = ZlByteArrayDatabase.createRandom(bitLength, DEFAULT_ROWS, SECURE_RANDOM);
        BitVector[] bitVectors = database.partition(EnvType.STANDARD, true);
        ZlByteArrayDatabase combinedDatabase = ZlByteArrayDatabase.create(EnvType.STANDARD, true, bitVectors);
        Assert.assertEquals(database, combinedDatabase);
    }
}
