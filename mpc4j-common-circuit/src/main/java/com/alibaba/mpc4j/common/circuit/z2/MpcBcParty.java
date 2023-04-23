package com.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Mpc Boolean Circuit Party.
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/20
 */
public interface MpcBcParty {
    /**
     * AND operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z0 ⊕ z1 = z = x & y = (x0 ⊕ x1) & (y0 ⊕ y1).
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector and(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector AND operations.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z0[j] ⊕ z1[j] = z[j] = x[j] & y[j] = (x0[j] ⊕ x1[j]) & (y0[j] ⊕ y1[j]).
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] and(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * XOR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z0 ⊕ z1 = z = x ^ y = (x0 ⊕ x1) ^ (y0 ⊕ y1).
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector xor(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector XOR operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z0[j] ⊕ z1[j] = z[j] = x[j] ^ y[j] = (x0[j] ⊕ x1[j]) ^ (y0[j] ⊕ y1[j]).
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] xor(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * OR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z0 ⊕ z1 = z = x | y = (x0 ⊕ x1) | (y0 ⊕ y1).
     * @throws MpcAbortException if the protocol is abort.
     */
    default MpcZ2Vector or(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException {
        return xor(xor(xi, yi), and(xi, yi));
    }


    /**
     * Vector OR operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z0[j] ⊕ z1[j] = z[j] = x[j] | y[j] = (x0[j] ⊕ x1[j]) | (y0[j] ⊕ y1[j]).
     * @throws MpcAbortException if the protocol is abort.
     */
    default MpcZ2Vector[] or(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        return xor(xor(xiArray, yiArray), and(xiArray, yiArray));
    }

    /**
     * NOT operation.
     *
     * @param xi xi.
     * @return zi, such that z0 ⊕ z1 = z = !x = !(x0 ⊕ x1).
     * @throws MpcAbortException if the protocol is abort.
     */
    default MpcZ2Vector not(MpcZ2Vector xi) throws MpcAbortException {
        return xor(xi, MpcZ2VectorFactory.createOnes(xi.getType(), xi.getNum()));
    }

    /**
     * Vector NOT operation.
     *
     * @param xiArray xi array.
     * @return zi array, such that for each j, z0[j] ⊕ z1[j] = z[j] = !x[j] = !(x0[j] ⊕ x1[j]).
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] not(MpcZ2Vector[] xiArray) throws MpcAbortException;


    /**
     * Get type of MpcZ2Vector.
     *
     * @return type.
     */
    MpcZ2Type getType();
}
