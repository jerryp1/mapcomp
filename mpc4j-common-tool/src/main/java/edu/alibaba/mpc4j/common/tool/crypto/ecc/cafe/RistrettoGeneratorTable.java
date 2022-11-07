/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

/**
 * A pre-computed table of multiples of a Ristretto generator, for accelerating
 * fixed-base scalar multiplication.
 */
public class RistrettoGeneratorTable {
    final EdwardsBasepointTable table;

    /**
     * Create a table of pre-computed multiples of generator.
     */
    public RistrettoGeneratorTable(final RistrettoElement generator) {
        this.table = new EdwardsBasepointTable(generator.repr);
    }

    /**
     * Constant-time fixed-base scalar multiplication.
     *
     * @param s the Scalar to multiply by.
     * @return $[s]B$
     */
    public RistrettoElement multiply(final CafeScalar s) {
        return new RistrettoElement(table.multiply(s));
    }
}
