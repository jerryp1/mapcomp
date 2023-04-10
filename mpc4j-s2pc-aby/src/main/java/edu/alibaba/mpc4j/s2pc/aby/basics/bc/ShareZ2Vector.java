package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.ShareVector;

/**
 * Secret-shared bit vector.
 *
 * @author Weiran Liu
 * @date 2022/12/16
 */
public interface ShareZ2Vector extends ShareVector {
    /**
     * Get the number of bytes in the share bit vector.
     *
     * @return the number of bytes in the share bit vector.
     */
    int getByteNum();

    /**
     * Replace the bit vector.
     *
     * @param bitVector the bit vector.
     * @param plain if the share bit vector is in plain state.
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
     * Get the share bit vector represented by bytes.
     *
     * @return the share bit vector represented by bytes.
     */
    default byte[] getBytes() {
        return getBitVector().getBytes();
    }

    /**
     * XOR operation.
     *
     * @param that the other share bit vector.
     * @param plain the result plain state.
     * @return the XOR result.
     */
    ShareZ2Vector xor(ShareZ2Vector that, boolean plain);

    /**
     * Inner XOR operation.
     *
     * @param that the other share bit vector.
     * @param plain the result plain state.
     */
    void xori(ShareZ2Vector that, boolean plain);

    /**
     * AND operation.
     *
     * @param that the other share bit vector.
     * @return the AND result.
     */
    ShareZ2Vector and(ShareZ2Vector that);

    /**
     * Inner AND operation.
     *
     * @param that the other share bit vector.
     */
    void andi(ShareZ2Vector that);

    /**
     * OR operation.
     *
     * @param that the other share bit vector.
     * @return the OR result.
     */
    ShareZ2Vector or(ShareZ2Vector that);

    /**
     * Inner OR operation.
     *
     * @param that the other share bit vector.
     */
    void ori(ShareZ2Vector that);

    /**
     * NOT operation.
     *
     * @return the NOT result.
     */
    ShareZ2Vector not();

    /**
     * Inner NOT operation.
     */
    void noti();
}
