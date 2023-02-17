package edu.alibaba.mpc4j.common.tool.galoisfield.zl;

import edu.alibaba.mpc4j.common.tool.galoisfield.BigIntegerRing;

import java.math.BigInteger;

/**
 * The Zl interface. All operations are done module 2^l.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
public interface Zl extends BigIntegerRing {
    /**
     * Gets the Zl type.
     *
     * @return the Zl type.
     */
    ZlFactory.ZlType getZlType();

    /**
     * Computes a mod p.
     *
     * @param a the input a.
     * @return a mod p.
     */
    BigInteger module(final BigInteger a);
}
