/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import java.util.Arrays;

/**
 * A Ristretto element in compressed wire format. Modified from:
 * <p>
 * github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/CompressedRistretto.java
 * </p>
 * The Ristretto encoding is canonical, so two elements are equal if and only if their encodings are equal.
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public class CafeRistrettoCompressedPoint {
    /**
     * The byte size in compressed form
     */
    public static final int BYTE_SIZE = 32;
    /**
     * The encoded element.
     */
    private final byte[] data;

    public CafeRistrettoCompressedPoint(byte[] data) {
        if (data.length != BYTE_SIZE) {
            throw new IllegalArgumentException("Invalid CompressedRistretto encoding");
        }
        this.data = data;
    }

    /**
     * Attempts to decompress to a RistrettoElement. This is the ristretto255 DECODE function.
     *
     * @return a RistrettoElement, if this is the canonical encoding of an element of the ristretto255 group.
     * @throws IllegalArgumentException if this is not the canonical encoding of an element of the ristretto255 group.
     */
    public CafeRistrettoPoint decompress() {
        // 1. First, interpret the string as an integer s in little-endian representation.
        //    If the resulting value is >= p, decoding fails.
        // 2. If IS_NEGATIVE(s) returns TRUE, decoding fails.
        final CafeFieldElement s = CafeFieldElement.decode(data);
        final byte[] sBytes = s.encode();
        final int sIsCanonical = CafeConstantTimeUtils.equal(data, sBytes);
        if (sIsCanonical == 0 || s.isNegative() == 1) {
            throw new IllegalArgumentException("Invalid ristretto255 encoding");
        }

        // 3. Process s as follows:
        final CafeFieldElement ss = s.sqr();
        // u1 = 1 - s^2
        final CafeFieldElement u1 = CafeFieldElement.ONE_INTS.sub(ss);
        // u2 = 1 + s^2
        final CafeFieldElement u2 = CafeFieldElement.ONE_INTS.add(ss);
        final CafeFieldElement u2Sqr = u2.sqr();

        final CafeFieldElement v = CafeConstants.NEG_EDWARDS_D.mul(u1.sqr()).sub(u2Sqr);

        final CafeFieldElement.SqrtRatioM1Result invsqrt = CafeFieldElement.sqrtRatioM1(CafeFieldElement.ONE_INTS, v.mul(u2Sqr));

        final CafeFieldElement denX = invsqrt.result.mul(u2);
        final CafeFieldElement denY = invsqrt.result.mul(denX).mul(v);

        final CafeFieldElement x = s.add(s).mul(denX).abs();
        final CafeFieldElement y = u1.mul(denY);
        final CafeFieldElement t = x.mul(y);

        // 4. If was_square is FALSE, or IS_NEGATIVE(t) returns TRUE, or y = 0, decoding fails.
        //    Otherwise, return the internal representation in extended coordinates (x, y, 1, t).
        if (invsqrt.wasSquare == 0 || t.isNegative() == 1 || y.isZero() == 1) {
            throw new IllegalArgumentException("Invalid ristretto255 encoding");
        } else {
            return new CafeRistrettoPoint(new CafeEdwardsPoint(x, y, CafeFieldElement.ONE_INTS, t));
        }
    }

    /**
     * Encode the element to its compressed 32-byte form.
     *
     * @return the encoded element.
     */
    public byte[] encode() {
        return data;
    }

    /**
     * Constant-time equality check.
     *
     * @return 1 if this and other are equal, 0 otherwise.
     */
    public int cequals(CafeRistrettoCompressedPoint other) {
        return CafeConstantTimeUtils.equal(data, other.data);
    }

    /**
     * Equality check overridden to be constant-time. Fails fast if the objects are of different types.
     *
     * @return true if this and other are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CafeRistrettoCompressedPoint)) {
            return false;
        }

        CafeRistrettoCompressedPoint other = (CafeRistrettoCompressedPoint) obj;
        return cequals(other) == 1;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
