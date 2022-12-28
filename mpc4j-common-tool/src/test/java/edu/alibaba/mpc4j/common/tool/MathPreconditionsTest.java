package edu.alibaba.mpc4j.common.tool;

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
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositive("x", BigInteger.ONE.negate())
        );
        // check 0 is not positive
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkPositive("x", 0));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkPositive("x", 0L));
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositive("x", BigInteger.ZERO)
        );
        // check 1 is positive
        MathPreconditions.checkPositive("x", 1);
        MathPreconditions.checkPositive("x", 1L);
        MathPreconditions.checkPositive("x", BigInteger.ONE);
    }

    @Test
    public void testCheckNonNegative() {
        // check -1 is not non-negative
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkNonNegative("x", -1));
        Assert.assertThrows(IllegalArgumentException.class, () -> MathPreconditions.checkNonNegative("x", -1L));
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkNonNegative("x", BigInteger.ONE.negate())
        );
        // check 0 is non-negative
        MathPreconditions.checkNonNegative("x", 1);
        MathPreconditions.checkNonNegative("x", 1L);
        MathPreconditions.checkNonNegative("x", BigInteger.ONE);
        // check 1 is positive
        MathPreconditions.checkNonNegative("x", 1);
        MathPreconditions.checkNonNegative("x", 1L);
        MathPreconditions.checkNonNegative("x", BigInteger.ONE);
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
            MathPreconditions.checkPositiveInRange("x", BigInteger.ZERO, BigInteger.ZERO)
        );
        // check 1 is not a valid max
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 1, 0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 1L, 0L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", BigInteger.ONE, BigInteger.ZERO)
        );
        // check -1 is not positive
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 2, -1)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 2L, -1L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", BigInteger.valueOf(2), BigInteger.ONE.negate())
        );
        // check 0 is not positive
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 2, 0)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 2L, 0L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", BigInteger.valueOf(2), BigInteger.ZERO)
        );
        // check 2 is not positive in range (0, 2)
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 2, 2)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 2L, 2L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", BigInteger.valueOf(2), BigInteger.valueOf(2))
        );
        // check 1 is positive in range (0, 2)
        MathPreconditions.checkPositiveInRange("x", 2, 1);
        MathPreconditions.checkPositiveInRange("x", 2L, 1L);
        MathPreconditions.checkPositiveInRange("x", BigInteger.valueOf(2), BigInteger.ONE);
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
            MathPreconditions.checkNonNegativeInRange("x", BigInteger.ZERO, BigInteger.ZERO)
        );
        // check 1 is a valid max
        MathPreconditions.checkNonNegativeInRange("x", 1, 0);
        MathPreconditions.checkNonNegativeInRange("x", 1L, 0L);
        MathPreconditions.checkNonNegativeInRange("x", BigInteger.ONE, BigInteger.ZERO);
        // check -1 is not non-negative
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkNonNegativeInRange("x", 1, -1)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkNonNegativeInRange("x", 1L, -1L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkNonNegativeInRange("x", BigInteger.ONE, BigInteger.ONE.negate())
        );
        // check 0 is non-negative in range [0, 1)
        MathPreconditions.checkNonNegativeInRange("x", 1, 0);
        MathPreconditions.checkNonNegativeInRange("x", 1L, 0L);
        MathPreconditions.checkNonNegativeInRange("x", BigInteger.ONE, BigInteger.ZERO);
        // check 1 is not non-negative in range [0, 1)
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 1, 1)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", 1L, 1L)
        );
        Assert.assertThrows(IllegalArgumentException.class, () ->
            MathPreconditions.checkPositiveInRange("x", BigInteger.ONE, BigInteger.ONE)
        );
        // check 1 is non-negative in range [0, 2)
        MathPreconditions.checkPositiveInRange("x", 2, 1);
        MathPreconditions.checkPositiveInRange("x", 2L, 1L);
        MathPreconditions.checkPositiveInRange("x", BigInteger.valueOf(2), BigInteger.ONE);
    }
}
