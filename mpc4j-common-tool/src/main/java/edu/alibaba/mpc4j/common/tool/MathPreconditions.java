/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package edu.alibaba.mpc4j.common.tool;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;

/**
 * A collection of preconditions for math functions. The implementation is from:
 * <p>
 * https://github.com/google/guava/blob/master/guava/src/com/google/common/math/MathPreconditions.java
 * </p>
 * We need to copy the source code since it is originally package-private.
 *
 * @author Louis Wasserman
 * @date 2022/12/28
 */
public class MathPreconditions {

    private MathPreconditions() {
        // empty
    }

    /**
     * Check x > 0.
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x <= 0.
     */
    @CanIgnoreReturnValue
    public static int checkPositive(String role, int x) {
        if (x <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    /**
     * Check x > 0.
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x <= 0.
     */
    @CanIgnoreReturnValue
    public static long checkPositive(String role, long x) {
        if (x <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    /**
     * Check x > 0.
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x <= 0.
     */
    @CanIgnoreReturnValue
    public static BigInteger checkPositive(String role, BigInteger x) {
        if (x.signum() <= 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > 0");
        }
        return x;
    }

    /**
     * Check x >= 0.
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x < 0.
     */
    @CanIgnoreReturnValue
    public static int checkNonNegative(String role, int x) {
        if (x < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    /**
     * Check x >= 0.
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x < 0.
     */
    @CanIgnoreReturnValue
    public static long checkNonNegative(String role, long x) {
        if (x < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    /**
     * Check x >= 0.
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @return the value x.
     * @throws IllegalArgumentException if x < 0.
     */
    @CanIgnoreReturnValue
    public static BigInteger checkNonNegative(String role, BigInteger x) {
        if (x.signum() < 0) {
            throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
        }
        return x;
    }

    /**
     * Check x > min.
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @param min the value min.
     * @return the value x.
     * @throws IllegalArgumentException if x < min.
     */
    @CanIgnoreReturnValue
    public static int checkGreaterThan(String role, int x, int min) {
        if (x <= min) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > " + min);
        }
        return x;
    }

    /**
     * Check x > min.
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @param min the value min.
     * @return the value x.
     * @throws IllegalArgumentException if x < min.
     */
    @CanIgnoreReturnValue
    public static long checkGreaterThan(String role, long x, long min) {
        if (x <= min) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > " + min);
        }
        return x;
    }

    /**
     * Check x > min.
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @param min the value min.
     * @return the value x.
     * @throws IllegalArgumentException if x < min.
     */
    @CanIgnoreReturnValue
    public static BigInteger checkGreaterThan(String role, BigInteger x, BigInteger min) {
        if (BigIntegerUtils.lessOrEqual(x, min)) {
            throw new IllegalArgumentException(role + " (" + x + ") must be > " + min);
        }
        return x;
    }

    /**
     * Check x ∈ (0, max).
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @param max the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ [0, max).
     */
    @CanIgnoreReturnValue
    public static int checkPositiveInRange(String role, int x, int max) {
        checkGreaterThan("max", max, 1);
        if (x <= 0 || x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range (0, " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ (0, max).
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @param max the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ [0, max).
     */
    @CanIgnoreReturnValue
    public static long checkPositiveInRange(String role, long x, long max) {
        checkGreaterThan("max", max, 1);
        if (x <= 0 || x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range (0, " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ (0, max).
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @param max the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ [0, max).
     */
    @CanIgnoreReturnValue
    public static BigInteger checkPositiveInRange(String role, BigInteger x, BigInteger max) {
        checkGreaterThan("max", max, BigInteger.ONE);
        if (x.signum() <= 0 || BigIntegerUtils.greaterOrEqual(x, max)) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range (0, " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ [0, max).
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @param max the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ [0, max).
     */
    @CanIgnoreReturnValue
    public static int checkNonNegativeInRange(String role, int x, int max) {
        checkPositive("max", max);
        if (x < 0 || x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [0, " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ [0, max).
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @param max the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ [0, max).
     */
    @CanIgnoreReturnValue
    public static long checkNonNegativeInRange(String role, long x, long max) {
        checkPositive("max", max);
        if (x < 0 || x >= max) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [0, " + max + ")");
        }
        return x;
    }

    /**
     * Check x ∈ [0, max).
     *
     * @param role the name of the value x.
     * @param x the value x.
     * @param max the value max.
     * @return the value x.
     * @throws IllegalArgumentException if max <= 0 or x ∉ [0, max).
     */
    @CanIgnoreReturnValue
    public static BigInteger checkNonNegativeInRange(String role, BigInteger x, BigInteger max) {
        checkPositive("max", max);
        if (x.signum() < 0 || BigIntegerUtils.greaterOrEqual(x, max)) {
            throw new IllegalArgumentException(role + " (" + x + ") must be in range [0, " + max + ")");
        }
        return x;
    }
}