package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.hint;

/**
 * hint for SPAM.
 *
 * @author Weiran Liu
 * @date 2023/8/30
 */
public interface SpamHint {
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
     * Expands the offset for the given chunk ID.
     *
     * @param chunkId chunk ID.
     * @return the offset of the given chunk ID.
     */
    int expandOffset(int chunkId);

    /**
     * Gets if the backup hint contains the given chunk ID.
     *
     * @param chunkId chunk ID.
     * @return return true if the backup hint contains the given chunk ID.
     */
    boolean containsChunkId(int chunkId);
}
