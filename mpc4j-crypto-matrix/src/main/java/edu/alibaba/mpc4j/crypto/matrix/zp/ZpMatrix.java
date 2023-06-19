package edu.alibaba.mpc4j.crypto.matrix.zp;

import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;

import java.math.BigInteger;

/**
 * Zp matrix.
 *
 * @author Weiran Liu
 * @date 2023/6/19
 */
public interface ZpMatrix {
    /**
     * Gets type.
     *
     * @return type.
     */
    ZpMatrixFactory.ZpMatrixType getType();

    /**
     * Copies the matrix.
     *
     * @return the copied matrix.
     */
    DenseZpMatrix copy();

    /**
     * Adds a matrix.
     *
     * @param that that matrix.
     * @return result.
     */
    ZpMatrix add(ZpMatrix that);

    /**
     * Multiplies a matrix.
     *
     * @param that that matrix.
     * @return result.
     */
    ZpMatrix multiply(ZpMatrix that);

    /**
     * Transposes the matrix.
     *
     * @return result.
     */
    ZpMatrix transpose();

    /**
     * Inverses the matrix.
     *
     * @return inversed matrix.
     */
    ZpMatrix inverse();

    /**
     * Gets Zp instance.
     *
     * @return Zp instance.
     */
    Zp getZp();

    /**
     * Gets the number of rows.
     *
     * @return the number of row.s
     */
    int getRows();

    /**
     * Gets the assigned row.
     *
     * @param iRow row index.
     * @return the assigned row.
     */
    BigInteger[] getRow(int iRow);

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
     * Gets the element at (iRow, iColumn).
     *
     * @param iRow    row index.
     * @param iColumn column index.
     * @return the element at (iRow, iColumn).
     */
    BigInteger getEntry(int iRow, int iColumn);

    /**
     * Gets the data.
     *
     * @return the data.
     */
    BigInteger[][] getData();
}
