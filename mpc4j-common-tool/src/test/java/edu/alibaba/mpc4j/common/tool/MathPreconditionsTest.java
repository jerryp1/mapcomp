package edu.alibaba.mpc4j.common.tool;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * tests for preconditions for math functions.
 *
 * @author Weiran Liu
 * @date 2022/12/28
 */
public class MathPreconditionsTest {

    @Test
    public void testCheckPositive() {
        // check -1 is not positive
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkPositive("x", -1));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkPositive("x", -1L));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkPositive("x", -1.0));
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositive("x", BigInteger.ONE.negate())
        );
        // check 0 is not positive
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkPositive("x", 0));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkPositive("x", 0L));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkPositive("x", 0.0));
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositive("x", BigInteger.ZERO)
        );
        // check 1 is positive
        MathPreconditions.checkPositive("x", 1);
        MathPreconditions.checkPositive("x", 1L);
        MathPreconditions.checkPositive("x", 1.0);
        MathPreconditions.checkPositive("x", BigInteger.ONE);
    }

    @Test
    public void testCheckNonNegative() {
        // check -1 is not non-negative
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkNonNegative("x", -1));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkNonNegative("x", -1L));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkNonNegative("x", -1.0));
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkNonNegative("x", BigInteger.ONE.negate())
        );
        // check 0 is non-negative
        MathPreconditions.checkNonNegative("x", 1);
        MathPreconditions.checkNonNegative("x", 1L);
        MathPreconditions.checkNonNegative("x", 0.0);
        MathPreconditions.checkNonNegative("x", BigInteger.ONE);
        // check 1 is positive
        MathPreconditions.checkNonNegative("x", 1);
        MathPreconditions.checkNonNegative("x", 1L);
        MathPreconditions.checkNonNegative("x", 1.0);
        MathPreconditions.checkNonNegative("x", BigInteger.ONE);
    }

    @Test
    public void testCheckEqual() {
        // check -1 is not equal to 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkEqual("x", "y", -1, 0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkEqual("x", "y", -1L, 0L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkEqual("x", "y", -1.0, 0.0, DoubleUtils.PRECISION)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkEqual("x", "y", BigInteger.ONE.negate(), BigInteger.ZERO)
        );
        // check -1 is equal to -1
        MathPreconditions.checkEqual("x", "y", -1, -1);
        MathPreconditions.checkEqual("x", "y", -1L, -1L);
        MathPreconditions.checkEqual("x", "y", -1.0, -1.0, DoubleUtils.PRECISION);
        MathPreconditions.checkEqual("x", "y", BigInteger.ONE.negate(), BigInteger.ONE.negate());
        // check 2 is equal to 2
        MathPreconditions.checkEqual("x", "y", 2, 2);
        MathPreconditions.checkEqual("x", "y", 2L, 2L);
        MathPreconditions.checkEqual("x", "y", 2.0, 2.0, DoubleUtils.PRECISION);
        // use two ways of creating BigInteger
        MathPreconditions.checkEqual("x", "y", BigInteger.ONE.add(BigInteger.ONE), BigInteger.valueOf(2));
    }

    @Test
    public void testCheckGreater() {
        // check -1 is not greater than -1
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkGreater("x", -1, -1));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkGreater("x", -1L, -1L));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkGreater("x", -1.0, -1.0));
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkGreater("x", BigInteger.ONE.negate(), BigInteger.ONE.negate())
        );
        // check -1 is not greater than 0
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkGreater("x", -1, 0));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkGreater("x", -1L, 0L));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkGreater("x", -1.0, 0.0));
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkGreater("x", BigInteger.ONE.negate(), BigInteger.ZERO)
        );
        // check 1 is greater than 0
        MathPreconditions.checkGreater("x", 1, 0);
        MathPreconditions.checkGreater("x", 1L, 0L);
        MathPreconditions.checkGreater("x", 1.0, 0.0);
        MathPreconditions.checkGreater("x", BigInteger.ONE, BigInteger.ZERO);
    }

    @Test
    public void testCheckGreaterOrEqual() {
        // check -2 is not greater than or equal to -1
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkGreaterOrEqual("x", -2, -1)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkGreaterOrEqual("x", -2L, -1L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkGreaterOrEqual("x", -2.0, -1.0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkGreaterOrEqual("x", BigInteger.valueOf(2).negate(), BigInteger.ONE.negate())
        );
        // check -1 is greater than or equal to -1
        MathPreconditions.checkGreaterOrEqual("x", -1, -1);
        MathPreconditions.checkGreaterOrEqual("x", -1L, -1L);
        MathPreconditions.checkGreaterOrEqual("x", -1.0, -1.0);
        MathPreconditions.checkGreaterOrEqual("x", BigInteger.ONE.negate(), BigInteger.ONE.negate());
        // check 1 is greater than 0
        MathPreconditions.checkGreaterOrEqual("x", 1, 0);
        MathPreconditions.checkGreaterOrEqual("x", 1L, 0L);
        MathPreconditions.checkGreaterOrEqual("x", 1.0, 0.0);
        MathPreconditions.checkGreaterOrEqual("x", BigInteger.ONE, BigInteger.ZERO);
    }

    @Test
    public void testCheckLess() {
        // check -1 is not less than -1
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkLess("x", -1, -1));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkLess("x", -1L, -1L));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkLess("x", -1.0, -1.0));
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkLess("x", BigInteger.ONE.negate(), BigInteger.ONE.negate())
        );
        // check 1 is not less than 0
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkLess("x", 1, 0));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkLess("x", 1L, 0L));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkLess("x", 1.0, 0.0));
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkLess("x", BigInteger.ONE, BigInteger.ZERO)
        );
        // check -1 is less than 0
        MathPreconditions.checkLess("x", -1, 0);
        MathPreconditions.checkLess("x", -1L, 0L);
        MathPreconditions.checkLess("x", -1.0, 0.0);
        MathPreconditions.checkLess("x", BigInteger.ONE.negate(), BigInteger.ZERO);
    }

    @Test
    public void testCheckLessOrEqual() {
        // check -1 is less than or equal to -1
        MathPreconditions.checkLessOrEqual("x", -1, -1);
        MathPreconditions.checkLessOrEqual("x", -1L, -1L);
        MathPreconditions.checkLessOrEqual("x", -1.0, -1.0);
        MathPreconditions.checkLessOrEqual("x", BigInteger.ONE.negate(), BigInteger.ONE.negate());
        // check 1 is not less than 0
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkLessOrEqual("x", 1, 0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkLessOrEqual("x", 1L, 0L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkLessOrEqual("x", 1.0, 0.0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkLessOrEqual("x", BigInteger.ONE, BigInteger.ZERO)
        );
        // check -1 is less than 0
        MathPreconditions.checkLessOrEqual("x", -1, 0);
        MathPreconditions.checkLessOrEqual("x", -1L, 0L);
        MathPreconditions.checkLessOrEqual("x", -1.0, 0.0);
        MathPreconditions.checkLessOrEqual("x", BigInteger.ONE.negate(), BigInteger.ZERO);
    }

    @Test
    public void testCheckPositiveInRange() {
        // check 0 is not a valid max
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 0, 0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 0L, 0L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 0.0, 0.0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", BigInteger.ZERO, BigInteger.ZERO)
        );
        // check 1 is not a valid max
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 1, 1)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 1L, 1L)
        );
        // 1 is a valid max for double value
        MathPreconditions.checkPositiveInRange("x", 0.4, 1.0);
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", BigInteger.ONE, BigInteger.ONE)
        );
        // check -1 is not positive
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", -1, 2)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", -1L, 2L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", -1.0, 2.0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", BigInteger.ONE.negate(), BigInteger.valueOf(2))
        );
        // check 0 is not positive
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 0, 2)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 0L, 2L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 0.0, 2.0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", BigInteger.ZERO, BigInteger.valueOf(2))
        );
        // check 2 is not positive in range (0, 2)
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 2, 2)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 2L, 2L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 2.0, 2.0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", BigInteger.valueOf(2), BigInteger.valueOf(2))
        );
        // check 1 is positive in range (0, 2)
        MathPreconditions.checkPositiveInRange("x", 1, 2);
        MathPreconditions.checkPositiveInRange("x", 1L, 2L);
        MathPreconditions.checkPositiveInRange("x", 1.0, 2.0);
        MathPreconditions.checkPositiveInRange("x", BigInteger.ONE, BigInteger.valueOf(2));
    }

    @Test
    public void testCheckPositiveInRangeClosed() {
        // check 0 is not a valid max
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", 0, 0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", 0L, 0L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", 0.0, 0.0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", BigInteger.ZERO, BigInteger.ZERO)
        );
        // check 1 is a valid max
        MathPreconditions.checkPositiveInRangeClosed("x", 1, 1);
        MathPreconditions.checkPositiveInRangeClosed("x", 1L, 1L);
        MathPreconditions.checkPositiveInRangeClosed("x", 1.0, 1.0);
        MathPreconditions.checkPositiveInRangeClosed("x", BigInteger.ONE, BigInteger.ONE);
        // check -1 is not positive
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", -1, 2)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", -1L, 2L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", -1.0, 2.0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", BigInteger.ONE.negate(), BigInteger.valueOf(2))
        );
        // check 0 is not positive
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", 0, 2)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", 0L, 2L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", 0.0, 2.0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", BigInteger.ZERO, BigInteger.valueOf(2))
        );
        // check 2 is not positive in range (0, 1]
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", 2, 1)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", 2L, 1L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", 2.0, 1.0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRangeClosed("x", BigInteger.valueOf(2), BigInteger.ONE)
        );
        // check 1 is positive in range (0, 1]
        MathPreconditions.checkPositiveInRangeClosed("x", 1, 1);
        MathPreconditions.checkPositiveInRangeClosed("x", 1L, 1L);
        MathPreconditions.checkPositiveInRangeClosed("x", 1.0, 1.0);
        MathPreconditions.checkPositiveInRangeClosed("x", BigInteger.ONE, BigInteger.ONE);
    }

    @Test
    public void testCheckNonNegativeInRange() {
        // check 0 is not a valid max
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkNonNegativeInRange("x", 0, 0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkNonNegativeInRange("x", 0L, 0L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkNonNegativeInRange("x", 0.0, 0.0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkNonNegativeInRange("x", BigInteger.ZERO, BigInteger.ZERO)
        );
        // check 1 is a valid max
        MathPreconditions.checkNonNegativeInRange("x", 0, 1);
        MathPreconditions.checkNonNegativeInRange("x", 0L, 1L);
        MathPreconditions.checkNonNegativeInRange("x", 0.0, 1.0);
        MathPreconditions.checkNonNegativeInRange("x", BigInteger.ZERO, BigInteger.ONE);
        // check -1 is not non-negative
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkNonNegativeInRange("x", -1, 1)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkNonNegativeInRange("x", -1L, 1L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkNonNegativeInRange("x", -1.0, 1.0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkNonNegativeInRange("x", BigInteger.ONE.negate(), BigInteger.ONE)
        );
        // check 0 is non-negative in range [0, 1)
        MathPreconditions.checkNonNegativeInRange("x", 0, 1);
        MathPreconditions.checkNonNegativeInRange("x", 0L, 1L);
        MathPreconditions.checkNonNegativeInRange("x", 0.0, 1.0);
        MathPreconditions.checkNonNegativeInRange("x", BigInteger.ZERO, BigInteger.ONE);
        // check 1 is not non-negative in range [0, 1)
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 1, 1)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 1L, 1L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 1.0, 1.0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", BigInteger.ONE, BigInteger.ONE)
        );
        // check 1 is non-negative in range [0, 2)
        MathPreconditions.checkPositiveInRange("x", 1, 2);
        MathPreconditions.checkPositiveInRange("x", 1L, 2L);
        MathPreconditions.checkPositiveInRange("x", 1.0, 2.0);
        MathPreconditions.checkPositiveInRange("x", BigInteger.ONE, BigInteger.valueOf(2));
    }
}
