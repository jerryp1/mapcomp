package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint;

/**
 * ZPSZ23 hint.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public interface PianoHint {
    /**
     * Gets chunk size.
     *
     * @return chunk size.
     */
    int getChunkSize();

    /**
     * Gets chunk num.
     *
     * @return chunk num.
     */
    int getChunkNum();

    /**
     * Gets parity bit length.
     *
     * @return parity bit length.
     */
    int getL();

    /**
     * Gets parity byte length.
     *
     * @return parity byte length.
     */
    int getByteL();

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

    /**
     * Expands the offset for the given chunk ID.
     *
     * @param chunkId chunk ID.
     * @return the offset of the given chunk ID.
     */
    int expandOffset(int chunkId);

    /**
     * Expands x for the given chunk ID.
     *
     * @param chunkId chunk ID.
     * @return x for the given chunk ID.
     */
    default int expand(int chunkId) {
        return expandOffset(chunkId) + chunkId * getChunkSize();
    }
}
