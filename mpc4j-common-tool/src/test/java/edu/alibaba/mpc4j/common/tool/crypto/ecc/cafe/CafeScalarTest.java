/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import org.bouncycastle.util.encoders.Hex;

/**
 * Scalar unit test. Modified from:
 * <p>
 * https://github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/test/java/cafe/cryptography/curve25519/ScalarTest.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/10/20
 */
public class CafeScalarTest {
    /**
     * x = 2238329342913194256032495932344128051776374960164957527413114840482143558222
     */
    static final byte[] X = Hex.decode("4e5ab4345d4708845913b4641bc27d5252a585101bcc4244d449f4a879d9f204");
    /**
     * 1 / x = 6859937278830797291664592131120606308688036382723378951768035303146619657244
     */
    static final byte[] X_INV = Hex.decode("1cdc17fce0e9a5bbd9247e56bb016347bbba31edd5a9bb96d50bcd7a3f962a0f");
    /**
     * y = 2592331292931086675770238855846338635550719849568364935475441891787804997264
     */
    static final byte[] Y = Hex.decode("907633fe1c4b66a4a28d2dd7678386c353d0de5455d4fc9de8ef7ac31f35bb05");
    /**
     * x * y = 5690045403673944803228348699031245560686958845067437804563560795922180092780
     */
    static final byte[] X_MUL_Y = Hex.decode("6c3374a1894f62210aaa2fe186a6f92ce0aa75c2779581c295fc08179a73940c");
    /**
     * sage: l = 2^252 + 27742317777372353535851937790883648493 sage: big = 2^256 - 1 sage: repr((big % l).digits(256))
     */
    static final byte[] CANONICAL_2_256_MINUS_1 = Hex.decode("1c95988d7431ecd670cf7d73f45befc6feffffffffffffffffffffffffffff0f");
    /**
     * a
     */
    static final byte[] A_SCALAR = Hex.decode("1a0e978a90f6622d3747023f8ad8264da758aa1b88e040d1589e7b7f2376ef09");
    /**
     * A_NAF
     */
    static final byte[] A_NAF = new byte[] { 0, 13, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, -9, 0, 0, 0, 0, -11, 0, 0,
        0, 0, 3, 0, 0, 0, 0, 1, 0, 0, 0, 0, 9, 0, 0, 0, 0, -5, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 11, 0, 0, 0, 0, 11,
        0, 0, 0, 0, 0, -9, 0, 0, 0, 0, 0, -3, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0,
        9, 0, 0, 0, 0, -15, 0, 0, 0, 0, -7, 0, 0, 0, 0, -9, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 13, 0, 0, 0, 0, 0, -3, 0,
        0, 0, 0, -11, 0, 0, 0, 0, -7, 0, 0, 0, 0, -13, 0, 0, 0, 0, 11, 0, 0, 0, 0, -9, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
        0, -15, 0, 0, 0, 0, 1, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 13, 0, 0, 0, 0, 0, 0, 11, 0,
        0, 0, 0, 0, 15, 0, 0, 0, 0, 0, -9, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, -15, 0,
        0, 0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 };

    /**
     * Example from RFC 8032 test case 1
     */
    static final byte[] TV1_R_INPUT = Hex.decode(
        "b6b19cd8e0426f5983fa112d89a143aa97dab8bc5deb8d5b6253c928b65272f4044098c2a990039cde5b6a4818df0bfb6e40dc5dee54248032962323e701352d"
    );
    static final byte[] TV1_R = Hex.decode("f38907308c893deaf244787db4af53682249107418afc2edc58f75ac58a07404");
    static final byte[] TV1_H = Hex.decode("86eabc8e4c96193d290504e7c600df6cf8d8256131ec2c138a3e7e162e525404");
    static final byte[] TV1_A = Hex.decode("307c83864f2833cb427a2ef1c00a013cfdff2768d980c0a3a520f006904de94f");
    static final byte[] TV1_S = Hex.decode("5fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b");
}
