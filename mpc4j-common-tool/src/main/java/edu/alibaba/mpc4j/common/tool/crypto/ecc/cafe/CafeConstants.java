/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * Various constants and useful parameters. Modified from:
 * <p>
 * https://github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/main/java/cafe/cryptography/curve25519/Constants.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/10/20
 */
public final class CafeConstants {
    /**
     * The order of the Ed25519 base point, $\ell = 2^{252} + 27742317777372353535851937790883648493$.
     */
    static final byte[] L_BYTES_SCALAR = new byte[]{
        (byte) 0xed, (byte) 0xd3, (byte) 0xf5, (byte) 0x5c, (byte) 0x1a, (byte) 0x63, (byte) 0x12, (byte) 0x58,
        (byte) 0xd6, (byte) 0x9c, (byte) 0xf7, (byte) 0xa2, (byte) 0xde, (byte) 0xf9, (byte) 0xde, (byte) 0x14,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10,
    };
    /**
     * The unpacked form of the Ed25519 base point order $\ell$.
     */
    static final int[] L_INTS_SCALAR = CafeUnpackScalar.decode(L_BYTES_SCALAR);
    /**
     * $\ell * \text{LFACTOR} = -1 \bmod 2^{29}$
     */
    static final int L_FACTOR = 0x12547e1b;
    /**
     * $= R \bmod \ell$ where $R = 2^{261}$
     */
    static final int[] R = new int[]{
        0x114df9ed, 0x1a617303, 0x0f7c098c, 0x16793167, 0x1ffd656e, 0x1fffffff, 0x1fffffff, 0x1fffffff, 0x000fffff,
    };
    /**
     * $= R^2 \bmod \ell$ where $R = 2^{261}$
     */
    static final int[] RR = new int[]{
        0x0b5f9d12, 0x1e141b17, 0x158d7f3d, 0x143f3757, 0x1972d781, 0x042feb7c, 0x1ceec73d, 0x1e184d1e, 0x0005046d,
    };

    /**
     * Precomputed value of one of the square roots of -1 (mod p).
     */
    static final int[] SQRT_M1 = new int[]{
        // @formatter:off
        -32595792, -7943725, 9377950, 3500415, 12389472,
        -272473, -25146209, -2005654, 326686, 11406482,
        // @formatter:on
    };
}
