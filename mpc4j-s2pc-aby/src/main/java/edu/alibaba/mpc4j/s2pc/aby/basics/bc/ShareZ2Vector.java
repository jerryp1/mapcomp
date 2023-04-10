package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.ShareVector;

/**
 * share Z2 vector.
 *
 * @author Weiran Liu
 * @date 2022/12/16
 */
public interface ShareZ2Vector extends ShareVector {
    /**
     * Gets the num in bytes.
     *
     * @return the num in bytes.
     */
    int getByteNum();

    /**
     * Replaces the Z2 vector.
     *
     * @param bitVector the Z2 vector.
     * @param plain     the plain state.
     */
    void replaceCopy(BitVector bitVector, boolean plain);

    /**
     * Get the inner bit vector.
     *
     * @return the inner bit vector.
     */
    BitVector getBitVector();

    /**
     * Get the value at the index.
     *
     * @param index the index.
     * @return the value at the index.
     */
    boolean get(int index);

    /**
     * Get the share Z2 vector represented by bytes.
     *
     * @return the share Z2 vector represented by bytes.
     */
    default byte[] getBytes() {
        return getBitVector().getBytes();
    }

    /**
     * XOR.
     *
     * @param that  the other share Z2 vector.
     * @param plain the result plain state.
     * @return the result.
     */
    ShareZ2Vector xor(ShareZ2Vector that, boolean plain);

    /**
     * In-place XOR.
     *
     * @param that  the other share Z2 vector.
     * @param plain the result plain state.
     */
    void xori(ShareZ2Vector that, boolean plain);

    /**
     * AND.
     *
     * @param that the other share Z2 vector.
     * @return the result.
     */
    ShareZ2Vector and(ShareZ2Vector that);

    /**
     * In-place AND.
     *
     * @param that the other share Z2 vector.
     */
    void andi(ShareZ2Vector that);

    /**
     * OR.
     *
     * @param that the other share Z2 vector.
     * @return the result.
     */
    ShareZ2Vector or(ShareZ2Vector that);

    /**
     * In-place OR.
     *
     * @param that the other share Z2 vector.
     */
    void ori(ShareZ2Vector that);

    /**
     * NOT.
     *
     * @return the result.
     */
    ShareZ2Vector not();

    /**
     * In-place NOT.
     */
    void noti();
}
