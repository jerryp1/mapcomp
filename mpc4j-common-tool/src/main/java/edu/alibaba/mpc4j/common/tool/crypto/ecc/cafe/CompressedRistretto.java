/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import java.io.*;
import java.util.Arrays;

/**
 * A Ristretto element in compressed wire format.
 * <p>
 * The Ristretto encoding is canonical, so two elements are equal if and only if
 * their encodings are equal.
 */
public class CompressedRistretto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The encoded element.
     */
    private transient byte[] data;

    public CompressedRistretto(byte[] data) {
        if (data.length != 32) {
            throw new IllegalArgumentException("Invalid CompressedRistretto encoding");
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
        throw new InvalidObjectException("Cannot deserialize CompressedRistretto from no data");
    }

    /**
     * Attempts to decompress to a RistrettoElement.
     * <p>
     * This is the ristretto255 DECODE function.
     *
     * @return a RistrettoElement, if this is the canonical encoding of an element
     *         of the ristretto255 group.
     * @throws InvalidEncodingException if this is an invalid encoding.
     */
    public RistrettoElement decompress() throws InvalidEncodingException {
        // 1. First, interpret the string as an integer s in little-endian
        // representation. If the resulting value is >= p, decoding fails.
        // 2. If IS_NEGATIVE(s) returns TRUE, decoding fails.
        final CafeFieldElement s = CafeFieldElement.decode(this.data);
        final byte[] sBytes = s.encode();
        final int sIsCanonical = CafeConstantTimeUtils.equal(this.data, sBytes);
        if (sIsCanonical == 0 || s.isNegative() == 1) {
            throw new InvalidEncodingException("Invalid ristretto255 encoding");
        }

        // 3. Process s as follows:
        final CafeFieldElement ss = s.sqr();
        final CafeFieldElement u1 = CafeFieldElement.ONE_INTS.sub(ss);
        final CafeFieldElement u2 = CafeFieldElement.ONE_INTS.add(ss);
        final CafeFieldElement u2Sqr = u2.sqr();

        final CafeFieldElement v = Constants.NEG_EDWARDS_D.mul(u1.sqr()).sub(u2Sqr);

        final CafeFieldElement.SqrtRatioM1Result invsqrt = CafeFieldElement.sqrtRatioM1(CafeFieldElement.ONE_INTS, v.mul(u2Sqr));

        final CafeFieldElement denX = invsqrt.result.mul(u2);
        final CafeFieldElement denY = invsqrt.result.mul(denX).mul(v);

        final CafeFieldElement x = s.add(s).mul(denX).abs();
        final CafeFieldElement y = u1.mul(denY);
        final CafeFieldElement t = x.mul(y);

        // 4. If was_square is FALSE, or IS_NEGATIVE(t) returns TRUE, or y = 0, decoding
        // fails. Otherwise, return the internal representation in extended coordinates
        // (x, y, 1, t).
        if (invsqrt.wasSquare == 0 || t.isNegative() == 1 || y.isZero() == 1) {
            throw new InvalidEncodingException("Invalid ristretto255 encoding");
        } else {
            return new RistrettoElement(new CafeEdwardsPoint(x, y, CafeFieldElement.ONE_INTS, t));
        }
    }

    /**
     * Encode the element to its compressed 32-byte form.
     *
     * @return the encoded element.
     */
    public byte[] toByteArray() {
        return data;
    }

    /**
     * Constant-time equality check.
     *
     * @return 1 if this and other are equal, 0 otherwise.
     */
    public int ctEquals(CompressedRistretto other) {
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
        if (!(obj instanceof CompressedRistretto)) {
            return false;
        }

        CompressedRistretto other = (CompressedRistretto) obj;
        return ctEquals(other) == 1;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
