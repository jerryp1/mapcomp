package edu.alibaba.mpc4j.crypto.matrix.database;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * database interface.
 *
 * @author Weiran Liu
 * @date 2023/4/4
 */
public interface Database {
    /**
     * Gets number of rows.
     *
     * @return number of rows.
     */
    int rows();

    /**
     * Gets number of columns (in bits).
     *
     * @return number of columns (in bits).
     */
    int getL();

    /**
     * Gets number of columns (in bytes).
     *
     * @return number of columns (in bytes).
     */
    int getByteL();

    /**
     * Partitions the bytes vector by columns.
     *
     * @param envType  the environment.
     * @param parallel parallel operation.
     * @return the partition result.
     */
    BitVector[] partition(EnvType envType, boolean parallel);

    /**
     * Splits the database with the split rows.
     *
     * @param splitRows the split rows.
     * @return a new database with the split rows.
     */
    Database split(int splitRows);

    /**
     * Reduces the database to the reduced rows.
     *
     * @param reduceRows the reduced rows.
     */
    void reduce(int reduceRows);

    /**
     * Merges two databases.
     *
     * @param other the other database.
     */
    void merge(Database other);
}
