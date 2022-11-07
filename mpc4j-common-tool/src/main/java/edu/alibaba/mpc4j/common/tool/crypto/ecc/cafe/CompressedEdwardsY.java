/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import java.io.*;
import java.util.Arrays;

/**
 * An Edwards point encoded in "Edwards y" / "Ed25519" format.
 * <p>
 * In "Edwards y" / "Ed25519" format, the curve point $(x, y)$ is determined by
 * the $y$-coordinate and the sign of $x$.
 * <p>
 * The first 255 bits of a CompressedEdwardsY represent the $y$-coordinate. The
 * high bit of the 32nd byte represents the sign of $x$.
 */
public class CompressedEdwardsY implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The encoded point.
     */
    private transient byte[] data;

    public CompressedEdwardsY(byte[] data) {
        if (data.length != 32) {
            throw new IllegalArgumentException("Invalid CompressedEdwardsY encoding");
        }
        this.data = data;
    }

    /**
     * Overrides class serialization to use the canonical encoded format.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.write(this.toByteArray());
    }

    /**
     * Overrides class serialization to use the canonical encoded format.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        byte[] encoded = new byte[32];
        in.readFully(encoded);
        this.data = encoded;
    }

    @SuppressWarnings("unused")
    private void readObjectNoData() throws ObjectStreamException {
        throw new InvalidObjectException("Cannot deserialize CompressedEdwardsY from no data");
    }

    /**
     * Attempts to decompress to an EdwardsPoint.
     *
     * @return an EdwardsPoint, if this is a valid encoding.
     * @throws InvalidEncodingException if this is an invalid encoding.
     */
    public EdwardsPoint decompress() throws InvalidEncodingException {
        CafeFieldElement Y = CafeFieldElement.decode(data);
        CafeFieldElement YY = Y.sqr();

        // u = y²-1
        CafeFieldElement u = YY.sub(CafeFieldElement.ONE_INTS);

        // v = dy²+1
        CafeFieldElement v = YY.mul(Constants.EDWARDS_D).add(CafeFieldElement.ONE_INTS);

        CafeFieldElement.SqrtRatioM1Result sqrt = CafeFieldElement.sqrtRatioM1(u, v);
        if (sqrt.wasSquare != 1) {
            throw new InvalidEncodingException("not a valid EdwardsPoint");
        }

        CafeFieldElement X = sqrt.result.negate().cmov(sqrt.result,
                CafeConstantTimeUtils.equal(sqrt.result.isNegative(), CafeConstantTimeUtils.bit(data, 255)));

        return new EdwardsPoint(X, Y, CafeFieldElement.ONE_INTS, X.mul(Y));
    }

    /**
     * Encode the point to its compressed 32-byte form.
     *
     * @return the encoded point.
     */
    public byte[] toByteArray() {
        return data;
    }

    /**
     * Constant-time equality check.
     *
     * @return 1 if this and other are equal, 0 otherwise.
     */
    public int ctEquals(CompressedEdwardsY other) {
        return CafeConstantTimeUtils.equal(data, other.data);
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
        if (!(obj instanceof CompressedEdwardsY)) {
            return false;
        }

        CompressedEdwardsY other = (CompressedEdwardsY) obj;
        return ctEquals(other) == 1;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
