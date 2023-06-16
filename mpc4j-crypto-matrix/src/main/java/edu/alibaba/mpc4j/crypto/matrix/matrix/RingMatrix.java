package edu.alibaba.mpc4j.crypto.matrix.matrix;

import edu.alibaba.mpc4j.crypto.matrix.vector.RingVector;
import edu.alibaba.mpc4j.crypto.matrix.vector.Vector;

/**
 * ring matrix.
 *
 * @author Liqiang Peng
 * @date 2023/5/23
 */
public interface RingMatrix extends Matrix {

    /**
     * Get the element at position (i, j).
     *
     * @param i row index.
     * @param j col index.
     * @return element.
     */
    long get(int i, int j);

    /**
     * Set the element at position (i, j).
     *
     * @param i       row index.
     * @param j       col index.
     * @param element value.
     */
    void set(int i, int j, long element);

    /**
     * Append rows with zero elements.
     *
     * @param n row num.
     * @return Appended matrix.
     */
    RingMatrix appendZeros(int n);

    /**
     * Concat.
     *
     * @param other the other matrix.
     * @return the result.
     */
    RingMatrix concat(RingMatrix other);

    /**
     * Addition.
     *
     * @param element the element.
     */
    void add(long element);

    /**
     * Addition.
     *
     * @param other the other matrix.
     * @return the result.
     */
    RingMatrix matrixAdd(RingMatrix other);

    /**
     * Addition at position (i, j).
     *
     * @param element the element.
     * @param i       row index.
     * @param j       col index.
     */
    void addAt(long element, int i, int j);

    /**
     * Subtraction.
     *
     * @param other the other matrix.
     * @return the result.
     */
    RingMatrix matrixSub(RingMatrix other);

    /**
     * Subtraction.
     *
     * @param element the element.
     */
    void sub(long element);

    /**
     * Multiplication.
     *
     * @param other the other matrix.
     * @return the result.
     */
    RingMatrix matrixMul(RingMatrix other);

    /**
     * Multiplication.
     *
     * @param vector the vector.
     * @return the result.
     */
    RingVector matrixMulVector(RingVector vector);

    /**
     * Transposition.
     *
     * @return the result.
     */
    RingMatrix transpose();

    /**
     * Decompose the matrix base on p.
     *
     * @param p the modulo.
     * @return decomposed matrix.
     */
    RingMatrix decompose(long p);

    /**
     * Recompose the matrix base on p.
     *
     * @param p the modulo.
     * @return recomposed matrix.
     */
    RingMatrix recompose(long p);
}
