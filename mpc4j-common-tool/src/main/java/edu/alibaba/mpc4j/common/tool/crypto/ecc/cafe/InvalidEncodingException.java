/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * Thrown to indicate that a {@link CompressedEdwardsY} or
 * {@link CompressedRistretto} was an invalid encoding of an
 * {@link EdwardsPoint} or {@link RistrettoElement}.
 */
public class InvalidEncodingException extends Exception {
    private static final long serialVersionUID = 1L;

    InvalidEncodingException(String msg) {
        super(msg);
    }
}
