package edu.alibaba.mpc4j.s2pc.aby.basics;

/**
 * secret-shared vector.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public interface ShareVector {
    /**
     * Whether the share vector is in plain state.
     *
     * @return the share vector is in plain state.
     */
    boolean isPlain();

    /**
     * Copies the share vector.
     *
     * @return the copied share vector.
     */
    ShareVector copy();

    /**
     * Gets the number of shares in the share vector.
     *
     * @return the number of shares in the share vector.
     */
    int getNum();

    /**
     * Splits a share vector with the given num. The current share vector keeps the remaining shares.
     *
     * @param splitNum the split num.
     * @return the split share vector.
     */
    ShareVector split(int splitNum);

    /**
     * Reduce the share vector with the given num.
     *
     * @param reduceNum the reduced num.
     */
    void reduce(int reduceNum);

    /**
     * Merge the other share vector.
     *
     * @param other the other share vector.
     */
    void merge(ShareVector other);
}
