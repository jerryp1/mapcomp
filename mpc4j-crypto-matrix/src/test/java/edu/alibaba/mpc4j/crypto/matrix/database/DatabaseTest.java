package edu.alibaba.mpc4j.crypto.matrix.database;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.crypto.matrix.database.DatabaseFactory.DatabaseType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * database test.
 *
 * @author Weiran Liu
 * @date 2023/3/31
 */
@RunWith(Parameterized.class)
public class DatabaseTest {
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

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // naive database
        configurations.add(new Object[]{DatabaseType.NAIVE.name(), DatabaseType.NAIVE});
        // Zl database
        configurations.add(new Object[]{DatabaseType.ZL.name(), DatabaseType.ZL});
        // Zl64 database
        configurations.add(new Object[]{DatabaseType.ZL64.name(), DatabaseType.ZL64});

        return configurations;
    }

    /**
     * the type
     */
    private final DatabaseType type;
    /**
     * max supported l
     */
    private final int maxL;

    public DatabaseTest(String name, DatabaseType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        maxL = DatabaseFactory.maxL(type);
    }

    @Test
    public void testBitPartition() {
        for (int l : L_ARRAY) {
            if (l <= maxL) {
                testBitPartition(l);
            }
        }
    }

    private void testBitPartition(int l) {
        Database database = DatabaseFactory.createRandom(type, l, DEFAULT_ROWS, SECURE_RANDOM);
        BitVector[] bitVectors = database.bitPartition(EnvType.STANDARD, true);
        Database combinedDatabase = DatabaseFactory.create(type, EnvType.STANDARD, true, bitVectors);
        Assert.assertEquals(database, combinedDatabase);
    }
}
