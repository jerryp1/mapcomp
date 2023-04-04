package edu.alibaba.mpc4j.crypto.matrix.database;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.ByteDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Zl byte array database. Each data is an element in Z_{2^l} represented by byte[] where l > 0.
 *
 * @author Weiran Liu
 * @date 2023/3/31
 */
public class ZlByteArrayDatabase implements Database {
    /**
     * display data rows
     */
    private static final int DISPLAY_DATA_ROWS = 256;
    /**
     * number of columns (in bit)
     */
    private final int l;
    /**
     * number of columns (in byte)
     */
    private final int byteL;
    /**
     * data
     */
    private byte[][] data;

    /**
     * Creates a database.
     *
     * @param l    number of columns.
     * @param data data.
     * @return a database.
     */
    public static ZlByteArrayDatabase create(int l, byte[][] data) {
        ZlByteArrayDatabase database = new ZlByteArrayDatabase(l);
        database.data = Arrays.stream(data)
            .peek(bytes -> Preconditions.checkArgument(
                BytesUtils.isFixedReduceByteArray(bytes, database.byteL, database.l)
            ))
            .toArray(byte[][]::new);
        return database;
    }

    /**
     * Creates a random database.
     *
     * @param l            number of columns.
     * @param rows         number of rows.
     * @param secureRandom the random state.
     * @return a database.
     */
    public static ZlByteArrayDatabase createRandom(int l, int rows, SecureRandom secureRandom) {
        ZlByteArrayDatabase database = new ZlByteArrayDatabase(l);
        MathPreconditions.checkPositive("rows", rows);
        database.data = IntStream.range(0, rows)
            .mapToObj(index -> BytesUtils.randomByteArray(database.byteL, database.l, secureRandom))
            .toArray(byte[][]::new);
        return database;
    }

    /**
     * Creates a database by combining databases.
     *
     * @param l         number of columns.
     * @param databases the combining databases.
     * @return a bytes vector.
     */
    public static ZlByteArrayDatabase create(int l, ZlByteArrayDatabase... databases) {
        // check BitVectors.length > 0
        MathPreconditions.checkPositive("databases.length", databases.length);
        int rows = databases[0].rows();
        // check all databases have the same rows
        Arrays.stream(databases).forEach(database ->
            MathPreconditions.checkEqual("rows", "database.rows", rows, database.rows())
        );
        // combine each database
        BigInteger[] bigIntegerData = new BigInteger[rows];
        Arrays.fill(bigIntegerData, BigInteger.ZERO);
        for (ZlByteArrayDatabase database : databases) {
            for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
                BigInteger partitionBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(database.data[rowIndex]);
                bigIntegerData[rowIndex] = bigIntegerData[rowIndex]
                    .shiftLeft(database.l)
                    .add(partitionBigInteger);
            }
        }
        // verify that all combined vectors has at most upper-bound bit length
        Arrays.stream(bigIntegerData).forEach(bigInteger -> Preconditions.checkArgument(bigInteger.bitLength() <= l));
        int byteL = CommonUtils.getByteLength(l);
        byte[][] data = Arrays.stream(bigIntegerData)
            .map(bigIntegerElement -> BigIntegerUtils.nonNegBigIntegerToByteArray(bigIntegerElement, byteL))
            .toArray(byte[][]::new);

        return ZlByteArrayDatabase.create(l, data);
    }

    /**
     * Creates a database by combining bit vectors.
     *
     * @param envType    the environment.
     * @param parallel   parallel combination.
     * @param bitVectors the combining bit vectors.
     * @return a database.
     */
    public static ZlByteArrayDatabase create(EnvType envType, boolean parallel, BitVector... bitVectors) {
        MathPreconditions.checkPositive("BitVectors.length", bitVectors.length);
        int l = bitVectors.length;
        int rows = bitVectors[0].bitNum();
        // check all bit vectors has the same bit num
        Arrays.stream(bitVectors).forEach(bitVector ->
            MathPreconditions.checkEqual("rows", "BitVector.bitNum", rows, bitVector.bitNum())
        );
        TransBitMatrix bitMatrix = TransBitMatrixFactory.createInstance(envType, rows, l, parallel);
        for (int columnIndex = 0; columnIndex < l; columnIndex++) {
            bitMatrix.setColumn(columnIndex, bitVectors[columnIndex].getBytes());
        }
        TransBitMatrix transBitMatrix = bitMatrix.transpose();
        byte[][] data = IntStream.range(0, rows)
            .mapToObj(transBitMatrix::getColumn)
            .toArray(byte[][]::new);

        return create(l, data);
    }

    /**
     * Creates an empty database.
     *
     * @param l number of columns.
     * @return a database.
     */
    public static ZlByteArrayDatabase createEmpty(int l) {
        ZlByteArrayDatabase database = new ZlByteArrayDatabase(l);
        database.data = new byte[0][];

        return database;
    }

    private ZlByteArrayDatabase(int l) {
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
    }

    @Override
    public int rows() {
        return data.length;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int getByteL() {
        return byteL;
    }

    /**
     * Partitions the bytes vector by the assigned partition bit length. Note that each bit length of the partitioned
     * bytes vector is the assigned partition bit length. For example, when the current bit length is 3, and the
     * partition bit length is 9, then we create 1 partition bytes vector with bit length 9 (byte length 2), but all
     * first byte in the partitioned bytes vector are 0.
     *
     * @param partitionL the partition bit length.
     * @return the partition result.
     */
    public ZlByteArrayDatabase[] partition(int partitionL) {
        MathPreconditions.checkPositive("partitionL", partitionL);
        int partitionNum = CommonUtils.getUnitNum(l, partitionL);
        ZlByteArrayDatabase[] partitionDatabases = new ZlByteArrayDatabase[partitionNum];
        // mod = 2^l, where l is the partition bit length.
        BigInteger mod = BigInteger.ONE.shiftLeft(partitionL);
        int partitionByteL = CommonUtils.getByteLength(partitionL);
        int rows = rows();
        BigInteger[] bigIntegerVector = Arrays.stream(data)
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
        // we need to partition in reverse order so that we can then combine
        for (int partitionIndex = partitionNum - 1; partitionIndex >= 0; partitionIndex--) {
            byte[][] partitionBytesArray = new byte[rows][partitionByteL];
            for (int index = 0; index < rows; index++) {
                BigInteger partitionBigInteger = bigIntegerVector[index].mod(mod);
                bigIntegerVector[index] = bigIntegerVector[index].shiftRight(partitionL);
                partitionBytesArray[index] = BigIntegerUtils.nonNegBigIntegerToByteArray(partitionBigInteger, partitionByteL);
            }
            partitionDatabases[partitionIndex] = ZlByteArrayDatabase.create(partitionL, partitionBytesArray);
        }
        return partitionDatabases;
    }

    @Override
    public BitVector[] partition(EnvType envType, boolean parallel) {
        int rows = rows();
        DenseBitMatrix byteDenseBitMatrix = ByteDenseBitMatrix.fromDense(l, data);
        DenseBitMatrix transByteDenseBitMatrix = byteDenseBitMatrix.transpose(envType, parallel);
        return IntStream.range(0, l)
            .mapToObj(index -> BitVectorFactory.create(rows, transByteDenseBitMatrix.getRow(index)))
            .toArray(BitVector[]::new);
    }

    @Override
    public Database split(int splitRows) {
        int rows = rows();
        MathPreconditions.checkPositiveInRangeClosed("split rows", splitRows, rows);
        byte[][] subData = new byte[splitRows][];
        byte[][] remainData = new byte[rows - splitRows][];
        System.arraycopy(data, 0, subData, 0, splitRows);
        System.arraycopy(data, splitRows, remainData, 0, rows - splitRows);
        data = remainData;
        return ZlByteArrayDatabase.create(l, subData);
    }

    @Override
    public void reduce(int reduceRows) {
        int rows = rows();
        MathPreconditions.checkPositiveInRangeClosed("reduce rows", reduceRows, rows);
        if (reduceRows < rows) {
            // reduce if the reduced rows is less than rows.
            byte[][] remainData = new byte[reduceRows][];
            System.arraycopy(data, 0, remainData, 0, reduceRows);
            data = remainData;
        }
    }

    @Override
    public void merge(Database other) {
        ZlByteArrayDatabase that = (ZlByteArrayDatabase) other;
        MathPreconditions.checkEqual("this.l", "that.l", this.l, that.l);
        byte[][] mergeData = new byte[this.data.length + that.data.length][];
        System.arraycopy(this.data, 0, mergeData, 0, this.data.length);
        System.arraycopy(that.data, 0, mergeData, this.data.length, that.data.length);
        data = mergeData;
    }

    /**
     * Gets the data.
     *
     * @return the data.
     */
    public byte[][] getData() {
        return data;
    }

    /**
     * Gets the data.
     *
     * @param index the index.
     * @return the data.
     */
    public byte[] getData(int index) {
        return data[index];
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(l);
        Arrays.stream(data).forEach(hashCodeBuilder::append);
        return hashCodeBuilder.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ZlByteArrayDatabase) {
            ZlByteArrayDatabase that = (ZlByteArrayDatabase) obj;
            if (this.rows() != that.rows()) {
                return false;
            }
            int rows = rows();
            EqualsBuilder equalsBuilder = new EqualsBuilder();
            equalsBuilder.append(this.l, that.l);
            IntStream.range(0, rows).forEach(index -> equalsBuilder.append(this.data[index], that.data[index]));
            return equalsBuilder.isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        String[] stringData = Arrays.stream(Arrays.copyOf(data, DISPLAY_DATA_ROWS))
            .map(Hex::toHexString)
            .toArray(String[]::new);
        return this.getClass().getSimpleName() + " (l = " + l + "): " + Arrays.toString(stringData);
    }
}
