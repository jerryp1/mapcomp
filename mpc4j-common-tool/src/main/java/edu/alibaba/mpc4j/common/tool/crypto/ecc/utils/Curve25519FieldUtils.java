package edu.alibaba.mpc4j.common.tool.crypto.ecc.utils;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe.CafeConstantTimeUtils;

/**
 * Curve25519有限域工具类。
 *
 * @author Weiran Liu
 * @date 2022/11/13
 */
public class Curve25519FieldUtils {

    private Curve25519FieldUtils() {
        // empty
    }

    /**
     * field int size
     */
    public static final int INT_SIZE = 10;
    /**
     * field byte size
     */
    public static final int BYTE_SIZE = 32;
    /**
     * 0
     */
    public static final int[] ZERO_INTS = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0,};
    /**
     * 0 in byte array
     */
    private static final byte[] ZERO_BYTES = new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };
    /**
     * 1
     */
    public static final int[] ONE_INTS = new int[]{1, 0, 0, 0, 0, 0, 0, 0, 0, 0,};
    /**
     * -1
     */
    public static final int[] MINUS_ONE_INTS = new int[]{-1, 0, 0, 0, 0, 0, 0, 0, 0, 0,};
    /**
     * a
     */
    public static final int[] A_INTS = new int[]{486662, 0, 0, 0, 0, 0, 0, 0, 0, 0,};
    /**
     * √(-(a + 2))
     */
    public static final int[] SQRT_MINUS_A_PLUS_2_INTS = new int[]{
        -12222970, -8312128, -11511410, 9067497, -15300785, -241793, 25456130, 14121551, -12187136, 3972024,
    };
    /**
     * √(-1 / 2)
     */
    public static final int[] SQRT_MINUS_HALF_INTS = new int[]{
        -17256545, 3971863, 28865457, -1750208, 27359696, -16640980, 12573105, 1002827, -163343, 11073975,
    };
    /**
     * Precomputed value of one of the square roots of -1 (mod p).
     */
    public static final int[] SQRT_M1_INTS = new int[]{
        -32595792, -7943725, 9377950, 3500415, 12389472, -272473, -25146209, -2005654, 326686, 11406482,
    };

    /**
     * Create a field element with value 0.
     *
     * @return a field element with value 0.
     */
    public static int[] createZero() {
        return new int[INT_SIZE];
    }

    /**
     * Create a field element with value 1.
     *
     * @return a field element with value 1.
     */
    public static int[] createOne() {
        int[] z = new int[INT_SIZE];
        z[0] = 1;
        return z;
    }

    /**
     * Encode the field element x in its 32-byte representation.
     *
     * @param x the field element x.
     * @return the 32-byte representation.
     */
    public static byte[] encode(final int[] x) {
        assert x.length == INT_SIZE;
        int h0 = x[0];
        int h1 = x[1];
        int h2 = x[2];
        int h3 = x[3];
        int h4 = x[4];
        int h5 = x[5];
        int h6 = x[6];
        int h7 = x[7];
        int h8 = x[8];
        int h9 = x[9];
        int q;
        int carry0;
        int carry1;
        int carry2;
        int carry3;
        int carry4;
        int carry5;
        int carry6;
        int carry7;
        int carry8;
        int carry9;

        // Step 1:
        // Calculate q
        q = (19 * h9 + (1 << 24)) >> 25;
        q = (h0 + q) >> 26;
        q = (h1 + q) >> 25;
        q = (h2 + q) >> 26;
        q = (h3 + q) >> 25;
        q = (h4 + q) >> 26;
        q = (h5 + q) >> 25;
        q = (h6 + q) >> 26;
        q = (h7 + q) >> 25;
        q = (h8 + q) >> 26;
        q = (h9 + q) >> 25;

        // r = h - q * p = h - 2^255 * q + 19 * q
        // First add 19 * q then discard the bit 255
        h0 += 19 * q;

        carry0 = h0 >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;
        carry1 = h1 >> 25;
        h2 += carry1;
        h1 -= carry1 << 25;
        carry2 = h2 >> 26;
        h3 += carry2;
        h2 -= carry2 << 26;
        carry3 = h3 >> 25;
        h4 += carry3;
        h3 -= carry3 << 25;
        carry4 = h4 >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;
        carry5 = h5 >> 25;
        h6 += carry5;
        h5 -= carry5 << 25;
        carry6 = h6 >> 26;
        h7 += carry6;
        h6 -= carry6 << 26;
        carry7 = h7 >> 25;
        h8 += carry7;
        h7 -= carry7 << 25;
        carry8 = h8 >> 26;
        h9 += carry8;
        h8 -= carry8 << 26;
        carry9 = h9 >> 25;
        h9 -= carry9 << 25;

        // Step 2 (straight forward conversion):
        byte[] s = new byte[BYTE_SIZE];
        s[0] = (byte) h0;
        s[1] = (byte) (h0 >> 8);
        s[2] = (byte) (h0 >> 16);
        s[3] = (byte) ((h0 >> 24) | (h1 << 2));
        s[4] = (byte) (h1 >> 6);
        s[5] = (byte) (h1 >> 14);
        s[6] = (byte) ((h1 >> 22) | (h2 << 3));
        s[7] = (byte) (h2 >> 5);
        s[8] = (byte) (h2 >> 13);
        s[9] = (byte) ((h2 >> 21) | (h3 << 5));
        s[10] = (byte) (h3 >> 3);
        s[11] = (byte) (h3 >> 11);
        s[12] = (byte) ((h3 >> 19) | (h4 << 6));
        s[13] = (byte) (h4 >> 2);
        s[14] = (byte) (h4 >> 10);
        s[15] = (byte) (h4 >> 18);
        s[16] = (byte) h5;
        s[17] = (byte) (h5 >> 8);
        s[18] = (byte) (h5 >> 16);
        s[19] = (byte) ((h5 >> 24) | (h6 << 1));
        s[20] = (byte) (h6 >> 7);
        s[21] = (byte) (h6 >> 15);
        s[22] = (byte) ((h6 >> 23) | (h7 << 3));
        s[23] = (byte) (h7 >> 5);
        s[24] = (byte) (h7 >> 13);
        s[25] = (byte) ((h7 >> 21) | (h8 << 4));
        s[26] = (byte) (h8 >> 4);
        s[27] = (byte) (h8 >> 12);
        s[28] = (byte) ((h8 >> 20) | (h9 << 6));
        s[29] = (byte) (h9 >> 2);
        s[30] = (byte) (h9 >> 10);
        s[31] = (byte) (h9 >> 18);
        return s;
    }


    /**
     * Copy x to z.
     *
     * @param x the input x.
     * @param z the copied output z = x.
     */
    public static void copy(final int[] x, int[] z) {
        assert x.length == INT_SIZE;
        assert z.length == INT_SIZE;
        System.arraycopy(x, 0, z, 0, INT_SIZE);
    }

    /**
     * Compares the two field elements x and y.
     *
     * @return 1 if x == y, 0 otherwise.
     */
    public static int areEqual(final int[] x, final int[] y) {
        return CafeConstantTimeUtils.equal(encode(x), encode(y));
    }

    /**
     * Determine whether the field element x is zero.
     *
     * @return 1 if x is zero, 0 otherwise.
     */
    public static int isZero(final int[] x) {
        final byte[] s = encode(x);
        return CafeConstantTimeUtils.equal(s, ZERO_BYTES);
    }

    /**
     * Determine whether the field element x is negative.
     *
     * @return 1 if x is negative, 0 otherwise.
     */
    public static int isNeg(final int[] x) {
        final byte[] s = encode(x);
        return s[0] & 1;
    }

    /**
     * If cond = 1, then z = z; Otherwise, z = x.
     *
     * @param cond the condition.
     * @param x the other input x.
     * @param z the input z.
     */
    public static void cmov(final int cond, final int[] x, int[] z) {
        assert x.length == INT_SIZE;
        assert z.length == INT_SIZE;
        int c = -cond;
        for (int i = 0; i < INT_SIZE; i++) {
            int zi = z[i];
            int diff = zi ^ x[i];
            zi ^= (diff & c);
            z[i] = zi;
        }
    }

    /**
     * Compute z = x + y.
     *
     * @param x the input x.
     * @param y the input y.
     * @param z the output z = x + y.
     */
    public static void add(int[] x, final int[] y, int[] z) {
        for (int i = 0; i < INT_SIZE; i++) {
            z[i] = x[i] + y[i];
        }
    }

    /**
     * Compute z = x - y.
     *
     * @param x the input x.
     * @param y the input y.
     * @param z the output z = x - y.
     */
    public static void sub(int[] x, final int[] y, int[] z) {
        for (int i = 0; i < INT_SIZE; i++) {
            z[i] = x[i] - y[i];
        }
    }
}
