package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * dense matrix.
 *
 * @author Weiran Liu
 * @date 2022/8/1
 */
public interface DenseBitMatrix {
    /**
     * Adds a matrix.
     *
     * @param that that matrix.
     * @return 相加结果。
     */
    DenseBitMatrix add(DenseBitMatrix that);

    /**
     * Inplace adds a matrix.
     *
     * @param that that matrix.
     */
    void addi(DenseBitMatrix that);

    /**
     * Multiplies a matrix.
     *
     * @param that that matrix.
     * @return result.
     */
    DenseBitMatrix multiply(DenseBitMatrix that);

    /**
     * Left-multiplies a vector v, i.e., v·M.
     *
     * @param v the vector v.
     * @return v·M.
     */
    byte[] lmul(final byte[] v);

    /**
     * Left-multiplies a vector v, i.e., v·M.
     *
     * @param v the vector v.
     * @return v·M.
     */
    boolean[] lmul(final boolean[] v);

    /**
     * Left-multiplies an extended vector v, i.e., v·M.
     *
     * @param v an extended vector v.
     * @return v·M。
     */
    byte[][] lExtMul(final byte[][] v);

    /**
     * Computes v·M + t, and sets the result into t.
     *
     * @param v the vector v.
     * @param t the vector t.
     */
    void lmulAddi(final byte[] v, byte[] t);

    /**
     * Computes v·M + t, and sets the result into t.
     *
     * @param v the vector v.
     * @param t the vector t.
     */
    void lmulAddi(final boolean[] v, boolean[] t);

    /**
     * Computes v·M + t, and sets the result into t.
     *
     * @param v an extended vector v.
     * @param t an extended vector t.
     */
    void lExtMulAddi(final byte[][] v, byte[][] t);

    /**
     * Transposes a matrix.
     *
     * @param envType  environment.
     * @param parallel parallel operation.
     * @return result.
     */
    DenseBitMatrix transpose(EnvType envType, boolean parallel);

    /**
     * Inverses the matrix.
     *
     * @return the inverse matrix.
     */
    DenseBitMatrix inverse();

    /**
     * Gets the number of rows.
     *
     * @return the number of rows.
     */
    int getRows();

    /**
     * Gets the assigned byte array row.
     *
     * @param iRow row index.
     * @return the assigned byte array row.
     */
    byte[] getByteArrayRow(int iRow);

    /**
     * Gets the assigned long array row.
     *
     * @param iRow row index.
     * @return the assigned long array row.
     */
    long[] getLongArrayRow(int iRow);

    /**
     * Gets the number of columns.
     *
     * @return the number of columns.
     */
    int getColumns();

    /**
     * Gets the size. Note that only square matrix support this.
     *
     * @return size.
     * @throws IllegalArgumentException if the matrix is not square.
     */
    int getSize();

    /**
     * Gets the size in byte. Note that only square matrix support this.
     *
     * @return size.
     * @throws IllegalArgumentException if the matrix is not square.
     */
    int getByteSize();

    /**
     * Gets the size in long. Note that only square matrix support this.
     *
     * @return size.
     * @throws IllegalArgumentException if the matrix is not square.
     */
    int getLongSize();

    /**
     * Gets the entry at (iRow, iColumn).
     *
     * @param x row index.
     * @param y column index.
     * @return the entry at (iRow, iColumn).
     */
    boolean get(int x, int y);

    /**
     * Gets the byte array data.
     *
     * @return the byte array data.
     */
    byte[][] getByteArrayData();

    /**
     * Gets the long array data.
     *
     * @return the long array data.
     */
    long[][] getLongArrayData();
}
