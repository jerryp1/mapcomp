package edu.alibaba.mpc4j.crypto.matrix.database;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * Zl64 long database test.
 *
 * @author Weiran Liu
 * @date 2023/4/4
 */
public class Zl64LongDatabaseTest {
    /**
     * default rows
     */
    private static final int DEFAULT_ROWS = 1 << 16;
    /**
     * l array
     */
    private static final int[] L_ARRAY = new int[]{1, 5, 7, 9, 15, 16, 17, LongUtils.MAX_L - 1, LongUtils.MAX_L};
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testPartition() {
        for (int l : L_ARRAY) {
            testPartition(l);
        }
    }

    private void testPartition(int l) {
        Zl64LongDatabase database = Zl64LongDatabase.createRandom(l, DEFAULT_ROWS, SECURE_RANDOM);
        BitVector[] bitVectors = database.partition(EnvType.STANDARD, true);
        Zl64LongDatabase combinedDatabase = Zl64LongDatabase.create(EnvType.STANDARD, true, bitVectors);
        Assert.assertEquals(database, combinedDatabase);
    }
}
