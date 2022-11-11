/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import java.util.Arrays;

/**
 * An element of the prime-order ristretto255 group. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/RistrettoElement.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public class CafeRistrettoPoint {
    /**
     * identity
     */
    public static final CafeRistrettoPoint IDENTITY = new CafeRistrettoPoint(CafeEdwardsPoint.IDENTITY);

    /**
     * The internal representation. Not canonical.
     */
    final CafeEdwardsPoint repr;

    /**
     * Only for internal use.
     */
    CafeRistrettoPoint(CafeEdwardsPoint repr) {
        this.repr = repr;
    }

    /**
     * The function MAP(t) from section 3.2.4 of the ristretto255 ID.
     *
     * @param t a field element.
     * @return encoded Ristretto element.
     */
    static CafeRistrettoPoint map(final CafeFieldElement t) {
        final CafeFieldElement r = t.sqr().mul(CafeConstants.SQRT_M1);
        final CafeFieldElement u = r.add(CafeFieldElement.ONE_INTS).mul(CafeConstants.ONE_MINUS_D_SQ);
        final CafeFieldElement v = CafeFieldElement.MINUS_ONE_INTS.sub(r.mul(CafeConstants.EDWARDS_D))
            .mul(r.add(CafeConstants.EDWARDS_D));

        final CafeFieldElement.SqrtRatioM1Result sqrt = CafeFieldElement.sqrtRatioM1(u, v);
        CafeFieldElement s = sqrt.result;

        final CafeFieldElement sPrime = s.mul(t).abs().neg();
        s = sPrime.cmov(s, sqrt.wasSquare);
        final CafeFieldElement c = r.cmov(CafeFieldElement.MINUS_ONE_INTS, sqrt.wasSquare);

        final CafeFieldElement N = c.mul(r.sub(CafeFieldElement.ONE_INTS)).mul(CafeConstants.D_MINUS_ONE_SQ).sub(v);
        final CafeFieldElement sSq = s.sqr();

        final CafeFieldElement w0 = s.add(s).mul(v);
        final CafeFieldElement w1 = N.mul(CafeConstants.SQRT_AD_MINUS_ONE);
        final CafeFieldElement w2 = CafeFieldElement.ONE_INTS.sub(sSq);
        final CafeFieldElement w3 = CafeFieldElement.ONE_INTS.add(sSq);

        return new CafeRistrettoPoint(new CafeEdwardsPoint(w0.mul(w3), w2.mul(w1), w1.mul(w3), w0.mul(w2)));
    }

    /**
     * Construct a ristretto255 element from a uniformly-distributed 64-byte string. This is the ristretto255
     * FROM_UNIFORM_BYTES function.
     *
     * @param b a uniformly-distributed 64-byte string.
     * @return the resulting element.
     */
    public static CafeRistrettoPoint fromUniformBytes(final byte[] b) {
        // 1. Interpret the least significant 255 bits of b[ 0..32] as an
        // integer r0 in little-endian representation. Reduce r0 modulo p.
        final byte[] b0 = Arrays.copyOfRange(b, 0, CafeFieldElement.FIELD_BYTE_SIZE);
        final CafeFieldElement r0 = CafeFieldElement.decode(b0);

        // 2. Interpret the least significant 255 bits of b[32..64] as an
        // integer r1 in little-endian representation. Reduce r1 modulo p.
        final byte[] b1 = Arrays.copyOfRange(b, CafeFieldElement.FIELD_BYTE_SIZE, CafeFieldElement.FIELD_BYTE_SIZE * 2);
        final CafeFieldElement r1 = CafeFieldElement.decode(b1);

        // 3. Compute group element P1 as MAP(r0)
        final CafeRistrettoPoint point1 = CafeRistrettoPoint.map(r0);

        // 4. Compute group element P2 as MAP(r1).
        final CafeRistrettoPoint point2 = CafeRistrettoPoint.map(r1);

        // 5. Return the group element P1 + P2.
        return point1.add(point2);
    }

    /**
     * Compress this element using the Ristretto encoding. This is the ristretto255 ENCODE function.
     *
     * @return the encoded element.
     */
    public CafeRistrettoCompressedPoint compress() {
        // 1. Process the internal representation into a field element s as follows:
        final CafeFieldElement u1 = repr.z.add(repr.y).mul(repr.z.sub(repr.y));
        final CafeFieldElement u2 = repr.x.mul(repr.y);

        // Ignore was_square since this is always square
        final CafeFieldElement.SqrtRatioM1Result invsqrt
            = CafeFieldElement.sqrtRatioM1(CafeFieldElement.ONE_INTS, u1.mul(u2.sqr()));

        final CafeFieldElement den1 = invsqrt.result.mul(u1);
        final CafeFieldElement den2 = invsqrt.result.mul(u2);
        final CafeFieldElement zInv = den1.mul(den2).mul(repr.t);

        final CafeFieldElement ix = repr.x.mul(CafeConstants.SQRT_M1);
        final CafeFieldElement iy = repr.y.mul(CafeConstants.SQRT_M1);
        final CafeFieldElement enchantedDenominator = den1.mul(CafeConstants.INVSQRT_A_MINUS_D);

        final int rotate = repr.t.mul(zInv).isNegative();

        final CafeFieldElement x = repr.x.cmov(iy, rotate);
        CafeFieldElement y = repr.y.cmov(ix, rotate);
        final CafeFieldElement z = repr.z;
        final CafeFieldElement denInv = den2.cmov(enchantedDenominator, rotate);

        y = y.cmov(y.neg(), x.mul(zInv).isNegative());

        CafeFieldElement s = denInv.mul(z.sub(y));
        final int sIsNegative = s.isNegative();
        s = s.cmov(s.neg(), sIsNegative);

        // 2. Return the canonical little-endian encoding of s.
        return new CafeRistrettoCompressedPoint(s.encode());
    }

    /**
     * Constant-time equality check. This is the ristretto255 EQUALS function.
     *
     * @param other the other Ristretto element.
     * @return 1 if this and other are equal, 0 otherwise.
     */
    public int cequals(final CafeRistrettoPoint other) {
        CafeFieldElement x1y2 = repr.x.mul(other.repr.y);
        CafeFieldElement y1x2 = repr.y.mul(other.repr.x);
        CafeFieldElement y1y2 = repr.y.mul(other.repr.y);
        CafeFieldElement x1x2 = repr.x.mul(other.repr.x);
        return x1y2.cequals(y1x2) | y1y2.cequals(x1x2);
    }

    /**
     * Constant-time selection between two RistrettoElements.
     *
     * @param that the other element.
     * @param c    must be 0 or 1, otherwise results are undefined.
     * @return a copy of this if $c == 0$, or a copy of that if $c == 1$.
     */
    public CafeRistrettoPoint cmov(final CafeRistrettoPoint that, final int c) {
        return new CafeRistrettoPoint(this.repr.cmove(that.repr, c));
    }

    /**
     * Equality check overridden to be constant-time. Fails fast if the objects are of different types.
     *
     * @return true if this and other are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CafeRistrettoPoint)) {
            return false;
        }

        CafeRistrettoPoint other = (CafeRistrettoPoint) obj;
        return cequals(other) == 1;
    }

    @Override
    public int hashCode() {
        // The general contract for the hashCode method states that equal objects must
        // have equal hash codes. Object equality is based on the encodings of the
        // elements, not their internal representations (which are not canonical). Note
        // that equality is actually implemented using the ristretto255 EQUALS function,
        // but it is simpler to derive a hashCode from the element's encoding.
        return compress().hashCode();
    }

    /**
     * Group addition.
     *
     * @param q the element to add to this one.
     * @return $P + Q$.
     */
    public CafeRistrettoPoint add(final CafeRistrettoPoint q) {
        return new CafeRistrettoPoint(repr.add(q.repr));
    }

    /**
     * Group subtraction.
     *
     * @param q the element to subtract from this one.
     * @return $P - Q$.
     */
    public CafeRistrettoPoint sub(final CafeRistrettoPoint q) {
        return new CafeRistrettoPoint(repr.sub(q.repr));
    }

    /**
     * Element negation.
     *
     * @return $-P$.
     */
    public CafeRistrettoPoint neg() {
        return new CafeRistrettoPoint(repr.neg());
    }

    /**
     * Element doubling.
     *
     * @return $[2]P$
     */
    public CafeRistrettoPoint dbl() {
        return new CafeRistrettoPoint(repr.dbl());
    }

    /**
     * Constant-time variable-base scalar multiplication.
     *
     * @param s the Scalar to multiply by.
     * @return $[s]P$.
     */
    public CafeRistrettoPoint mul(final CafeScalar s) {
        return new CafeRistrettoPoint(repr.mul(s));
    }

    @Override
    public String toString() {
        return "RistrettoElement(" + repr.toString() + ")";
    }
}
