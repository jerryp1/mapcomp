package edu.alibaba.mpc4j.s2pc.pcg;

/**
 * merge party output.
 *
 * @author Weiran Liu
 * @date 2023/3/13
 */
public interface MergePartyOutput {
    /**
     * Splits the output with the split num.
     *
     * @param splitNum the split num.
     * @return a new output with the split num.
     */
    MergePartyOutput split(int splitNum);

    /**
     * Reduces the output to the reduced num.
     *
     * @param reduceNum the reduced num.
     */
    void reduce(int reduceNum);

    /**
     * Merges two outputs.
     *
     * @param other the other output.
     */
    void merge(MergePartyOutput other);

    /**
     * Gets num.
     *
     * @return num.
     */
    int getNum();
}
