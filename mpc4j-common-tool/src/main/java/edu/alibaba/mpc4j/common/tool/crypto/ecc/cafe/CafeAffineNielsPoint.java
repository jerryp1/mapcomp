/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * A pre-computed point on the affine model of the curve, represented as $(y+x, y-x, 2dxy)$ in "Niels coordinates".
 * Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/AffineNielsPoint.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/9
 */
class CafeAffineNielsPoint {
    /**
     * 单位元
     */
    private static final CafeAffineNielsPoint IDENTITY = new CafeAffineNielsPoint(
        CafeFieldElement.ONE_INTS, CafeFieldElement.ONE_INTS, CafeFieldElement.ZERO_INTS
    );
    /**
     * 查找表大小
     */
    private static final int LOOKUP_TABLE_SIZE = 8;
    /**
     * y + x
     */
    final CafeFieldElement yAddX;
    /**
     * y - x
     */
    final CafeFieldElement ySubX;
    /**
     * x * y * 2d
     */
    final CafeFieldElement xy2d;

    CafeAffineNielsPoint(CafeFieldElement yAddX, CafeFieldElement ySubX, CafeFieldElement xy2d) {
        this.yAddX = yAddX;
        this.ySubX = ySubX;
        this.xy2d = xy2d;
    }

    /**
     * Constant-time selection between two AffineNielsPoints.
     *
     * @param b the other point.
     * @param c must be 0 or 1, otherwise results are undefined.
     * @return a copy of this if $c == 0$, or a copy of b if $c == 1$.
     */
    public CafeAffineNielsPoint cmov(CafeAffineNielsPoint b, int c) {
        return new CafeAffineNielsPoint(yAddX.cmov(b.yAddX, c), ySubX.cmov(b.ySubX, c), xy2d.cmov(b.xy2d, c));
    }

    /**
     * Point negation.
     *
     * @return $-P$
     */
    public CafeAffineNielsPoint neg() {
        return new CafeAffineNielsPoint(ySubX, yAddX, xy2d.neg());
    }

    /**
     * Construct a lookup table of $[P, [2]P, [3]P, [4]P, [5]P, [6]P, [7]P, [8]P]$.
     *
     * @param point the point to calculate multiples for.
     * @return the lookup table.
     */
    static LookupTable buildLookupTable(CafeEdwardsPoint point) {
        CafeAffineNielsPoint[] points = new CafeAffineNielsPoint[LOOKUP_TABLE_SIZE];
        points[0] = point.toAffineNiels();
        for (int i = 1; i < LOOKUP_TABLE_SIZE; i++) {
            points[i] = point.add(points[i - 1]).toExtended().toAffineNiels();
        }
        return new LookupTable(points);
    }

    static class LookupTable {
        /**
         * 预计算点$[P, [2]P, [3]P, [4]P, [5]P, [6]P, [7]P, [8]P]$
         */
        private final CafeAffineNielsPoint[] table;

        LookupTable(CafeAffineNielsPoint[] table) {
            this.table = table;
        }

        /**
         * Given $-8 \leq x \leq 8$, return $[x]P$ in constant time.
         *
         * @param x the index.
         * @return the pre-computed point.
         */
        CafeAffineNielsPoint select(final int x) {
            if (x < -LOOKUP_TABLE_SIZE || x > LOOKUP_TABLE_SIZE) {
                throw new IllegalArgumentException(
                    "x must be in range ]" + -LOOKUP_TABLE_SIZE + ", " + LOOKUP_TABLE_SIZE + "]: " + x
                );
            }
            // Is x negative?
            final int xNegative = CafeConstantTimeUtils.isNeg(x);
            // |x|
            final int xabs = x - (((-xNegative) & x) << 1);

            // |x| P
            CafeAffineNielsPoint t = CafeAffineNielsPoint.IDENTITY;
            for (int i = 1; i < LOOKUP_TABLE_SIZE + 1; i++) {
                t = t.cmov(table[i - 1], CafeConstantTimeUtils.equal(xabs, i));
            }

            // -|x| P
            final CafeAffineNielsPoint tNeg = t.neg();
            // [x]P
            return t.cmov(tNeg, xNegative);
        }
    }

    /**
     * Construct a lookup table of $[P, [3]P, [5]P, [7]P, [9]P, [11]P, [13]P, [15]P]$.
     *
     * @param point the point to calculate multiples for.
     * @return the lookup table.
     */
    static NafLookupTable buildNafLookupTable(CafeEdwardsPoint point) {
        CafeAffineNielsPoint[] points = new CafeAffineNielsPoint[8];
        points[0] = point.toAffineNiels();
        CafeEdwardsPoint doublePoint = point.dbl();
        for (int i = 0; i < LOOKUP_TABLE_SIZE - 1; i++) {
            points[i + 1] = doublePoint.add(points[i]).toExtended().toAffineNiels();
        }
        return new NafLookupTable(points);
    }

    static class NafLookupTable {
        private final CafeAffineNielsPoint[] table;

        NafLookupTable(CafeAffineNielsPoint[] table) {
            this.table = table;
        }

        /**
         * Given public, odd $x$ with $0 \lt x \lt 2^4$, return $[x]A$.
         *
         * @param x the index.
         * @return the pre-computed point.
         */
        CafeAffineNielsPoint select(final int x) {
            if ((x % 2 == 0) || x >= LOOKUP_TABLE_SIZE * 2) {
                throw new IllegalArgumentException("invalid x");
            }

            return this.table[x / 2];
        }
    }
}
