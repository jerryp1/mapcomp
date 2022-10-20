/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * Represents an scalar element in ℤ/lℤ as 9 29-bit limbs. Modified from:
 * <p>
 * https://github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/UnpackedScalar.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/10/20
 */
class CafeUnpackScalar {
    /**
     * byte[]幂指数长度
     */
    public static final int BYTES_SCALAR_SIZE = 32;
    /**
     * int[]幂指数长度
     */
    public static final int INTS_SCALAR_SIZE = 9;
    /**
     * 2^29 - 1
     */
    static final int MASK_29_BITS = (1 << 29) - 1;
    /**
     * 2^24 - 1
     */
    static final int MASK_24_BITS = (1 << 24) - 1;
    /**
     * 0的int[]表示
     */
    static final int[] ZERO_INTS_SCALAR = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};

    /**
     * Unpack a 32 byte / 256 bit scalar into 9 29-bit limbs.
     *
     * @param in The 32-byte representation.
     * @return The element in its 29-bit limbs.
     */
    public static int[] decode(final byte[] in) {
        int[] words = new int[8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 4; j++) {
                words[i] |= ((in[(i * 4) + j]) & 0xff) << (j * 8);
            }
        }

        int[] s = new int[INTS_SCALAR_SIZE];
        s[0] = (words[0] & MASK_29_BITS);
        s[1] = (((words[0] >>> 29) | (words[1] << 3)) & MASK_29_BITS);
        s[2] = (((words[1] >>> 26) | (words[2] << 6)) & MASK_29_BITS);
        s[3] = (((words[2] >>> 23) | (words[3] << 9)) & MASK_29_BITS);
        s[4] = (((words[3] >>> 20) | (words[4] << 12)) & MASK_29_BITS);
        s[5] = (((words[4] >>> 17) | (words[5] << 15)) & MASK_29_BITS);
        s[6] = (((words[5] >>> 14) | (words[6] << 18)) & MASK_29_BITS);
        s[7] = (((words[6] >>> 11) | (words[7] << 21)) & MASK_29_BITS);
        s[8] = ((words[7] >>> 8) & MASK_24_BITS);

        return s;
    }

    /**
     * Pack the limbs of this UnpackedScalar into 32 bytes.
     *
     * @param s The element in its 29-bit limbs.
     * @return The 32-byte representation.
     */
    public static byte[] encode(final int[] s) {
        byte[] out = new byte[BYTES_SCALAR_SIZE];
        // All limbs are 29 bits, but let's use the unsigned right shift anyway.
        out[0] = (byte) (s[0]);
        out[1] = (byte) (s[0] >>> 8);
        out[2] = (byte) (s[0] >>> 16);
        out[3] = (byte) ((s[0] >>> 24) | (s[1] << 5));
        out[4] = (byte) (s[1] >>> 3);
        out[5] = (byte) (s[1] >>> 11);
        out[6] = (byte) (s[1] >>> 19);
        out[7] = (byte) ((s[1] >>> 27) | (s[2] << 2));
        out[8] = (byte) (s[2] >>> 6);
        out[9] = (byte) (s[2] >>> 14);
        out[10] = (byte) ((s[2] >>> 22) | (s[3] << 7));
        out[11] = (byte) (s[3] >>> 1);
        out[12] = (byte) (s[3] >>> 9);
        out[13] = (byte) (s[3] >>> 17);
        out[14] = (byte) ((s[3] >>> 25) | (s[4] << 4));
        out[15] = (byte) (s[4] >>> 4);
        out[16] = (byte) (s[4] >>> 12);
        out[17] = (byte) (s[4] >>> 20);
        out[18] = (byte) ((s[4] >>> 28) | (s[5] << 1));
        out[19] = (byte) (s[5] >>> 7);
        out[20] = (byte) (s[5] >>> 15);
        out[21] = (byte) ((s[5] >>> 23) | (s[6] << 6));
        out[22] = (byte) (s[6] >>> 2);
        out[23] = (byte) (s[6] >>> 10);
        out[24] = (byte) (s[6] >>> 18);
        out[25] = (byte) ((s[6] >>> 26) | (s[7] << 3));
        out[26] = (byte) (s[7] >>> 5);
        out[27] = (byte) (s[7] >>> 13);
        out[28] = (byte) (s[7] >>> 21);
        out[29] = (byte) (s[8]);
        out[30] = (byte) (s[8] >>> 8);
        out[31] = (byte) (s[8] >>> 16);

        return out;
    }

    /**
     * Compute $a + b \bmod \ell$.
     *
     * @param a the scalar.
     * @param b the scalar to add to this.
     * @return $a + b \bmod \ell$
     */
    public static int[] add(final int[] a, final int[] b) {
        int[] sum = new int[INTS_SCALAR_SIZE];

        int carry = 0;
        for (int i = 0; i < 9; i++) {
            carry = a[i] + b[i] + (carry >> 29);
            sum[i] = carry & MASK_29_BITS;
        }
        // Subtract l if the sum is >= l
        return subtract(sum, CafeConstants.L_INTS_SCALAR);
    }

    /**
     * Compute $a - b \bmod \ell$.
     *
     * @param a the scalar.
     * @param b the scalar to subtract from this.
     * @return $a - b \bmod \ell$
     */
    public static int[] subtract(final int[] a, final int[] b) {
        int[] difference = new int[INTS_SCALAR_SIZE];

        int borrow = 0;
        for (int i = 0; i < 9; i++) {
            borrow = a[i] - (b[i] + (borrow >>> 31));
            difference[i] = borrow & MASK_29_BITS;
        }
        // Conditionally add l if the difference is negative
        int underflowMask = ((borrow >>> 31) ^ 1) - 1;
        int carry = 0;
        for (int i = 0; i < 9; i++) {
            carry = (carry >>> 29) + difference[i] + (CafeConstants.L_INTS_SCALAR[i] & underflowMask);
            difference[i] = carry & MASK_29_BITS;
        }

        return difference;
    }

    private static long m(final int a, final int b) {
        return ((long) a) * ((long) b);
    }

    /**
     * Compute $a * b \bmod \ell$.
     *
     * @param a the scalar.
     * @param b the Scalar to multiply with this.
     * @return the unreduced limbs.
     */
    private static long[] mulInternal(final int[] a, final int[] b) {
        long[] z = new long[17];
        // c00
        z[0] = m(a[0], b[0]);
        // c01
        z[1] = m(a[0], b[1]) + m(a[1], b[0]);
        // c02
        z[2] = m(a[0], b[2]) + m(a[1], b[1]) + m(a[2], b[0]);
        // c03
        z[3] = m(a[0], b[3]) + m(a[1], b[2]) + m(a[2], b[1]) + m(a[3], b[0]);
        // c04
        z[4] = m(a[0], b[4]) + m(a[1], b[3]) + m(a[2], b[2]) + m(a[3], b[1]) + m(a[4], b[0]);
        // c05
        z[5] = m(a[1], b[4]) + m(a[2], b[3]) + m(a[3], b[2]) + m(a[4], b[1]);
        // 06
        z[6] = m(a[2], b[4]) + m(a[3], b[3]) + m(a[4], b[2]);
        // c07
        z[7] = m(a[3], b[4]) + m(a[4], b[3]);
        // c08 - c03
        z[8] = (m(a[4], b[4])) - z[3];
        // c05mc10
        z[10] = z[5] - (m(a[5], b[5]));
        // c06mc11
        z[11] = z[6] - (m(a[5], b[6]) + m(a[6], b[5]));
        // c07mc12
        z[12] = z[7] - (m(a[5], b[7]) + m(a[6], b[6]) + m(a[7], b[5]));
        // c13
        z[13] = m(a[5], b[8]) + m(a[6], b[7]) + m(a[7], b[6]) + m(a[8], b[5]);
        // c14
        z[14] = m(a[6], b[8]) + m(a[7], b[7]) + m(a[8], b[6]);
        // c15
        z[15] = m(a[7], b[8]) + m(a[8], b[7]);
        // c16
        z[16] = m(a[8], b[8]);

        // c05mc10 - c00
        z[5] = z[10] - (z[0]);
        // c06mc11 - c01
        z[6] = z[11] - (z[1]);
        // c07mc12 - c02
        z[7] = z[12] - (z[2]);
        // c08mc13 - c03
        z[8] = z[8] - (z[13]);
        // c14 + c04
        z[9] = z[14] + (z[4]);
        // c15 + c05mc10
        z[10] = z[15] + (z[10]);
        // c16 + c06mc11
        z[11] = z[16] + (z[11]);

        int aa0 = a[0] + a[5];
        int aa1 = a[1] + a[6];
        int aa2 = a[2] + a[7];
        int aa3 = a[3] + a[8];

        int bb0 = b[0] + b[5];
        int bb1 = b[1] + b[6];
        int bb2 = b[2] + b[7];
        int bb3 = b[3] + b[8];

        // c20 + c05mc10 - c00
        z[5] = (m(aa0, bb0)) + z[5];
        // c21 + c06mc11 - c01
        z[6] = (m(aa0, bb1) + m(aa1, bb0)) + z[6];
        // c22 + c07mc12 - c02
        z[7] = (m(aa0, bb2) + m(aa1, bb1) + m(aa2, bb0)) + z[7];
        // c23 + c08mc13 - c03
        z[8] = (m(aa0, bb3) + m(aa1, bb2) + m(aa2, bb1) + m(aa3, bb0)) + z[8];
        // c24 - c14 - c04
        z[9] = (m(aa0, b[4]) + m(aa1, bb3) + m(aa2, bb2) + m(aa3, bb1) + m(a[4], bb0)) - z[9];
        // c25 - c15 - c05mc10
        z[10] = (m(aa1, b[4]) + m(aa2, bb3) + m(aa3, bb2) + m(a[4], bb1)) - z[10];
        // c26 - c16 - c06mc11
        z[11] = (m(aa2, b[4]) + m(aa3, bb3) + m(a[4], bb2)) - z[11];
        // c27 - c07mc12
        z[12] = (m(aa3, b[4]) + m(a[4], bb3)) - z[12];

        return z;
    }

    /**
     * Compute $\text{limbs}/R \bmod \ell$, where R is the Montgomery modulus 2^261.
     *
     * @param limbs the value to reduce.
     * @return $\text{limbs}/R \bmod \ell$
     */
    @SuppressWarnings("PointlessBitwiseExpression")
    public static int[] montgomeryReduce(final long[] limbs) {
        // Note: l5,l6,l7 are zero, so their multiplies can be skipped
        int[] l = CafeConstants.L_INTS_SCALAR;
        long sum, carry;
        int n0, n1, n2, n3, n4, n5, n6, n7, n8;
        int[] r = new int[INTS_SCALAR_SIZE];
        // The first half computes the Montgomery adjustment factor n, and begins adding
        // n*l to make limbs divisible by R
        sum = (limbs[0]);
        n0 = (int) (((sum & 0xffffffff) * CafeConstants.L_FACTOR) & MASK_29_BITS);
        carry = (sum + m(n0, l[0])) >>> 29;
        sum = (carry + limbs[1] + m(n0, l[1]));
        n1 = (int) (((sum & 0xffffffff) * CafeConstants.L_FACTOR) & MASK_29_BITS);
        carry = (sum + m(n1, l[0])) >>> 29;
        sum = (carry + limbs[2] + m(n0, l[2]) + m(n1, l[1]));
        n2 = (int) (((sum & 0xffffffff) * CafeConstants.L_FACTOR) & MASK_29_BITS);
        carry = (sum + m(n2, l[0])) >>> 29;
        sum = (carry + limbs[3] + m(n0, l[3]) + m(n1, l[2]) + m(n2, l[1]));
        n3 = (int) (((sum & 0xffffffff) * CafeConstants.L_FACTOR) & MASK_29_BITS);
        carry = (sum + m(n3, l[0])) >>> 29;
        sum = (carry + limbs[4] + m(n0, l[4]) + m(n1, l[3]) + m(n2, l[2]) + m(n3, l[1]));
        n4 = (int) (((sum & 0xffffffff) * CafeConstants.L_FACTOR) & MASK_29_BITS);
        carry = (sum + m(n4, l[0])) >>> 29;
        sum = (carry + limbs[5] + m(n1, l[4]) + m(n2, l[3]) + m(n3, l[2]) + m(n4, l[1]));
        n5 = (int) (((sum & 0xffffffff) * CafeConstants.L_FACTOR) & MASK_29_BITS);
        carry = (sum + m(n5, l[0])) >>> 29;
        sum = (carry + limbs[6] + m(n2, l[4]) + m(n3, l[3]) + m(n4, l[2]) + m(n5, l[1]));
        n6 = (int) (((sum & 0xffffffff) * CafeConstants.L_FACTOR) & MASK_29_BITS);
        carry = (sum + m(n6, l[0])) >>> 29;
        sum = (carry + limbs[7] + m(n3, l[4]) + m(n4, l[3]) + m(n5, l[2]) + m(n6, l[1]));
        n7 = (int) (((sum & 0xffffffff) * CafeConstants.L_FACTOR) & MASK_29_BITS);
        carry = (sum + m(n7, l[0])) >>> 29;
        sum = (carry + limbs[8] + m(n0, l[8]) + m(n4, l[4]) + m(n5, l[3]) + m(n6, l[2]) + m(n7, l[1]));
        n8 = (int) (((sum & 0xffffffff) * CafeConstants.L_FACTOR) & MASK_29_BITS);
        carry = (sum + m(n8, l[0])) >>> 29;

        // limbs is divisible by R now, so we can divide by R by simply storing the upper half as the result
        sum = (carry + limbs[9] + m(n1, l[8]) + m(n5, l[4]) + m(n6, l[3]) + m(n7, l[2]) + m(n8, l[1]));
        r[0] = (int) (sum & MASK_29_BITS);
        carry = sum >>> 29;
        sum = (carry + limbs[10] + m(n2, l[8]) + m(n6, l[4]) + m(n7, l[3]) + m(n8, l[2]));
        r[1] = (int) (sum & MASK_29_BITS);
        carry = sum >>> 29;
        sum = (carry + limbs[11] + m(n3, l[8]) + m(n7, l[4]) + m(n8, l[3]));
        r[2] = (int) (sum & MASK_29_BITS);
        carry = sum >>> 29;
        sum = (carry + limbs[12] + m(n4, l[8]) + m(n8, l[4]));
        r[3] = (int) (sum & MASK_29_BITS);
        carry = sum >>> 29;
        sum = (carry + limbs[13] + m(n5, l[8]));
        r[4] = (int) (sum & MASK_29_BITS);
        carry = sum >>> 29;
        sum = (carry + limbs[14] + m(n6, l[8]));
        r[5] = (int) (sum & MASK_29_BITS);
        carry = sum >>> 29;
        sum = (carry + limbs[15] + m(n7, l[8]));
        r[6] = (int) (sum & MASK_29_BITS);
        carry = sum >>> 29;
        sum = (carry + limbs[16] + m(n8, l[8]));
        r[7] = (int) (sum & MASK_29_BITS);
        carry = sum >>> 29;
        r[8] = (int) (carry & 0xffffffff);
        // Result may be >= l, so attempt to subtract l
        return subtract(r, CafeConstants.L_INTS_SCALAR);
    }

    /**
     * Compute $a * b \bmod \ell$.
     *
     * @param a the scalar.
     * @param b the scalar to multiply with this.
     * @return $a * b \bmod \ell$.
     */
    public static int[] multiply(final int[] a, final int[] b) {
        int[] ab = montgomeryReduce(mulInternal(a, b));
        return montgomeryReduce(mulInternal(ab, CafeConstants.RR));
    }
}
