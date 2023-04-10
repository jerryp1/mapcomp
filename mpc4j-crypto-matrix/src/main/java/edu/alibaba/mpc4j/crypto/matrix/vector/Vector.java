package edu.alibaba.mpc4j.crypto.matrix.vector;

/**
 * the vector interface.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public interface Vector {
    /**
     * Copies the vector.
     *
     * @return the copied vector.
     */
    Vector copy();

    /**
     * Gets the number of elements in the vector.
     *
     * @return the number of elements in the vector.
     */
    int getNum();

    /**
     * Splits a vector with the given num. The current vector keeps the remaining elements.
     *
     * @param splitNum the split num.
     * @return the split vector.
     */
    Vector split(int splitNum);

    /**
     * Reduce the vector with the given num.
     *
     * @param reduceNum the reduced num.
     */
    void reduce(int reduceNum);

    /**
     * Merge the other vector.
     *
     * @param that the other vector.
     */
    void merge(Vector that);
}
