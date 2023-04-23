package com.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Mpc Boolean Circuit Party.
 *
 * @author Li Peng
 * @date 2023/4/20
 */
public interface MpcBcParty {
    /**
     * Create a (plain) all-one vector.
     *
     * @param bitNum the bit num.
     * @return a vector.
     */
    MpcZ2Vector createOnes(int bitNum);

    /**
     * Create a (plain) all-zero vector.
     *
     * @param bitNum the bit num.
     * @return a vector.
     */
    MpcZ2Vector createZeros(int bitNum);

    /**
     * Create a (plain) vector with all bits equal to the assigned value.
     *
     * @param bitNum the bit num.
     * @param value  the assigned value.
     * @return a vector.
     */
    MpcZ2Vector create(int bitNum, boolean value);

    /**
     * AND operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x & y.
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector and(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector AND operations.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] & y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] and(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * XOR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x ^ y.
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector xor(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector XOR operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] ^ y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] xor(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * OR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x | y.
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
     * @return zi array, such that for each j, z[i] = z[i] | y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    default MpcZ2Vector[] or(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        return xor(xor(xiArray, yiArray), and(xiArray, yiArray));
    }

    /**
     * NOT operation.
     *
     * @param xi xi.
     * @return zi, such that z = !x.
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector not(MpcZ2Vector xi) throws MpcAbortException;

    /**
     * Vector NOT operation.
     *
     * @param xiArray xi array.
     * @return zi array, such that for each j, z[i] = !x[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    MpcZ2Vector[] not(MpcZ2Vector[] xiArray) throws MpcAbortException;
}
