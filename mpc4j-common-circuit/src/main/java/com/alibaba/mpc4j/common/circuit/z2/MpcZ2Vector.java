package com.alibaba.mpc4j.common.circuit.z2;

import com.alibaba.mpc4j.common.circuit.MpcVector;

/**
 * Mpc Bit Vector.
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/20
 */
public interface MpcZ2Vector extends MpcVector {

    /**
     * Gets the num in bytes.
     *
     * @return the num in bytes.
     */
    int getByteNum();


    /**
     * Get the value at the index.
     *
     * @param index the index.
     * @return the value at the index.
     */
    boolean get(int index);

    /**
     * XOR.
     *
     * @param that  the other share Z2 vector.
     * @param plain the result plain state.
     * @return the result.
     */
    MpcZ2Vector xor(MpcZ2Vector that, boolean plain);

    /**
     * In-place XOR.
     *
     * @param that  the other share Z2 vector.
     * @param plain the result plain state.
     */
    void xori(MpcZ2Vector that, boolean plain);

    /**
     * AND.
     *
     * @param that the other share Z2 vector.
     * @return the result.
     */
    MpcZ2Vector and(MpcZ2Vector that);

    /**
     * In-place AND.
     *
     * @param that the other share Z2 vector.
     */
    void andi(MpcZ2Vector that);

    /**
     * OR.
     *
     * @param that the other share Z2 vector.
     * @return the result.
     */
    MpcZ2Vector or(MpcZ2Vector that);

    /**
     * In-place OR.
     *
     * @param that the other share Z2 vector.
     */
    void ori(MpcZ2Vector that);

    /**
     * NOT.
     *
     * @return the result.
     */
    MpcZ2Vector not();

    /**
     * In-place NOT.
     */
    void noti();

    /**
     * Get type of MpcZ2Vector.
     *
     * @return type.
     */
    MpcZ2Type getType();

}
