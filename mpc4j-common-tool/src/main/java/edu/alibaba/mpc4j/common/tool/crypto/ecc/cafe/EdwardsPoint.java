/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import java.io.*;

/**
 * An EdwardsPoint represents a point on the Edwards form of Curve25519.
 */
public class EdwardsPoint implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final EdwardsPoint IDENTITY = new EdwardsPoint(CafeFieldElement.ZERO_INTS, CafeFieldElement.ONE_INTS, CafeFieldElement.ONE_INTS,
            CafeFieldElement.ZERO_INTS);

    transient CafeFieldElement X;
    transient CafeFieldElement Y;
    transient CafeFieldElement Z;
    transient CafeFieldElement T;

    /**
     * Only for internal use.
     */
    EdwardsPoint(CafeFieldElement X, CafeFieldElement Y, CafeFieldElement Z, CafeFieldElement T) {
        this.X = X;
        this.Y = Y;
        this.Z = Z;
        this.T = T;
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
            EdwardsPoint point = new CompressedEdwardsY(encoded).decompress();
            this.X = point.X;
            this.Y = point.Y;
            this.Z = point.Z;
            this.T = point.T;
        } catch (InvalidEncodingException iee) {
            throw new InvalidObjectException(iee.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private void readObjectNoData() throws ObjectStreamException {
        throw new InvalidObjectException("Cannot deserialize EdwardsPoint from no data");
    }

    /**
     * Compress this point to CompressedEdwardsY format.
     *
     * @return the encoded point.
     */
    public CompressedEdwardsY compress() {
        CafeFieldElement recip = this.Z.inv();
        CafeFieldElement x = this.X.mul(recip);
        CafeFieldElement y = this.Y.mul(recip);
        byte[] s = y.encode();
        s[31] |= (x.isNegative() << 7);
        return new CompressedEdwardsY(s);
    }

    /**
     * Constant-time equality check.
     * <p>
     * Compares the encodings of the two EdwardsPoints.
     *
     * @return 1 if this and other are equal, 0 otherwise.
     */
    public int ctEquals(EdwardsPoint other) {
        return compress().ctEquals(other.compress());
    }

    /**
     * Constant-time selection between two EdwardsPoints.
     *
     * @param that the other point.
     * @param b    must be 0 or 1, otherwise results are undefined.
     * @return a copy of this if $b == 0$, or a copy of that if $b == 1$.
     */
    public EdwardsPoint ctSelect(EdwardsPoint that, int b) {
        return new EdwardsPoint(this.X.cmov(that.X, b), this.Y.cmov(that.Y, b), this.Z.cmov(that.Z, b),
                this.T.cmov(that.T, b));
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
        if (!(obj instanceof EdwardsPoint)) {
            return false;
        }

        EdwardsPoint other = (EdwardsPoint) obj;
        return ctEquals(other) == 1;
    }

    @Override
    public int hashCode() {
        // The general contract for the hashCode method states that equal objects must
        // have equal hash codes. Object equality is based on the encodings of the
        // points, not their internal representations (which may not be canonical).
        return compress().hashCode();
    }

    /**
     * Convert the representation of this point from extended coordinates to
     * projective coordinates.
     * <p>
     * Free.
     */
    ProjectivePoint toProjective() {
        return new ProjectivePoint(this.X, this.Y, this.Z);
    }

    /**
     * Convert to a ProjectiveNielsPoint.
     */
    ProjectiveNielsPoint toProjectiveNiels() {
        return new ProjectiveNielsPoint(this.Y.add(this.X), this.Y.sub(this.X), this.Z,
                this.T.mul(Constants.EDWARDS_2D));
    }

    /**
     * Dehomogenize to an AffineNielsPoint.
     */
    AffineNielsPoint toAffineNiels() {
        CafeFieldElement recip = this.Z.inv();
        CafeFieldElement x = this.X.mul(recip);
        CafeFieldElement y = this.Y.mul(recip);
        CafeFieldElement xy2D = x.mul(y).mul(Constants.EDWARDS_2D);
        return new AffineNielsPoint(y.add(x), y.sub(x), xy2D);
    }

    /**
     * Point addition.
     *
     * @param Q the point to add to this one.
     * @return $P + Q$
     */
    public EdwardsPoint add(EdwardsPoint Q) {
        return this.add(Q.toProjectiveNiels()).toExtended();
    }

    /**
     * Point addition.
     *
     * @param Q the point to add to this one, in projective "Niels coordinates".
     * @return $P + Q$
     */
    CompletedPoint add(ProjectiveNielsPoint Q) {
        CafeFieldElement YPlusX = this.Y.add(this.X);
        CafeFieldElement YMinusX = this.Y.sub(this.X);
        CafeFieldElement PP = YPlusX.mul(Q.YPlusX);
        CafeFieldElement MM = YMinusX.mul(Q.YMinusX);
        CafeFieldElement TT2D = this.T.mul(Q.T2D);
        CafeFieldElement ZZ = this.Z.mul(Q.Z);
        CafeFieldElement ZZ2 = ZZ.add(ZZ);
        return new CompletedPoint(PP.sub(MM), PP.add(MM), ZZ2.add(TT2D), ZZ2.sub(TT2D));
    }

    /**
     * Point addition.
     *
     * @param q the point to add to this one, in affine "Niels coordinates".
     * @return $P + q$
     */
    CompletedPoint add(AffineNielsPoint q) {
        CafeFieldElement YPlusX = this.Y.add(this.X);
        CafeFieldElement YMinusX = this.Y.sub(this.X);
        CafeFieldElement PP = YPlusX.mul(q.yPlusx);
        CafeFieldElement MM = YMinusX.mul(q.yMinusx);
        CafeFieldElement Txy2D = this.T.mul(q.xy2D);
        CafeFieldElement Z2 = this.Z.add(this.Z);
        return new CompletedPoint(PP.sub(MM), PP.add(MM), Z2.add(Txy2D), Z2.sub(Txy2D));
    }

    /**
     * Point subtraction.
     *
     * @param Q the point to subtract from this one.
     * @return $P - Q$
     */
    public EdwardsPoint subtract(EdwardsPoint Q) {
        return this.subtract(Q.toProjectiveNiels()).toExtended();
    }

    /**
     * Point subtraction.
     *
     * @param Q the point to subtract from this one, in projective "Niels
     *          coordinates".
     * @return $P - Q$
     */
    CompletedPoint subtract(ProjectiveNielsPoint Q) {
        CafeFieldElement YPlusX = this.Y.add(this.X);
        CafeFieldElement YMinusX = this.Y.sub(this.X);
        CafeFieldElement PM = YPlusX.mul(Q.YMinusX);
        CafeFieldElement MP = YMinusX.mul(Q.YPlusX);
        CafeFieldElement TT2D = this.T.mul(Q.T2D);
        CafeFieldElement ZZ = Z.mul(Q.Z);
        CafeFieldElement ZZ2 = ZZ.add(ZZ);
        return new CompletedPoint(PM.sub(MP), PM.add(MP), ZZ2.sub(TT2D), ZZ2.add(TT2D));
    }

    /**
     * Point subtraction.
     *
     * @param q the point to subtract from this one, in affine "Niels coordinates".
     * @return $P - q$
     */
    CompletedPoint subtract(AffineNielsPoint q) {
        CafeFieldElement YPlusX = this.Y.add(this.X);
        CafeFieldElement YMinusX = this.Y.sub(this.X);
        CafeFieldElement PM = YPlusX.mul(q.yMinusx);
        CafeFieldElement MP = YMinusX.mul(q.yPlusx);
        CafeFieldElement Txy2D = this.T.mul(q.xy2D);
        CafeFieldElement Z2 = this.Z.add(this.Z);
        return new CompletedPoint(PM.sub(MP), PM.add(MP), Z2.sub(Txy2D), Z2.add(Txy2D));
    }

    /**
     * Point negation.
     *
     * @return $-P$
     */
    public EdwardsPoint negate() {
        return new EdwardsPoint(this.X.negate(), this.Y, this.Z, this.T.negate());
    }

    /**
     * Point doubling.
     *
     * @return $[2]P$
     */
    public EdwardsPoint dbl() {
        return this.toProjective().dbl().toExtended();
    }

    /**
     * Constant-time variable-base scalar multiplication.
     *
     * @param s the Scalar to multiply by.
     * @return $[s]P$
     */
    public EdwardsPoint multiply(final CafeScalar s) {
        // Construct a lookup table of [P,2P,3P,4P,5P,6P,7P,8P]
        final ProjectiveNielsPoint.LookupTable lookupTable = ProjectiveNielsPoint.buildLookupTable(this);

        // Compute
        //
        // s = s_0 + s_1*16^1 + ... + s_63*16^63,
        //
        // with -8 ≤ s_i < 8 for 0 ≤ i < 63 and -8 ≤ s_63 ≤ 8.
        final byte[] e = s.toRadix16();

        // Compute s*P as
        //
        // @formatter:off
        //    s*P = P*(s_0 +   s_1*16^1 +   s_2*16^2 + ... +   s_63*16^63)
        //    s*P =  P*s_0 + P*s_1*16^1 + P*s_2*16^2 + ... + P*s_63*16^63
        //    s*P = P*s_0 + 16*(P*s_1 + 16*(P*s_2 + 16*( ... + P*s_63)...))
        // @formatter:on
        //
        // We sum right-to-left.
        EdwardsPoint Q = EdwardsPoint.IDENTITY;
        for (int i = 63; i >= 0; i--) {
            Q = Q.multiplyByPow2(4);
            Q = Q.add(lookupTable.select(e[i])).toExtended();
        }
        return Q;
    }

    /**
     * Compute $r = [a]A + [b]B$ in variable time, where $B$ is the Ed25519
     * basepoint.
     *
     * @param a a Scalar.
     * @param A an EdwardsPoint.
     * @param b a Scalar.
     * @return $[a]A + [b]B$
     */
    public static EdwardsPoint vartimeDoubleScalarMultiplyBasepoint(final CafeScalar a, final EdwardsPoint A,
                                                                    final CafeScalar b) {
        final byte[] aNaf = a.nonAdjacentForm();
        final byte[] bNaf = b.nonAdjacentForm();

        ProjectiveNielsPoint.NafLookupTable tableA = ProjectiveNielsPoint.buildNafLookupTable(A);
        AffineNielsPoint.NafLookupTable tableB = Constants.AFFINE_ODD_MULTIPLES_OF_BASEPOINT;

        int i;
        for (i = 255; i >= 0; --i) {
            if (aNaf[i] != 0 || bNaf[i] != 0) {
                break;
            }
        }

        ProjectivePoint r = EdwardsPoint.IDENTITY.toProjective();
        for (; i >= 0; --i) {
            CompletedPoint t = r.dbl();

            if (aNaf[i] > 0) {
                t = t.toExtended().add(tableA.select(aNaf[i]));
            } else if (aNaf[i] < 0) {
                t = t.toExtended().subtract(tableA.select(-aNaf[i]));
            }

            if (bNaf[i] > 0) {
                t = t.toExtended().add(tableB.select(bNaf[i]));
            } else if (bNaf[i] < 0) {
                t = t.toExtended().subtract(tableB.select(-bNaf[i]));
            }

            r = t.toProjective();
        }

        return r.toExtended();
    }

    /**
     * Multiply by the cofactor.
     *
     * @return $[8]P$
     */
    public EdwardsPoint multiplyByCofactor() {
        return this.multiplyByPow2(3);
    }

    /**
     * Compute $[2^k]P$ by successive doublings.
     *
     * @param k the exponent of 2. Must be positive and non-zero.
     * @return $[2^k]P$
     */
    EdwardsPoint multiplyByPow2(int k) {
        if (!(k > 0)) {
            throw new IllegalArgumentException("Exponent must be positive and non-zero");
        }
        ProjectivePoint s = this.toProjective();
        for (int i = 0; i < k - 1; i++) {
            s = s.dbl().toProjective();
        }
        // Unroll last doubling so we can go directly to extended coordinates.
        return s.dbl().toExtended();
    }

    /**
     * Determine if this point is the identity.
     *
     * @return true if this point is the identity, false otherwise.
     */
    public boolean isIdentity() {
        return this.ctEquals(EdwardsPoint.IDENTITY) == 1;
    }

    /**
     * Determine if this point is in the 8-torsion subgroup $(\mathcal E[8])$, and
     * therefore of small order.
     *
     * @return true if this point is of small order, false otherwise.
     */
    public boolean isSmallOrder() {
        return this.multiplyByCofactor().isIdentity();
    }

    /**
     * Determine if this point is contained in the prime-order subgroup $(\mathcal
     * E[\ell])$, and has no torsion component.
     *
     * @return true if this point has zero torsion component and is in the
     *         prime-order subgroup, false otherwise.
     */
    public boolean isTorsionFree() {
        return this.multiply(Constants.BASEPOINT_ORDER).isIdentity();
    }

    @Override
    public String toString() {
        String ir = "EdwardsPoint(\n";
        ir += "    X: " + X.toString() + ",\n";
        ir += "    Y: " + Y.toString() + ",\n";
        ir += "    Z: " + Z.toString() + ",\n";
        ir += "    T: " + T.toString() + ",\n";
        ir += ")";
        return ir;
    }
}
