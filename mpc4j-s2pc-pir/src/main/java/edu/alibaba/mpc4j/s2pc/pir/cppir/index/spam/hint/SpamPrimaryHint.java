package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.hint;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * primary hint for SPAM.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public interface SpamPrimaryHint extends SpamHint {
    /**
     * Expands all offsets for all chunks. The total number of indexes are chunkNum.
     *
     * @return all offsets for all chunks.
     */
    default int[] expandOffset() {
        int chunkNum = getChunkNum();
        int[] offsets = new int[chunkNum];
        for (int chunkId = 0; chunkId < chunkNum; chunkId++) {
            offsets[chunkId] = expandOffset(chunkId);
        }
        return offsets;
    }

    /**
     * Gets if the given x is in the indexes.
     *
     * @param x the given x.
     * @return true if the given x is in the indexes.
     */
    default boolean contains(int x) {
        int chunkNum = getChunkNum();
        int chunkSize = getChunkSize();
        MathPreconditions.checkNonNegativeInRange("x", x, chunkNum * chunkSize);
        // compute chunk ID and offset
        int chunkId = x / chunkSize;
        int offset = Math.abs(x % chunkSize);
        int expandOffset = expandOffset(chunkId);

        return offset == expandOffset;
    }

    /**
     * Gets the parity.
     *
     * @return the parity.
     */
    byte[] getParity();

    /**
     * Inplace XOR the current parity with the other parity.
     *
     * @param otherParity other parity.
     */
    void xori(byte[] otherParity);
}
