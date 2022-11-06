/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class EdwardsBasepointTableTest {
    @Test
    public void scalarMulVsEd25519py() {
        EdwardsBasepointTable Bt = new EdwardsBasepointTable(Constants.ED25519_BASEPOINT);
        EdwardsPoint aB = Bt.multiply(EdwardsPointTest.A_SCALAR);
        assertThat(aB.compress(), Matchers.is(EdwardsPointTest.A_TIMES_BASEPOINT));
    }
}
