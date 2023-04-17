package edu.alibaba.mpc4j.s2pc.aby.basics.ac;

import edu.alibaba.mpc4j.crypto.matrix.vector.RingVector;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.ShareVector;

/**
 * Secret-shared ring vector.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public interface ShareRingVector extends ShareVector {
    /**
     * Replace the vector.
     *
     * @param vector the vector.
     * @param plain  the plain state.
     */
    void replaceCopy(RingVector vector, boolean plain);

    /**
     * Addition.
     *
     * @param other the other vector.
     * @param plain the result plain state.
     * @return the result.
     */
    ShareRingVector add(ShareRingVector other, boolean plain);

    /**
     * In-place addition.
     *
     * @param other the other vector.
     * @param plain the result plain state.
     */
    void addi(ShareRingVector other, boolean plain);

    /**
     * Negation.
     *
     * @param plain the result plain state.
     * @return the result.
     */
    ShareRingVector neg(boolean plain);

    /**
     * In-place negation.
     *
     * @param plain the result plain state.
     */
    void negi(boolean plain);

    /**
     * Subtraction.
     *
     * @param other the other vector.
     * @param plain the result plain state.
     * @return the result.
     */
    ShareRingVector sub(ShareRingVector other, boolean plain);

    /**
     * In-place subtraction.
     *
     * @param other the other vector.
     * @param plain the result plain state.
     */
    void subi(ShareRingVector other, boolean plain);

    /**
     * Multiplication.
     *
     * @param other the other vector.
     * @return the result.
     */
    ShareRingVector mul(ShareRingVector other);

    /**
     * In-place multiplication.
     *
     * @param other the other vector.
     */
    void muli(ShareRingVector other);

    /**
     * Gets the vector.
     *
     * @return the vector.
     */
    RingVector getVector();
}
