/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.utils.ByteEccUtils;

import java.util.Arrays;

/**
 * A field element of the field $\mathbb{Z} / (2^{255} - 19)$. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/FieldElement.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/6
 */
class CafeFieldElement {
    /**
     * field int size
     */
    static final int INT_SIZE = 10;
    /**
     * field byte size
     */
    static final int BYTE_SIZE = 32;
    /**
     * 0
     */
    static final CafeFieldElement ZERO = new CafeFieldElement(new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, });
    /**
     * 0 in byte array
     */
    private static final byte[] ZERO_BYTES = new byte[BYTE_SIZE];
    /**
     * 1
     */
    static final CafeFieldElement ONE = new CafeFieldElement(new int[]{1, 0, 0, 0, 0, 0, 0, 0, 0, 0, });
    /**
     * -1
     */
    static final CafeFieldElement MINUS_ONE = ZERO.sub(ONE);
    /**
     * a = 486662
     */
    static final CafeFieldElement A = new CafeFieldElement(new int[]{486662, 0, 0, 0, 0, 0, 0, 0, 0, 0, });
    /**
     * √(-(a + 2))
     */
    static final CafeFieldElement SQRT_MINUS_A_PLUS_2 = new CafeFieldElement(new int[]{
        -12222970, -8312128, -11511410, 9067497, -15300785, -241793, 25456130, 14121551, -12187136, 3972024,
    });
    /**
     * √(-1 / 2)
     */
    static final CafeFieldElement SQRT_MINUS_HALF = new CafeFieldElement(new int[]{
        -17256545, 3971863, 28865457, -1750208, 27359696, -16640980, 12573105, 1002827, -163343, 11073975,
    });

    /**
     * An element $t$, entries $t[0] \dots t[9]$, represents the integer $t[0] +
     * 2^{26} t[1] + 2^{51} t[2] + 2^{77} t[3] + 2^{102} t[4] + \dots + 2^{230}
     * t[9]$. Bounds on each $t[i]$ vary depending on context.
     */
    final int[] t;

    /**
     * Create a field element.
     *
     * @param t The $2^{25.5}$ bit representation of the field element.
     */
    public CafeFieldElement(int[] t) {
        if (t.length != INT_SIZE) {
            throw new IllegalArgumentException("Invalid radix-2^25.5 representation");
        }
        this.t = t;
    }

    /**
     * Decode a FieldElement from the low 255 bits of a 256-bit input.
     *
     * @param in The 32-byte representation.
     * @return The field element in its $2^{25.5}$ bit representation.
     */
    public static CafeFieldElement decode(byte[] in) {
        if (in.length != BYTE_SIZE) {
            throw new IllegalArgumentException("Invalid byte[] representation");
        }
        long h0 = ByteEccUtils.decodeLong32(in, 0);
        long h1 = ByteEccUtils.decodeLong24(in, 4) << 6;
        long h2 = ByteEccUtils.decodeLong24(in, 7) << 5;
        long h3 = ByteEccUtils.decodeLong24(in, 10) << 3;
        long h4 = ByteEccUtils.decodeLong24(in, 13) << 2;
        long h5 = ByteEccUtils.decodeLong32(in, 16);
        long h6 = ByteEccUtils.decodeLong24(in, 20) << 7;
        long h7 = ByteEccUtils.decodeLong24(in, 23) << 5;
        long h8 = ByteEccUtils.decodeLong24(in, 26) << 4;
        long h9 = (ByteEccUtils.decodeLong24(in, 29) & 0x7FFFFF) << 2;
        long carry0;
        long carry1;
        long carry2;
        long carry3;
        long carry4;
        long carry5;
        long carry6;
        long carry7;
        long carry8;
        long carry9;

        // Remember: 2^255 congruent 19 modulo p
        carry9 = (h9 + (long) (1 << 24)) >> 25;
        h0 += carry9 * 19;
        h9 -= carry9 << 25;
        carry1 = (h1 + (long) (1 << 24)) >> 25;
        h2 += carry1;
        h1 -= carry1 << 25;
        carry3 = (h3 + (long) (1 << 24)) >> 25;
        h4 += carry3;
        h3 -= carry3 << 25;
        carry5 = (h5 + (long) (1 << 24)) >> 25;
        h6 += carry5;
        h5 -= carry5 << 25;
        carry7 = (h7 + (long) (1 << 24)) >> 25;
        h8 += carry7;
        h7 -= carry7 << 25;

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;
        carry2 = (h2 + (long) (1 << 25)) >> 26;
        h3 += carry2;
        h2 -= carry2 << 26;
        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;
        carry6 = (h6 + (long) (1 << 25)) >> 26;
        h7 += carry6;
        h6 -= carry6 << 26;
        carry8 = (h8 + (long) (1 << 25)) >> 26;
        h9 += carry8;
        h8 -= carry8 << 26;

        int[] h = new int[INT_SIZE];
        h[0] = (int) h0;
        h[1] = (int) h1;
        h[2] = (int) h2;
        h[3] = (int) h3;
        h[4] = (int) h4;
        h[5] = (int) h5;
        h[6] = (int) h6;
        h[7] = (int) h7;
        h[8] = (int) h8;
        h[9] = (int) h9;
        return new CafeFieldElement(h);
    }

    /**
     * Encode a FieldElement in its 32-byte representation.
     * <p>
     * This is done in two steps:
     * <ol>
     * <li>Reduce the value of the field element modulo $p$.
     * <li>Convert the field element to the 32 byte representation.
     * <p>
     * The idea for the modulo $p$ reduction algorithm is as follows:
     * <h2>Assumption:</h2>
     * <ul>
     * <li>$p = 2^{255} - 19$
     * <li>$h = h_0 + 2^{25} * h_1 + 2^{(26+25)} * h_2 + \dots + 2^{230} * h_9$
     * where $0 \le |h_i| \lt 2^{27}$ for all $i=0,\dots,9$.
     * <li>$h \cong r \mod p$, i.e. $h = r + q * p$ for some suitable $0 \le r \lt
     * p$ and an integer $q$.
     * </ul>
     * <p>
     * Then $q = [2^{-255} * (h + 19 * 2^{-25} * h_9 + 1/2)]$ where $[x] =
     * floor(x)$.
     * <h2>Proof:</h2>
     * <p>
     * We begin with some very raw estimation for the bounds of some expressions:
     * <p>
     * $$ \begin{equation} |h| \lt 2^{230} * 2^{30} = 2^{260} \Rightarrow |r + q *
     * p| \lt 2^{260} \Rightarrow |q| \lt 2^{10}. \\ \Rightarrow -1/4 \le a := 19^2
     * * 2^{-255} * q \lt 1/4. \\ |h - 2^{230} * h_9| = |h_0 + \dots + 2^{204} *
     * h_8| \lt 2^{204} * 2^{30} = 2^{234}. \\ \Rightarrow -1/4 \le b := 19 *
     * 2^{-255} * (h - 2^{230} * h_9) \lt 1/4 \end{equation} $$
     * <p>
     * Therefore $0 \lt 1/2 - a - b \lt 1$.
     * <p>
     * Set $x := r + 19 * 2^{-255} * r + 1/2 - a - b$. Then:
     * <p>
     * $$ 0 \le x \lt 255 - 20 + 19 + 1 = 2^{255} \\ \Rightarrow 0 \le 2^{-255} * x
     * \lt 1. $$
     * <p>
     * Since $q$ is an integer we have
     * <p>
     * $$ [q + 2^{-255} * x] = q \quad (1) $$
     * <p>
     * Have a closer look at $x$:
     * <p>
     * $$ \begin{align} x &amp;= h - q * (2^{255} - 19) + 19 * 2^{-255} * (h - q *
     * (2^{255} - 19)) + 1/2 - 19^2 * 2^{-255} * q - 19 * 2^{-255} * (h - 2^{230} *
     * h_9) \\ &amp;= h - q * 2^{255} + 19 * q + 19 * 2^{-255} * h - 19 * q + 19^2 *
     * 2^{-255} * q + 1/2 - 19^2 * 2^{-255} * q - 19 * 2^{-255} * h + 19 * 2^{-25} *
     * h_9 \\ &amp;= h + 19 * 2^{-25} * h_9 + 1/2 - q^{255}. \end{align} $$
     * <p>
     * Inserting the expression for $x$ into $(1)$ we get the desired expression for
     * $q$.
     *
     * @return the 32-byte encoding of this FieldElement.
     */
    byte[] encode() {
        int h0 = t[0];
        int h1 = t[1];
        int h2 = t[2];
        int h3 = t[3];
        int h4 = t[4];
        int h5 = t[5];
        int h6 = t[6];
        int h7 = t[7];
        int h8 = t[8];
        int h9 = t[9];
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
     * Constant-time equality check. Compares the encodings of the two FieldElements.
     *
     * @return 1 if self and other are equal, 0 otherwise.
     */
    public int cequals(CafeFieldElement other) {
        return CafeConstantTimeUtils.equal(encode(), other.encode());
    }

    /**
     * Constant-time selection between two FieldElements.
     * <p>
     * Implemented as a conditional copy. Logic is inspired by the SUPERCOP implementation.
     * </p>
     * This function is identical to CMOV(a, that, c):
     * <p>
     * If c is False, CMOV returns a, otherwise it returns that.
     * For constant-time implementations, this operation must run in time independent of the value of c.
     * </p>
     *
     * @param that the other field element.
     * @param c    must be 0 or 1, otherwise results are undefined.
     * @return a copy of this if $c == 0$, or a copy of that if $c == 1$.
     * @see <a href=
     * "https://github.com/floodyberry/supercop/blob/master/crypto_sign/ed25519/ref10/fe_cmov.c"
     * target="_top">SUPERCOP</a>
     */
    public CafeFieldElement cmov(CafeFieldElement that, int c) {
        c = -c;
        int[] result = new int[INT_SIZE];
        for (int i = 0; i < INT_SIZE; i++) {
            result[i] = t[i];
            int x = t[i] ^ that.t[i];
            x &= c;
            result[i] ^= x;
        }
        return new CafeFieldElement(result);
    }

    /**
     * Compute the absolute value of this FieldElement in constant time.
     *
     * @return $|\text{this}|$.
     */
    CafeFieldElement abs() {
        return cmov(neg(), isNegative());
    }

    /**
     * Equality check overridden to be constant-time. Fails fast if the objects are of different types.
     *
     * @return true if self and other are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CafeFieldElement)) {
            return false;
        }

        CafeFieldElement other = (CafeFieldElement) obj;
        return cequals(other) == 1;
    }

    @Override
    public int hashCode() {
        // The general contract for the hashCode method states that equal objects must
        // have equal hash codes. Object equality is based on the encodings of the
        // field elements, not their internal representations (which may not be
        // canonical).
        final byte[] s = encode();
        return Arrays.hashCode(s);
    }

    /**
     * Determine whether this FieldElement is zero.
     *
     * @return 1 if this FieldElement is zero, 0 otherwise.
     */
    int isZero() {
        final byte[] s = encode();
        return CafeConstantTimeUtils.equal(s, ZERO_BYTES);
    }

    /**
     * Determine whether this FieldElement is negative.
     * <p>
     * As in RFC 8032, a FieldElement is negative if the least significant bit of the encoding is 1.
     * </p>
     *
     * @return 1 if this FieldElement is negative, 0 otherwise.
     * @see <a href="https://tools.ietf.org/html/rfc8032" target="_top">RFC 8032</a>
     */
    int isNegative() {
        final byte[] s = encode();
        return s[0] & 1;
    }

    /**
     * $h = f + g$
     * <p>
     * Preconditions:
     * <ul>
     * <li>$|f|$ bounded by $1.1*2^{25},1.1*2^{24},1.1*2^{25},1.1*2^{24},$ etc.
     * <li>$|g|$ bounded by $1.1*2^{25},1.1*2^{24},1.1*2^{25},1.1*2^{24},$ etc.
     * </ul>
     * <p>
     * Postconditions:
     * <ul>
     * <li>$|h|$ bounded by $1.1*2^{26},1.1*2^{25},1.1*2^{26},1.1*2^{25},$ etc.
     * </ul>
     *
     * @param val The field element to add.
     * @return The field element this + val.
     */
    public CafeFieldElement add(CafeFieldElement val) {
        int[] h = new int[INT_SIZE];for (int i = 0; i < INT_SIZE; i++) {
            h[i] = t[i] + val.t[i];
        }

        return new CafeFieldElement(h);
    }

    /**
     * $h = f - g$
     * <p>
     * Can overlap $h$ with $f$ or $g$.
     * <p>
     * Preconditions:
     * <ul>
     * <li>$|f|$ bounded by $1.1*2^{25},1.1*2^{24},1.1*2^{25},1.1*2^{24},$ etc.
     * <li>$|g|$ bounded by $1.1*2^{25},1.1*2^{24},1.1*2^{25},1.1*2^{24},$ etc.
     * </ul>
     * <p>
     * Postconditions:
     * <ul>
     * <li>$|h|$ bounded by $1.1*2^{26},1.1*2^{25},1.1*2^{26},1.1*2^{25},$ etc.
     * </ul>
     *
     * @param val The field element to subtract.
     * @return The field element this - val.
     **/
    public CafeFieldElement sub(CafeFieldElement val) {
        int[] h = new int[INT_SIZE];
        for (int i = 0; i < INT_SIZE; i++) {
            h[i] = t[i] - val.t[i];
        }
        return new CafeFieldElement(h);
    }

    /**
     * $h = -f$
     * <p>
     * Preconditions:
     * <ul>
     * <li>$|f|$ bounded by $1.1*2^{25},1.1*2^{24},1.1*2^{25},1.1*2^{24},$ etc.
     * <p>
     * Postconditions:
     * <ul>
     * <li>$|h|$ bounded by $1.1*2^{25},1.1*2^{24},1.1*2^{25},1.1*2^{24},$ etc.
     *
     * @return The field element (-1) * this.
     */
    public CafeFieldElement neg() {
        int[] h = new int[INT_SIZE];
        for (int i = 0; i < INT_SIZE; i++) {
            h[i] = -t[i];
        }
        return new CafeFieldElement(h);
    }

    /**
     * $h = f * g$
     * <p>
     * Can overlap $h$ with $f$ or $g$.
     * <p>
     * Preconditions:
     * <ul>
     * <li>$|f|$ bounded by $1.65*2^{26},1.65*2^{25},1.65*2^{26},1.65*2^{25},$ etc.
     * <li>$|g|$ bounded by $1.65*2^{26},1.65*2^{25},1.65*2^{26},1.65*2^{25},$ etc.
     * <p>
     * Postconditions:
     * <ul>
     * <li>$|h|$ bounded by $1.01*2^{25},1.01*2^{24},1.01*2^{25},1.01*2^{24},$ etc.
     * <p>
     * Notes on implementation strategy:
     * <p>
     * Using schoolbook multiplication. Karatsuba would save a little in some cost
     * models.
     * <p>
     * Most multiplications by 2 and 19 are 32-bit precomputations; cheaper than
     * 64-bit postcomputations.
     * <p>
     * There is one remaining multiplication by 19 in the carry chain; one *19
     * precomputation can be merged into this, but the resulting data flow is
     * considerably less clean.
     * <p>
     * There are 12 carries below. 10 of them are 2-way parallelizable and
     * vectorizable. Can get away with 11 carries, but then data flow is much
     * deeper.
     * <p>
     * With tighter constraints on inputs can squeeze carries into int32.
     *
     * @param val The field element to multiply.
     * @return The (reasonably reduced) field element this * val.
     */
    public CafeFieldElement mul(CafeFieldElement val) {
        int[] g = val.t;
        // 1.959375*2^29
        int g1_19 = 19 * g[1];
        // 1.959375*2^30; still ok
        int g2_19 = 19 * g[2];
        int g3_19 = 19 * g[3];
        int g4_19 = 19 * g[4];
        int g5_19 = 19 * g[5];
        int g6_19 = 19 * g[6];
        int g7_19 = 19 * g[7];
        int g8_19 = 19 * g[8];
        int g9_19 = 19 * g[9];
        int f1_2 = 2 * t[1];
        int f3_2 = 2 * t[3];
        int f5_2 = 2 * t[5];
        int f7_2 = 2 * t[7];
        int f9_2 = 2 * t[9];

        long f0g0 = t[0] * (long) g[0];
        long f0g1 = t[0] * (long) g[1];
        long f0g2 = t[0] * (long) g[2];
        long f0g3 = t[0] * (long) g[3];
        long f0g4 = t[0] * (long) g[4];
        long f0g5 = t[0] * (long) g[5];
        long f0g6 = t[0] * (long) g[6];
        long f0g7 = t[0] * (long) g[7];
        long f0g8 = t[0] * (long) g[8];
        long f0g9 = t[0] * (long) g[9];
        long f1g0 = t[1] * (long) g[0];
        long f1g1_2 = f1_2 * (long) g[1];
        long f1g2 = t[1] * (long) g[2];
        long f1g3_2 = f1_2 * (long) g[3];
        long f1g4 = t[1] * (long) g[4];
        long f1g5_2 = f1_2 * (long) g[5];
        long f1g6 = t[1] * (long) g[6];
        long f1g7_2 = f1_2 * (long) g[7];
        long f1g8 = t[1] * (long) g[8];
        long f1g9_38 = f1_2 * (long) g9_19;
        long f2g0 = t[2] * (long) g[0];
        long f2g1 = t[2] * (long) g[1];
        long f2g2 = t[2] * (long) g[2];
        long f2g3 = t[2] * (long) g[3];
        long f2g4 = t[2] * (long) g[4];
        long f2g5 = t[2] * (long) g[5];
        long f2g6 = t[2] * (long) g[6];
        long f2g7 = t[2] * (long) g[7];
        long f2g8_19 = t[2] * (long) g8_19;
        long f2g9_19 = t[2] * (long) g9_19;
        long f3g0 = t[3] * (long) g[0];
        long f3g1_2 = f3_2 * (long) g[1];
        long f3g2 = t[3] * (long) g[2];
        long f3g3_2 = f3_2 * (long) g[3];
        long f3g4 = t[3] * (long) g[4];
        long f3g5_2 = f3_2 * (long) g[5];
        long f3g6 = t[3] * (long) g[6];
        long f3g7_38 = f3_2 * (long) g7_19;
        long f3g8_19 = t[3] * (long) g8_19;
        long f3g9_38 = f3_2 * (long) g9_19;
        long f4g0 = t[4] * (long) g[0];
        long f4g1 = t[4] * (long) g[1];
        long f4g2 = t[4] * (long) g[2];
        long f4g3 = t[4] * (long) g[3];
        long f4g4 = t[4] * (long) g[4];
        long f4g5 = t[4] * (long) g[5];
        long f4g6_19 = t[4] * (long) g6_19;
        long f4g7_19 = t[4] * (long) g7_19;
        long f4g8_19 = t[4] * (long) g8_19;
        long f4g9_19 = t[4] * (long) g9_19;
        long f5g0 = t[5] * (long) g[0];
        long f5g1_2 = f5_2 * (long) g[1];
        long f5g2 = t[5] * (long) g[2];
        long f5g3_2 = f5_2 * (long) g[3];
        long f5g4 = t[5] * (long) g[4];
        long f5g5_38 = f5_2 * (long) g5_19;
        long f5g6_19 = t[5] * (long) g6_19;
        long f5g7_38 = f5_2 * (long) g7_19;
        long f5g8_19 = t[5] * (long) g8_19;
        long f5g9_38 = f5_2 * (long) g9_19;
        long f6g0 = t[6] * (long) g[0];
        long f6g1 = t[6] * (long) g[1];
        long f6g2 = t[6] * (long) g[2];
        long f6g3 = t[6] * (long) g[3];
        long f6g4_19 = t[6] * (long) g4_19;
        long f6g5_19 = t[6] * (long) g5_19;
        long f6g6_19 = t[6] * (long) g6_19;
        long f6g7_19 = t[6] * (long) g7_19;
        long f6g8_19 = t[6] * (long) g8_19;
        long f6g9_19 = t[6] * (long) g9_19;
        long f7g0 = t[7] * (long) g[0];
        long f7g1_2 = f7_2 * (long) g[1];
        long f7g2 = t[7] * (long) g[2];
        long f7g3_38 = f7_2 * (long) g3_19;
        long f7g4_19 = t[7] * (long) g4_19;
        long f7g5_38 = f7_2 * (long) g5_19;
        long f7g6_19 = t[7] * (long) g6_19;
        long f7g7_38 = f7_2 * (long) g7_19;
        long f7g8_19 = t[7] * (long) g8_19;
        long f7g9_38 = f7_2 * (long) g9_19;
        long f8g0 = t[8] * (long) g[0];
        long f8g1 = t[8] * (long) g[1];
        long f8g2_19 = t[8] * (long) g2_19;
        long f8g3_19 = t[8] * (long) g3_19;
        long f8g4_19 = t[8] * (long) g4_19;
        long f8g5_19 = t[8] * (long) g5_19;
        long f8g6_19 = t[8] * (long) g6_19;
        long f8g7_19 = t[8] * (long) g7_19;
        long f8g8_19 = t[8] * (long) g8_19;
        long f8g9_19 = t[8] * (long) g9_19;
        long f9g0 = t[9] * (long) g[0];
        long f9g1_38 = f9_2 * (long) g1_19;
        long f9g2_19 = t[9] * (long) g2_19;
        long f9g3_38 = f9_2 * (long) g3_19;
        long f9g4_19 = t[9] * (long) g4_19;
        long f9g5_38 = f9_2 * (long) g5_19;
        long f9g6_19 = t[9] * (long) g6_19;
        long f9g7_38 = f9_2 * (long) g7_19;
        long f9g8_19 = t[9] * (long) g8_19;
        long f9g9_38 = f9_2 * (long) g9_19;

        /*
         * Remember: 2^255 congruent 19 modulo p. h = h0 * 2^0 + h1 * 2^26 + h2 *
         * 2^(26+25) + h3 * 2^(26+25+26) + ... + h9 * 2^(5*26+5*25). So to get the real
         * number we would have to multiply the coefficients with the corresponding
         * powers of 2. To get an idea what is going on below, look at the calculation
         * of h0: h0 is the coefficient to the power 2^0 so it collects (sums) all
         * products that have the power 2^0. f0 * g0 really is f0 * 2^0 * g0 * 2^0 = (f0
         * * g0) * 2^0. f1 * g9 really is f1 * 2^26 * g9 * 2^230 = f1 * g9 * 2^256 = 2 *
         * f1 * g9 * 2^255 congruent 2 * 19 * f1 * g9 * 2^0 modulo p. f2 * g8 really is
         * f2 * 2^51 * g8 * 2^204 = f2 * g8 * 2^255 congruent 19 * f2 * g8 * 2^0 modulo
         * p. and so on...
         */
        long h0 = f0g0 + f1g9_38 + f2g8_19 + f3g7_38 + f4g6_19 + f5g5_38 + f6g4_19 + f7g3_38 + f8g2_19 + f9g1_38;
        long h1 = f0g1 + f1g0 + f2g9_19 + f3g8_19 + f4g7_19 + f5g6_19 + f6g5_19 + f7g4_19 + f8g3_19 + f9g2_19;
        long h2 = f0g2 + f1g1_2 + f2g0 + f3g9_38 + f4g8_19 + f5g7_38 + f6g6_19 + f7g5_38 + f8g4_19 + f9g3_38;
        long h3 = f0g3 + f1g2 + f2g1 + f3g0 + f4g9_19 + f5g8_19 + f6g7_19 + f7g6_19 + f8g5_19 + f9g4_19;
        long h4 = f0g4 + f1g3_2 + f2g2 + f3g1_2 + f4g0 + f5g9_38 + f6g8_19 + f7g7_38 + f8g6_19 + f9g5_38;
        long h5 = f0g5 + f1g4 + f2g3 + f3g2 + f4g1 + f5g0 + f6g9_19 + f7g8_19 + f8g7_19 + f9g6_19;
        long h6 = f0g6 + f1g5_2 + f2g4 + f3g3_2 + f4g2 + f5g1_2 + f6g0 + f7g9_38 + f8g8_19 + f9g7_38;
        long h7 = f0g7 + f1g6 + f2g5 + f3g4 + f4g3 + f5g2 + f6g1 + f7g0 + f8g9_19 + f9g8_19;
        long h8 = f0g8 + f1g7_2 + f2g6 + f3g5_2 + f4g4 + f5g3_2 + f6g2 + f7g1_2 + f8g0 + f9g9_38;
        long h9 = f0g9 + f1g8 + f2g7 + f3g6 + f4g5 + f5g4 + f6g3 + f7g2 + f8g1 + f9g0;

        long carry0;
        long carry1;
        long carry2;
        long carry3;
        long carry4;
        long carry5;
        long carry6;
        long carry7;
        long carry8;
        long carry9;

        /*
         * |h0| <= (1.65*1.65*2^52*(1+19+19+19+19)+1.65*1.65*2^50*(38+38+38+38+38)) i.e.
         * |h0| <= 1.4*2^60; narrower ranges for h2, h4, h6, h8 |h1| <=
         * (1.65*1.65*2^51*(1+1+19+19+19+19+19+19+19+19)) i.e. |h1| <= 1.7*2^59;
         * narrower ranges for h3, h5, h7, h9
         */

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;
        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;
        /* |h0| <= 2^25 */
        /* |h4| <= 2^25 */
        /* |h1| <= 1.71*2^59 */
        /* |h5| <= 1.71*2^59 */

        carry1 = (h1 + (long) (1 << 24)) >> 25;
        h2 += carry1;
        h1 -= carry1 << 25;
        carry5 = (h5 + (long) (1 << 24)) >> 25;
        h6 += carry5;
        h5 -= carry5 << 25;
        /* |h1| <= 2^24; from now on fits into int32 */
        /* |h5| <= 2^24; from now on fits into int32 */
        /* |h2| <= 1.41*2^60 */
        /* |h6| <= 1.41*2^60 */

        carry2 = (h2 + (long) (1 << 25)) >> 26;
        h3 += carry2;
        h2 -= carry2 << 26;
        carry6 = (h6 + (long) (1 << 25)) >> 26;
        h7 += carry6;
        h6 -= carry6 << 26;
        /* |h2| <= 2^25; from now on fits into int32 unchanged */
        /* |h6| <= 2^25; from now on fits into int32 unchanged */
        /* |h3| <= 1.71*2^59 */
        /* |h7| <= 1.71*2^59 */

        carry3 = (h3 + (long) (1 << 24)) >> 25;
        h4 += carry3;
        h3 -= carry3 << 25;
        carry7 = (h7 + (long) (1 << 24)) >> 25;
        h8 += carry7;
        h7 -= carry7 << 25;
        /* |h3| <= 2^24; from now on fits into int32 unchanged */
        /* |h7| <= 2^24; from now on fits into int32 unchanged */
        /* |h4| <= 1.72*2^34 */
        /* |h8| <= 1.41*2^60 */

        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;
        carry8 = (h8 + (long) (1 << 25)) >> 26;
        h9 += carry8;
        h8 -= carry8 << 26;
        /* |h4| <= 2^25; from now on fits into int32 unchanged */
        /* |h8| <= 2^25; from now on fits into int32 unchanged */
        /* |h5| <= 1.01*2^24 */
        /* |h9| <= 1.71*2^59 */

        carry9 = (h9 + (long) (1 << 24)) >> 25;
        h0 += carry9 * 19;
        h9 -= carry9 << 25;
        /* |h9| <= 2^24; from now on fits into int32 unchanged */
        /* |h0| <= 1.1*2^39 */

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;
        /* |h0| <= 2^25; from now on fits into int32 unchanged */
        /* |h1| <= 1.01*2^24 */

        int[] h = new int[INT_SIZE];
        h[0] = (int) h0;
        h[1] = (int) h1;
        h[2] = (int) h2;
        h[3] = (int) h3;
        h[4] = (int) h4;
        h[5] = (int) h5;
        h[6] = (int) h6;
        h[7] = (int) h7;
        h[8] = (int) h8;
        h[9] = (int) h9;
        return new CafeFieldElement(h);
    }

    /**
     * $h = f * f$
     * <p>
     * Can overlap $h$ with $f$.
     * <p>
     * Preconditions:
     * <ul>
     * <li>$|f|$ bounded by $1.65*2^{26},1.65*2^{25},1.65*2^{26},1.65*2^{25},$ etc.
     * <p>
     * Postconditions:
     * <ul>
     * <li>$|h|$ bounded by $1.01*2^{25},1.01*2^{24},1.01*2^{25},1.01*2^{24},$ etc.
     * <p>
     * See {@link #mul(CafeFieldElement)} for discussion of implementation
     * strategy.
     *
     * @return The (reasonably reduced) square of this field element.
     */
    public CafeFieldElement sqr() {
        int f0 = t[0];
        int f1 = t[1];
        int f2 = t[2];
        int f3 = t[3];
        int f4 = t[4];
        int f5 = t[5];
        int f6 = t[6];
        int f7 = t[7];
        int f8 = t[8];
        int f9 = t[9];
        int f0_2 = 2 * f0;
        int f1_2 = 2 * f1;
        int f2_2 = 2 * f2;
        int f3_2 = 2 * f3;
        int f4_2 = 2 * f4;
        int f5_2 = 2 * f5;
        int f6_2 = 2 * f6;
        int f7_2 = 2 * f7;
        // 1.959375*2^30
        int f5_38 = 38 * f5;
        // 1.959375*2^30
        int f6_19 = 19 * f6;
        // 1.959375*2^30
        int f7_38 = 38 * f7;
        // 1.959375*2^30
        int f8_19 = 19 * f8;
        // 1.959375*2^30
        int f9_38 = 38 * f9;

        long f0f0 = f0 * (long) f0;
        long f0f1_2 = f0_2 * (long) f1;
        long f0f2_2 = f0_2 * (long) f2;
        long f0f3_2 = f0_2 * (long) f3;
        long f0f4_2 = f0_2 * (long) f4;
        long f0f5_2 = f0_2 * (long) f5;
        long f0f6_2 = f0_2 * (long) f6;
        long f0f7_2 = f0_2 * (long) f7;
        long f0f8_2 = f0_2 * (long) f8;
        long f0f9_2 = f0_2 * (long) f9;
        long f1f1_2 = f1_2 * (long) f1;
        long f1f2_2 = f1_2 * (long) f2;
        long f1f3_4 = f1_2 * (long) f3_2;
        long f1f4_2 = f1_2 * (long) f4;
        long f1f5_4 = f1_2 * (long) f5_2;
        long f1f6_2 = f1_2 * (long) f6;
        long f1f7_4 = f1_2 * (long) f7_2;
        long f1f8_2 = f1_2 * (long) f8;
        long f1f9_76 = f1_2 * (long) f9_38;
        long f2f2 = f2 * (long) f2;
        long f2f3_2 = f2_2 * (long) f3;
        long f2f4_2 = f2_2 * (long) f4;
        long f2f5_2 = f2_2 * (long) f5;
        long f2f6_2 = f2_2 * (long) f6;
        long f2f7_2 = f2_2 * (long) f7;
        long f2f8_38 = f2_2 * (long) f8_19;
        long f2f9_38 = f2 * (long) f9_38;
        long f3f3_2 = f3_2 * (long) f3;
        long f3f4_2 = f3_2 * (long) f4;
        long f3f5_4 = f3_2 * (long) f5_2;
        long f3f6_2 = f3_2 * (long) f6;
        long f3f7_76 = f3_2 * (long) f7_38;
        long f3f8_38 = f3_2 * (long) f8_19;
        long f3f9_76 = f3_2 * (long) f9_38;
        long f4f4 = f4 * (long) f4;
        long f4f5_2 = f4_2 * (long) f5;
        long f4f6_38 = f4_2 * (long) f6_19;
        long f4f7_38 = f4 * (long) f7_38;
        long f4f8_38 = f4_2 * (long) f8_19;
        long f4f9_38 = f4 * (long) f9_38;
        long f5f5_38 = f5 * (long) f5_38;
        long f5f6_38 = f5_2 * (long) f6_19;
        long f5f7_76 = f5_2 * (long) f7_38;
        long f5f8_38 = f5_2 * (long) f8_19;
        long f5f9_76 = f5_2 * (long) f9_38;
        long f6f6_19 = f6 * (long) f6_19;
        long f6f7_38 = f6 * (long) f7_38;
        long f6f8_38 = f6_2 * (long) f8_19;
        long f6f9_38 = f6 * (long) f9_38;
        long f7f7_38 = f7 * (long) f7_38;
        long f7f8_38 = f7_2 * (long) f8_19;
        long f7f9_76 = f7_2 * (long) f9_38;
        long f8f8_19 = f8 * (long) f8_19;
        long f8f9_38 = f8 * (long) f9_38;
        long f9f9_38 = f9 * (long) f9_38;

        /*
         * Same procedure as in multiply, but this time we have a higher symmetry leading to less summands. e.g.
         * f1f9_76 really stands for f1 * 2^26 * f9 * 2^230 + f9 * 2^230 + f1 * 2^26 congruent 2 * 2 * 19 * f1 * f9 2^0
         * modulo p.
         */
        long h0 = f0f0 + f1f9_76 + f2f8_38 + f3f7_76 + f4f6_38 + f5f5_38;
        long h1 = f0f1_2 + f2f9_38 + f3f8_38 + f4f7_38 + f5f6_38;
        long h2 = f0f2_2 + f1f1_2 + f3f9_76 + f4f8_38 + f5f7_76 + f6f6_19;
        long h3 = f0f3_2 + f1f2_2 + f4f9_38 + f5f8_38 + f6f7_38;
        long h4 = f0f4_2 + f1f3_4 + f2f2 + f5f9_76 + f6f8_38 + f7f7_38;
        long h5 = f0f5_2 + f1f4_2 + f2f3_2 + f6f9_38 + f7f8_38;
        long h6 = f0f6_2 + f1f5_4 + f2f4_2 + f3f3_2 + f7f9_76 + f8f8_19;
        long h7 = f0f7_2 + f1f6_2 + f2f5_2 + f3f4_2 + f8f9_38;
        long h8 = f0f8_2 + f1f7_4 + f2f6_2 + f3f5_4 + f4f4 + f9f9_38;
        long h9 = f0f9_2 + f1f8_2 + f2f7_2 + f3f6_2 + f4f5_2;

        long carry0;
        long carry1;
        long carry2;
        long carry3;
        long carry4;
        long carry5;
        long carry6;
        long carry7;
        long carry8;
        long carry9;

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;
        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;

        carry1 = (h1 + (long) (1 << 24)) >> 25;
        h2 += carry1;
        h1 -= carry1 << 25;
        carry5 = (h5 + (long) (1 << 24)) >> 25;
        h6 += carry5;
        h5 -= carry5 << 25;

        carry2 = (h2 + (long) (1 << 25)) >> 26;
        h3 += carry2;
        h2 -= carry2 << 26;
        carry6 = (h6 + (long) (1 << 25)) >> 26;
        h7 += carry6;
        h6 -= carry6 << 26;

        carry3 = (h3 + (long) (1 << 24)) >> 25;
        h4 += carry3;
        h3 -= carry3 << 25;
        carry7 = (h7 + (long) (1 << 24)) >> 25;
        h8 += carry7;
        h7 -= carry7 << 25;

        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;
        carry8 = (h8 + (long) (1 << 25)) >> 26;
        h9 += carry8;
        h8 -= carry8 << 26;

        carry9 = (h9 + (long) (1 << 24)) >> 25;
        h0 += carry9 * 19;
        h9 -= carry9 << 25;

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;

        int[] h = new int[INT_SIZE];
        h[0] = (int) h0;
        h[1] = (int) h1;
        h[2] = (int) h2;
        h[3] = (int) h3;
        h[4] = (int) h4;
        h[5] = (int) h5;
        h[6] = (int) h6;
        h[7] = (int) h7;
        h[8] = (int) h8;
        h[9] = (int) h9;
        return new CafeFieldElement(h);
    }

    /**
     * $h = 2 * f * f$
     * <p>
     * Can overlap $h$ with $f$.
     * <p>
     * Preconditions:
     * <ul>
     * <li>$|f|$ bounded by $1.65*2^{26},1.65*2^{25},1.65*2^{26},1.65*2^{25},$ etc.
     * </ul>
     * <p>
     * Postconditions:
     * <ul>
     * <li>$|h|$ bounded by $1.01*2^{25},1.01*2^{24},1.01*2^{25},1.01*2^{24},$ etc.
     * </ul>
     * <p>
     * See {@link #mul(CafeFieldElement)} for discussion of implementation
     * strategy.
     *
     * @return The (reasonably reduced) square of this field element times 2.
     */
    public CafeFieldElement sqrDbl() {
        int f0 = t[0];
        int f1 = t[1];
        int f2 = t[2];
        int f3 = t[3];
        int f4 = t[4];
        int f5 = t[5];
        int f6 = t[6];
        int f7 = t[7];
        int f8 = t[8];
        int f9 = t[9];
        int f0_2 = 2 * f0;
        int f1_2 = 2 * f1;
        int f2_2 = 2 * f2;
        int f3_2 = 2 * f3;
        int f4_2 = 2 * f4;
        int f5_2 = 2 * f5;
        int f6_2 = 2 * f6;
        int f7_2 = 2 * f7;
        // 1.959375*2^30
        int f5_38 = 38 * f5;
        // 1.959375*2^30
        int f6_19 = 19 * f6;
        // 1.959375*2^30
        int f7_38 = 38 * f7;
        // 1.959375*2^30
        int f8_19 = 19 * f8;
        // 1.959375*2^30
        int f9_38 = 38 * f9;

        long f0f0 = f0 * (long) f0;
        long f0f1_2 = f0_2 * (long) f1;
        long f0f2_2 = f0_2 * (long) f2;
        long f0f3_2 = f0_2 * (long) f3;
        long f0f4_2 = f0_2 * (long) f4;
        long f0f5_2 = f0_2 * (long) f5;
        long f0f6_2 = f0_2 * (long) f6;
        long f0f7_2 = f0_2 * (long) f7;
        long f0f8_2 = f0_2 * (long) f8;
        long f0f9_2 = f0_2 * (long) f9;
        long f1f1_2 = f1_2 * (long) f1;
        long f1f2_2 = f1_2 * (long) f2;
        long f1f3_4 = f1_2 * (long) f3_2;
        long f1f4_2 = f1_2 * (long) f4;
        long f1f5_4 = f1_2 * (long) f5_2;
        long f1f6_2 = f1_2 * (long) f6;
        long f1f7_4 = f1_2 * (long) f7_2;
        long f1f8_2 = f1_2 * (long) f8;
        long f1f9_76 = f1_2 * (long) f9_38;
        long f2f2 = f2 * (long) f2;
        long f2f3_2 = f2_2 * (long) f3;
        long f2f4_2 = f2_2 * (long) f4;
        long f2f5_2 = f2_2 * (long) f5;
        long f2f6_2 = f2_2 * (long) f6;
        long f2f7_2 = f2_2 * (long) f7;
        long f2f8_38 = f2_2 * (long) f8_19;
        long f2f9_38 = f2 * (long) f9_38;
        long f3f3_2 = f3_2 * (long) f3;
        long f3f4_2 = f3_2 * (long) f4;
        long f3f5_4 = f3_2 * (long) f5_2;
        long f3f6_2 = f3_2 * (long) f6;
        long f3f7_76 = f3_2 * (long) f7_38;
        long f3f8_38 = f3_2 * (long) f8_19;
        long f3f9_76 = f3_2 * (long) f9_38;
        long f4f4 = f4 * (long) f4;
        long f4f5_2 = f4_2 * (long) f5;
        long f4f6_38 = f4_2 * (long) f6_19;
        long f4f7_38 = f4 * (long) f7_38;
        long f4f8_38 = f4_2 * (long) f8_19;
        long f4f9_38 = f4 * (long) f9_38;
        long f5f5_38 = f5 * (long) f5_38;
        long f5f6_38 = f5_2 * (long) f6_19;
        long f5f7_76 = f5_2 * (long) f7_38;
        long f5f8_38 = f5_2 * (long) f8_19;
        long f5f9_76 = f5_2 * (long) f9_38;
        long f6f6_19 = f6 * (long) f6_19;
        long f6f7_38 = f6 * (long) f7_38;
        long f6f8_38 = f6_2 * (long) f8_19;
        long f6f9_38 = f6 * (long) f9_38;
        long f7f7_38 = f7 * (long) f7_38;
        long f7f8_38 = f7_2 * (long) f8_19;
        long f7f9_76 = f7_2 * (long) f9_38;
        long f8f8_19 = f8 * (long) f8_19;
        long f8f9_38 = f8 * (long) f9_38;
        long f9f9_38 = f9 * (long) f9_38;
        long h0 = f0f0 + f1f9_76 + f2f8_38 + f3f7_76 + f4f6_38 + f5f5_38;
        long h1 = f0f1_2 + f2f9_38 + f3f8_38 + f4f7_38 + f5f6_38;
        long h2 = f0f2_2 + f1f1_2 + f3f9_76 + f4f8_38 + f5f7_76 + f6f6_19;
        long h3 = f0f3_2 + f1f2_2 + f4f9_38 + f5f8_38 + f6f7_38;
        long h4 = f0f4_2 + f1f3_4 + f2f2 + f5f9_76 + f6f8_38 + f7f7_38;
        long h5 = f0f5_2 + f1f4_2 + f2f3_2 + f6f9_38 + f7f8_38;
        long h6 = f0f6_2 + f1f5_4 + f2f4_2 + f3f3_2 + f7f9_76 + f8f8_19;
        long h7 = f0f7_2 + f1f6_2 + f2f5_2 + f3f4_2 + f8f9_38;
        long h8 = f0f8_2 + f1f7_4 + f2f6_2 + f3f5_4 + f4f4 + f9f9_38;
        long h9 = f0f9_2 + f1f8_2 + f2f7_2 + f3f6_2 + f4f5_2;

        long carry0;
        long carry1;
        long carry2;
        long carry3;
        long carry4;
        long carry5;
        long carry6;
        long carry7;
        long carry8;
        long carry9;

        h0 += h0;
        h1 += h1;
        h2 += h2;
        h3 += h3;
        h4 += h4;
        h5 += h5;
        h6 += h6;
        h7 += h7;
        h8 += h8;
        h9 += h9;

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;
        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;

        carry1 = (h1 + (long) (1 << 24)) >> 25;
        h2 += carry1;
        h1 -= carry1 << 25;
        carry5 = (h5 + (long) (1 << 24)) >> 25;
        h6 += carry5;
        h5 -= carry5 << 25;

        carry2 = (h2 + (long) (1 << 25)) >> 26;
        h3 += carry2;
        h2 -= carry2 << 26;
        carry6 = (h6 + (long) (1 << 25)) >> 26;
        h7 += carry6;
        h6 -= carry6 << 26;

        carry3 = (h3 + (long) (1 << 24)) >> 25;
        h4 += carry3;
        h3 -= carry3 << 25;
        carry7 = (h7 + (long) (1 << 24)) >> 25;
        h8 += carry7;
        h7 -= carry7 << 25;

        carry4 = (h4 + (long) (1 << 25)) >> 26;
        h5 += carry4;
        h4 -= carry4 << 26;
        carry8 = (h8 + (long) (1 << 25)) >> 26;
        h9 += carry8;
        h8 -= carry8 << 26;

        carry9 = (h9 + (long) (1 << 24)) >> 25;
        h0 += carry9 * 19;
        h9 -= carry9 << 25;

        carry0 = (h0 + (long) (1 << 25)) >> 26;
        h1 += carry0;
        h0 -= carry0 << 26;

        int[] h = new int[INT_SIZE];
        h[0] = (int) h0;
        h[1] = (int) h1;
        h[2] = (int) h2;
        h[3] = (int) h3;
        h[4] = (int) h4;
        h[5] = (int) h5;
        h[6] = (int) h6;
        h[7] = (int) h7;
        h[8] = (int) h8;
        h[9] = (int) h9;
        return new CafeFieldElement(h);
    }

    /**
     * Invert this field element.
     * <p>
     * The inverse is found via Fermat's little theorem:<br>
     * $a^p \cong a \mod p$ and therefore $a^{(p-2)} \cong a^{-1} \mod p$
     * </p>
     *
     * @return The inverse of this field element.
     */
    @SuppressWarnings("AlibabaUndefineMagicConstant")
    public CafeFieldElement inv() {
        CafeFieldElement t0, t1, t2, t3;

        // 2 == 2 * 1
        t0 = sqr();

        // 4 == 2 * 2
        t1 = t0.sqr();

        // 8 == 2 * 4
        t1 = t1.sqr();

        // 9 == 8 + 1
        t1 = mul(t1);

        // 11 == 9 + 2
        t0 = t0.mul(t1);

        // 22 == 2 * 11
        t2 = t0.sqr();

        // 31 == 22 + 9
        t1 = t1.mul(t2);

        // 2^6 - 2^1
        t2 = t1.sqr();

        // 2^10 - 2^5
        for (int i = 1; i < 5; ++i) {
            t2 = t2.sqr();
        }

        // 2^10 - 2^0
        t1 = t2.mul(t1);

        // 2^11 - 2^1
        t2 = t1.sqr();

        // 2^20 - 2^10
        for (int i = 1; i < 10; ++i) {
            t2 = t2.sqr();
        }

        // 2^20 - 2^0
        t2 = t2.mul(t1);

        // 2^21 - 2^1
        t3 = t2.sqr();

        // 2^40 - 2^20
        for (int i = 1; i < 20; ++i) {
            t3 = t3.sqr();
        }

        // 2^40 - 2^0
        t2 = t3.mul(t2);

        // 2^41 - 2^1
        t2 = t2.sqr();

        // 2^50 - 2^10
        for (int i = 1; i < 10; ++i) {
            t2 = t2.sqr();
        }

        // 2^50 - 2^0
        t1 = t2.mul(t1);

        // 2^51 - 2^1
        t2 = t1.sqr();

        // 2^100 - 2^50
        for (int i = 1; i < 50; ++i) {
            t2 = t2.sqr();
        }

        // 2^100 - 2^0
        t2 = t2.mul(t1);

        // 2^101 - 2^1
        t3 = t2.sqr();

        // 2^200 - 2^100
        for (int i = 1; i < 100; ++i) {
            t3 = t3.sqr();
        }

        // 2^200 - 2^0
        t2 = t3.mul(t2);

        // 2^201 - 2^1
        t2 = t2.sqr();

        // 2^250 - 2^50
        for (int i = 1; i < 50; ++i) {
            t2 = t2.sqr();
        }

        // 2^250 - 2^0
        t1 = t2.mul(t1);

        // 2^251 - 2^1
        t1 = t1.sqr();

        // 2^255 - 2^5
        for (int i = 1; i < 5; ++i) {
            t1 = t1.sqr();
        }

        // 2^255 - 21
        return t1.mul(t0);
    }

    /**
     * Raises this field element to the power $(p - 5) / 8 = 2^{252} - 3$.
     * <p>
     * Helper for {@link #sqrtRatioM1(CafeFieldElement, CafeFieldElement)}.
     * </p>
     *
     * @return $\text{this}^{(p-5)/8}$.
     */
    CafeFieldElement powPm5d8() {
        CafeFieldElement t0, t1, t2;

        // 2 == 2 * 1
        t0 = sqr();

        // 4 == 2 * 2
        t1 = t0.sqr();

        // 8 == 2 * 4
        t1 = t1.sqr();

        // z9 = z1*z8
        t1 = mul(t1);

        // 11 == 9 + 2
        t0 = t0.mul(t1);

        // 22 == 2 * 11
        t0 = t0.sqr();

        // 31 == 22 + 9
        t0 = t1.mul(t0);

        // 2^6 - 2^1
        t1 = t0.sqr();

        // 2^10 - 2^5
        for (int i = 1; i < 5; ++i) {
            t1 = t1.sqr();
        }

        // 2^10 - 2^0
        t0 = t1.mul(t0);

        // 2^11 - 2^1
        t1 = t0.sqr();

        // 2^20 - 2^10
        for (int i = 1; i < 10; ++i) {
            t1 = t1.sqr();
        }

        // 2^20 - 2^0
        t1 = t1.mul(t0);

        // 2^21 - 2^1
        t2 = t1.sqr();

        // 2^40 - 2^20
        for (int i = 1; i < 20; ++i) {
            t2 = t2.sqr();
        }

        // 2^40 - 2^0
        t1 = t2.mul(t1);

        // 2^41 - 2^1
        t1 = t1.sqr();

        // 2^50 - 2^10
        for (int i = 1; i < 10; ++i) {
            t1 = t1.sqr();
        }

        // 2^50 - 2^0
        t0 = t1.mul(t0);

        // 2^51 - 2^1
        t1 = t0.sqr();

        // 2^100 - 2^50
        for (int i = 1; i < 50; ++i) {
            t1 = t1.sqr();
        }

        // 2^100 - 2^0
        t1 = t1.mul(t0);

        // 2^101 - 2^1
        t2 = t1.sqr();

        // 2^200 - 2^100
        for (int i = 1; i < 100; ++i) {
            t2 = t2.sqr();
        }

        // 2^200 - 2^0
        t1 = t2.mul(t1);

        // 2^201 - 2^1
        t1 = t1.sqr();

        // 2^250 - 2^50
        for (int i = 1; i < 50; ++i) {
            t1 = t1.sqr();
        }

        // 2^250 - 2^0
        t0 = t1.mul(t0);

        // 2^251 - 2^1
        t0 = t0.sqr();

        // 2^252 - 2^2
        t0 = t0.sqr();

        // 2^252 - 3
        return mul(t0);
    }

    /**
     * Raises this field element to the power $(p - 1) / 2. The result is either 1, 0, or -1 depending on whether z is
     * a non-zero square, zero, or a non-square.
     * <p>
     * See https://github.com/agl/ed25519/blob/5312a6153412/extra25519/extra25519.go for more details.
     * </p>
     *
     * @return $\text{this}^{(p-1)/2}$.
     */
    CafeFieldElement chi() {
        // edwards25519.FeSquare(&t0, z)     // 2^1
        CafeFieldElement t0 = sqr();
        // edwards25519.FeMul(&t1, &t0, z)   // 2^1 + 2^0
        CafeFieldElement t1 = t0.mul(this);
        // edwards25519.FeSquare(&t0, &t1)   // 2^2 + 2^1
        t0 = t1.sqr();
        // edwards25519.FeSquare(&t2, &t0)   // 2^3 + 2^2
        CafeFieldElement t2 = t0.sqr();
        // edwards25519.FeSquare(&t2, &t2)   // 4,3
        t2 = t2.sqr();
        // edwards25519.FeMul(&t2, &t2, &t0) // 4,3,2,1
        t2 = t2.mul(t0);
        // edwards25519.FeMul(&t1, &t2, z)   // 4..0
        t1 = t2.mul(this);
        // edwards25519.FeSquare(&t2, &t1)   // 5..1
        t2 = t1.sqr();
        // 9,8,7,6,5
        for (int i = 1; i < 5; i++) {
            // edwards25519.FeSquare(&t2, &t2)
            t2 = t2.sqr();
        }
        // edwards25519.FeMul(&t1, &t2, &t1) // 9,8,7,6,5,4,3,2,1,0
        t1 = t2.mul(t1);
        // edwards25519.FeSquare(&t2, &t1)   // 10..1
        t2 = t1.sqr();
        // 19..10
        for (int i = 1; i < 10; i++) {
            // edwards25519.FeSquare(&t2, &t2)
            t2 = t2.sqr();
        }
        // edwards25519.FeMul(&t2, &t2, &t1) // 19..0
        t2 = t2.mul(t1);
        // edwards25519.FeSquare(&t3, &t2)   // 20..1
        CafeFieldElement t3 = t2.sqr();
        // 39..20
        for (int i = 1; i < 20; i++) {
            // edwards25519.FeSquare(&t3, &t3)
            t3 = t3.sqr();
        }
        // edwards25519.FeMul(&t2, &t3, &t2) // 39..0
        t2 = t3.mul(t2);
        // edwards25519.FeSquare(&t2, &t2)   // 40..1
        t2 = t2.sqr();
        // 49..10
        for (int i = 1; i < 10; i++) {
            // edwards25519.FeSquare(&t2, &t2)
            t2 = t2.sqr();
        }
        // edwards25519.FeMul(&t1, &t2, &t1) // 49..0
        t1 = t2.mul(t1);
        // edwards25519.FeSquare(&t2, &t1)   // 50..1
        t2 = t1.sqr();
        // 99..50
        for (int i = 1; i < 50; i++) {
            // edwards25519.FeSquare(&t2, &t2)
            t2 = t2.sqr();
        }
        // edwards25519.FeMul(&t2, &t2, &t1) // 99..0
        t2 = t2.mul(t1);
        // edwards25519.FeSquare(&t3, &t2)   // 100..1
        t3 = t2.sqr();
        // 199..100
        for (int i = 1; i < 100; i++) {
            // edwards25519.FeSquare(&t3, &t3)
            t3 = t3.sqr();
        }
        // edwards25519.FeMul(&t2, &t3, &t2) // 199..0
        t2 = t3.mul(t2);
        // edwards25519.FeSquare(&t2, &t2)   // 200..1
        t2 = t2.sqr();
        // 249..50
        for (int i = 1; i < 50; i++) {
            // edwards25519.FeSquare(&t2, &t2)
            t2 = t2.sqr();
        }
        // edwards25519.FeMul(&t1, &t2, &t1) // 249..0
        t1 = t2.mul(t1);
        // edwards25519.FeSquare(&t1, &t1)   // 250..1
        t1 = t1.sqr();
        // 253..4
        for (int i = 1; i < 4; i++) {
            // edwards25519.FeSquare(&t1, &t1)
            t1 = t1.sqr();
        }
        // edwards25519.FeMul(out, &t1, &t0) // 253..4,2,1
        return t1.mul(t0);
    }

    /**
     * The result of calling {@link #sqrtRatioM1(CafeFieldElement, CafeFieldElement)}.
     */
    public static class SqrtRatioM1Result {
        /**
         * <ul>
         * <li>true (1) if $v$ is non-zero and $u / v$ is square.
         * <li>true (1) if $u$ is zero.
         * <li>false (0) if $v$ is zero and $u$ is non-zero.
         * <li>false (0) if $u / v$ is non-square (so $i * u / v$ is square).
         * </ul>
         */
        final int wasSquare;
        /**
         * <ul>
         * <li>+$\sqrt{u / v}$ if $v$ is non-zero and $u / v$ is square.
         * <li>zero if $u$ is zero.
         * <li>zero if $v$ is zero and $u$ is non-zero.
         * <li>+$\sqrt{i * u / v}$ if $u / v$ is non-square (so $i * u / v$ is square).
         * </ul>
         */
        final CafeFieldElement result;

        SqrtRatioM1Result(int wasSquare, CafeFieldElement result) {
            this.wasSquare = wasSquare;
            this.result = result;
        }
    }

    /**
     * Given FieldElements $u$ and $v$, compute either $\sqrt{u / v}$ or $\sqrt{i * u / v}$ in constant time.
     * <p>
     * This function always returns the non-negative square root.
     * </p>
     *
     * @param u the numerator.
     * @param v the denominator.
     * @return <ul>
     * <li>(true, +$\sqrt{u / v}$) if $v$ is non-zero and $u / v$ is square.
     * <li>(true, zero) if $u$ is zero.
     * <li>(false, zero) if $v$ is zero and $u$ is non-zero.
     * <li>(false, +$\sqrt{i * u / v}$) if $u / v$ is non-square (so $i * u / v$ is square).
     * </ul>
     */
    static SqrtRatioM1Result sqrtRatioM1(CafeFieldElement u, CafeFieldElement v) {
        CafeFieldElement v3 = v.sqr().mul(v);
        CafeFieldElement v7 = v3.sqr().mul(v);
        CafeFieldElement r = u.mul(v3).mul(u.mul(v7).powPm5d8());
        CafeFieldElement check = v.mul(r.sqr());

        CafeFieldElement uNeg = u.neg();
        int correctSignSqrt = check.cequals(u);
        int flippedSignSqrt = check.cequals(uNeg);
        int flippedSignSqrtM1 = check.cequals(uNeg.mul(CafeConstants.SQRT_M1));

        CafeFieldElement rPrime = r.mul(CafeConstants.SQRT_M1);
        r = r.cmov(rPrime, flippedSignSqrt | flippedSignSqrtM1);

        // Choose the non-negative square root.
        r = r.abs();

        return new SqrtRatioM1Result(correctSignSqrt | flippedSignSqrt, r);
    }

    @Override
    public String toString() {
        StringBuilder ir = new StringBuilder("FieldElement([");
        for (int i = 0; i < t.length; i++) {
            if (i > 0) {
                ir.append(", ");
            }
            ir.append(t[i]);
        }
        ir.append("])");
        return ir.toString();
    }
}
