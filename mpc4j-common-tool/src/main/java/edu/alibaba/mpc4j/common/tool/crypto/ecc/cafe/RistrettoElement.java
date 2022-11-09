/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import java.io.*;
import java.util.Arrays;

/**
 * An element of the prime-order ristretto255 group.
 */
public class RistrettoElement implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final RistrettoElement IDENTITY = new RistrettoElement(CafeEdwardsPoint.IDENTITY);

    /**
     * The internal representation. Not canonical.
     */
    transient CafeEdwardsPoint repr;

    /**
     * Only for internal use.
     */
    RistrettoElement(CafeEdwardsPoint repr) {
        this.repr = repr;
    }

    /**
     * Overrides class serialization to use the canonical encoded format.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.write(this.compress().toByteArray());
    }

    /**
     * Overrides class serialization to use the canonical encoded format.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        byte[] encoded = new byte[32];
        in.readFully(encoded);

        try {
            RistrettoElement elem = new CompressedRistretto(encoded).decompress();
            this.repr = elem.repr;
        } catch (InvalidEncodingException iee) {
            throw new InvalidObjectException(iee.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private void readObjectNoData() throws ObjectStreamException {
        throw new InvalidObjectException("Cannot deserialize RistrettoElement from no data");
    }

    /**
     * The function MAP(t) from section 3.2.4 of the ristretto255 ID.
     */
    static RistrettoElement map(final CafeFieldElement t) {
        final CafeFieldElement r = t.sqr().mul(Constants.SQRT_M1);
        final CafeFieldElement u = r.add(CafeFieldElement.ONE_INTS).mul(Constants.ONE_MINUS_D_SQ);
        final CafeFieldElement v = CafeFieldElement.MINUS_ONE_INTS.sub(r.mul(Constants.EDWARDS_D))
                .mul(r.add(Constants.EDWARDS_D));

        final CafeFieldElement.SqrtRatioM1Result sqrt = CafeFieldElement.sqrtRatioM1(u, v);
        CafeFieldElement s = sqrt.result;

        final CafeFieldElement sPrime = s.mul(t).abs().neg();
        s = sPrime.cmov(s, sqrt.wasSquare);
        final CafeFieldElement c = r.cmov(CafeFieldElement.MINUS_ONE_INTS, sqrt.wasSquare);

        final CafeFieldElement N = c.mul(r.sub(CafeFieldElement.ONE_INTS)).mul(Constants.D_MINUS_ONE_SQ).sub(v);
        final CafeFieldElement sSq = s.sqr();

        final CafeFieldElement w0 = s.add(s).mul(v);
        final CafeFieldElement w1 = N.mul(Constants.SQRT_AD_MINUS_ONE);
        final CafeFieldElement w2 = CafeFieldElement.ONE_INTS.sub(sSq);
        final CafeFieldElement w3 = CafeFieldElement.ONE_INTS.add(sSq);

        return new RistrettoElement(
                new CafeEdwardsPoint(w0.mul(w3), w2.mul(w1), w1.mul(w3), w0.mul(w2)));
    }

    /**
     * Construct a ristretto255 element from a uniformly-distributed 64-byte string.
     * <p>
     * This is the ristretto255 FROM_UNIFORM_BYTES function.
     *
     * @return the resulting element.
     */
    public static RistrettoElement fromUniformBytes(final byte[] b) {
        // 1. Interpret the least significant 255 bits of b[ 0..32] as an
        // integer r0 in little-endian representation. Reduce r0 modulo p.
        final byte[] b0 = Arrays.copyOfRange(b, 0, 32);
        final CafeFieldElement r0 = CafeFieldElement.decode(b0);

        // 2. Interpret the least significant 255 bits of b[32..64] as an
        // integer r1 in little-endian representation. Reduce r1 modulo p.
        final byte[] b1 = Arrays.copyOfRange(b, 32, 64);
        final CafeFieldElement r1 = CafeFieldElement.decode(b1);

        // 3. Compute group element P1 as MAP(r0)
        final RistrettoElement P1 = RistrettoElement.map(r0);

        // 4. Compute group element P2 as MAP(r1).
        final RistrettoElement P2 = RistrettoElement.map(r1);

        // 5. Return the group element P1 + P2.
        return P1.add(P2);
    }

    /**
     * Compress this element using the Ristretto encoding.
     * <p>
     * This is the ristretto255 ENCODE function.
     *
     * @return the encoded element.
     */
    public CompressedRistretto compress() {
        // 1. Process the internal representation into a field element s as follows:
        final CafeFieldElement u1 = this.repr.z.add(this.repr.y).mul(this.repr.z.sub(this.repr.y));
        final CafeFieldElement u2 = this.repr.x.mul(this.repr.y);

        // Ignore was_square since this is always square
        final CafeFieldElement.SqrtRatioM1Result invsqrt = CafeFieldElement.sqrtRatioM1(CafeFieldElement.ONE_INTS,
                u1.mul(u2.sqr()));

        final CafeFieldElement den1 = invsqrt.result.mul(u1);
        final CafeFieldElement den2 = invsqrt.result.mul(u2);
        final CafeFieldElement zInv = den1.mul(den2).mul(this.repr.t);

        final CafeFieldElement ix = this.repr.x.mul(Constants.SQRT_M1);
        final CafeFieldElement iy = this.repr.y.mul(Constants.SQRT_M1);
        final CafeFieldElement enchantedDenominator = den1.mul(Constants.INVSQRT_A_MINUS_D);

        final int rotate = this.repr.t.mul(zInv).isNegative();

        final CafeFieldElement x = this.repr.x.cmov(iy, rotate);
        CafeFieldElement y = this.repr.y.cmov(ix, rotate);
        final CafeFieldElement z = this.repr.z;
        final CafeFieldElement denInv = den2.cmov(enchantedDenominator, rotate);

        y = y.cmov(y.neg(), x.mul(zInv).isNegative());

        CafeFieldElement s = denInv.mul(z.sub(y));
        final int sIsNegative = s.isNegative();
        s = s.cmov(s.neg(), sIsNegative);

        // 2. Return the canonical little-endian encoding of s.
        return new CompressedRistretto(s.encode());
    }

    /**
     * Constant-time equality check.
     * <p>
     * This is the ristretto255 EQUALS function.
     *
     * @return 1 if this and other are equal, 0 otherwise.
     */
    public int ctEquals(final RistrettoElement other) {
        CafeFieldElement X1Y2 = this.repr.x.mul(other.repr.y);
        CafeFieldElement Y1X2 = this.repr.y.mul(other.repr.x);
        CafeFieldElement Y1Y2 = this.repr.y.mul(other.repr.y);
        CafeFieldElement X1X2 = this.repr.x.mul(other.repr.x);
        return X1Y2.cequals(Y1X2) | Y1Y2.cequals(X1X2);
    }

    /**
     * Constant-time selection between two RistrettoElements.
     *
     * @param that the other element.
     * @param b    must be 0 or 1, otherwise results are undefined.
     * @return a copy of this if $b == 0$, or a copy of that if $b == 1$.
     */
    public RistrettoElement ctSelect(final RistrettoElement that, final int b) {
        return new RistrettoElement(this.repr.cmove(that.repr, b));
    }

    /**
     * Equality check overridden to be constant-time.
     * <p>
     * Fails fast if the objects are of different types.
     *
     * @return true if this and other are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RistrettoElement)) {
            return false;
        }

        RistrettoElement other = (RistrettoElement) obj;
        return ctEquals(other) == 1;
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
     * @param Q the element to add to this one.
     * @return $P + Q$
     */
    public RistrettoElement add(final RistrettoElement Q) {
        return new RistrettoElement(this.repr.add(Q.repr));
    }

    /**
     * Group subtraction.
     *
     * @param Q the element to subtract from this one.
     * @return $P - Q$
     */
    public RistrettoElement subtract(final RistrettoElement Q) {
        return new RistrettoElement(this.repr.sub(Q.repr));
    }

    /**
     * Element negation.
     *
     * @return $-P$
     */
    public RistrettoElement negate() {
        return new RistrettoElement(this.repr.neg());
    }

    /**
     * Element doubling.
     *
     * @return $[2]P$
     */
    public RistrettoElement dbl() {
        return new RistrettoElement(this.repr.dbl());
    }

    /**
     * Constant-time variable-base scalar multiplication.
     *
     * @param s the Scalar to multiply by.
     * @return $[s]P$
     */
    public RistrettoElement multiply(final CafeScalar s) {
        return new RistrettoElement(this.repr.mul(s));
    }

    @Override
    public String toString() {
        return "RistrettoElement(" + repr.toString() + ")";
    }
}
