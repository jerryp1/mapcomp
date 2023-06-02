package edu.alibaba.mpc4j.crypto.matrix.matrix;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.crypto.matrix.vector.RingVector;
import edu.alibaba.mpc4j.crypto.matrix.vector.Zl64Vector;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * the Zl64 matrix.
 *
 * @author Liqiang Peng
 * @date 2023/5/23
 */
public class Zl64Matrix implements RingMatrix {
    /**
     * Zl instance
     */
    public final Zl64 zl64;
    /**
     * elements
     */
    public long[] elements;
    /**
     * parallel operation.
     */
    public boolean parallel;
    /**
     * rows
     */
    public int rows;
    /**
     * cols
     */
    public int cols;

    /**
     * Creates a matrix.
     *
     * @param zl64     Zl64 instance.
     * @param elements elements.
     * @param rows     rows.
     * @param cols     cols.
     * @return a matrix.
     */
    public static Zl64Matrix create(Zl64 zl64, long[] elements, int rows, int cols) {
        assert rows * cols == elements.length;
        Zl64Matrix matrix = new Zl64Matrix(zl64);
        matrix.elements = elements;
        matrix.rows = rows;
        matrix.cols = cols;
        return matrix;
    }

    /**
     * Creates a random matrix.
     *
     * @param zl64         Zl64 instance.
     * @param rows         rows.
     * @param cols         cols.
     * @param secureRandom the random state.
     * @return a matrix.
     */
    public static Zl64Matrix createRandom(Zl64 zl64, int rows, int cols, SecureRandom secureRandom) {
        Zl64Matrix matrix = new Zl64Matrix(zl64);
        matrix.elements = new long[rows * cols];
        matrix.rows = rows;
        matrix.cols = cols;
        IntStream.range(0, rows * cols).forEach(i -> matrix.elements[i] = zl64.createRandom(secureRandom));
        return matrix;
    }

    /**
     * Creates an all-zero matrix.
     *
     * @param zl64 Zl64 instance.
     * @param rows rows.
     * @param cols cols.
     * @return a matrix.
     */
    public static Zl64Matrix createZeros(Zl64 zl64, int rows, int cols) {
        Zl64Matrix matrix = new Zl64Matrix(zl64);
        matrix.elements = new long[rows * cols];
        matrix.rows = rows;
        matrix.cols = cols;
        return matrix;
    }

    /**
     * Creates a random matrix based on the seed.
     *
     * @param zl64 Zl64 instance.
     * @param rows rows.
     * @param cols cols.
     * @param seed the seed.
     * @return a matrix.
     */
    public static Zl64Matrix createRandom(Zl64 zl64, int rows, int cols, byte[] seed) {
        assert seed.length == CommonConstants.BLOCK_BYTE_LENGTH;
        Zl64Matrix matrix = new Zl64Matrix(zl64);
        matrix.elements = new long[rows * cols];
        matrix.rows = rows;
        matrix.cols = cols;
        IntStream intStream = IntStream.range(0, rows * cols);
        intStream.forEach(i -> {
            byte[] temp = ByteBuffer
                .allocate(Integer.BYTES + CommonConstants.BLOCK_BYTE_LENGTH)
                .putInt(i).put(seed)
                .array();
            matrix.elements[i] = zl64.createRandom(temp);
        });
        return matrix;
    }

    private Zl64Matrix(Zl64 zl64) {
        this.zl64 = zl64;
    }

    @Override
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    @Override
    public Zl64Matrix copy() {
        long[] copyElements = Arrays.copyOf(elements, rows * cols);
        return Zl64Matrix.create(zl64, copyElements, rows, cols);
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public int getCols() {
        return cols;
    }

    @Override
    public long get(int i, int j) {
        assert i < rows && j < cols;
        return this.elements[i * cols + j];
    }

    @Override
    public void set(int i, int j, long element) {
        assert i < rows && j < cols;
        this.elements[i * cols + j] = element;
    }

    @Override
    public Zl64Matrix appendZeros(int n) {
        return concat(Zl64Matrix.createZeros(zl64, n, cols));
    }

    @Override
    public Zl64Matrix concat(RingMatrix other) {
        if (rows == 0 && cols == 0) {
            return (Zl64Matrix) other;
        }
        assert cols == other.getCols();
        Zl64Matrix that = (Zl64Matrix) other;
        long[] result = new long[(rows + that.rows) * cols];
        System.arraycopy(elements, 0, result, 0, rows * cols);
        System.arraycopy(that.elements, 0, result, rows * cols, that.rows * cols);
        return Zl64Matrix.create(zl64, result, rows + that.rows, cols);
    }

    @Override
    public void add(long element) {
        IntStream intStream = IntStream.range(0, rows * cols);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(index -> this.elements[index] = zl64.add(this.elements[index], element));
    }

    @Override
    public Zl64Matrix matrixAdd(RingMatrix other) {
        assert this.rows == other.getRows() && this.cols == other.getCols();
        Zl64Matrix that = (Zl64Matrix) other;
        long[] result = new long[rows * cols];
        IntStream intStream = IntStream.range(0, rows * cols);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(index -> result[index] = zl64.add(this.elements[index], that.elements[index]));
        return Zl64Matrix.create(zl64, result, rows, cols);
    }

    @Override
    public void addAt(long element, int i, int j) {
        assert i < rows && j < cols;
        set(i, j, zl64.add(get(i, j), element));
    }

    @Override
    public Zl64Matrix matrixSub(RingMatrix other) {
        assert this.rows == other.getRows() && this.cols == other.getCols();
        Zl64Matrix that = (Zl64Matrix) other;
        long[] result = new long[rows * cols];
        IntStream intStream = IntStream.range(0, rows * cols);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(index -> result[index] = zl64.sub(this.elements[index], that.elements[index]));
        return Zl64Matrix.create(zl64, result, rows, cols);
    }

    @Override
    public void sub(long element) {
        IntStream intStream = IntStream.range(0, rows * cols);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(index -> this.elements[index] = zl64.sub(this.elements[index], element));
    }

    @Override
    public Zl64Matrix matrixMul(RingMatrix other) {
        Zl64Matrix that = (Zl64Matrix) other;
        if (that.cols == 1) {
            Zl64Vector vector = matrixMulVector(Zl64Vector.create(zl64, that.elements));
            return Zl64Matrix.create(zl64, vector.getElements(), rows, 1);
        }
        assert cols == that.rows;
        long[] element = new long[rows * that.cols];
        IntStream rowIndexStream = IntStream.range(0, rows);
        rowIndexStream = parallel ? rowIndexStream.parallel() : rowIndexStream;
        rowIndexStream.forEach(i ->
            IntStream.range(0, that.cols).forEach(j ->
                IntStream.range(0, cols).forEach(l ->
                    element[i * that.cols + j] =
                        zl64.add(element[i * that.cols + j], zl64.mul(get(i, l), that.get(l, j)))
                )
            )
        );
        return Zl64Matrix.create(zl64, element, rows, that.cols);
    }

    @Override
    public Zl64Vector matrixMulVector(RingVector vector) {
        assert cols == vector.getNum();
        Zl64Vector zl64Vector = (Zl64Vector) vector;
        long[] result = new long[rows];
        IntStream rowIndexStream = IntStream.range(0, rows);
        rowIndexStream = parallel ? rowIndexStream.parallel() : rowIndexStream;
        rowIndexStream.forEach(rowIndex -> IntStream.range(0, cols).forEach(colIndex -> result[rowIndex] =
            zl64.add(result[rowIndex], zl64.mul(get(rowIndex, colIndex), zl64Vector.getElement(colIndex)))
        ));
        return Zl64Vector.create(zl64, result);
    }

    @Override
    public Zl64Matrix transpose() {
        if (cols == 1) {
            return Zl64Matrix.create(zl64, elements, 1, rows);
        }
        if (rows == 1) {
            return Zl64Matrix.create(zl64, elements, cols, 1);
        }
        Zl64Matrix transposedMatrix = Zl64Matrix.createZeros(zl64, cols, rows);
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                transposedMatrix.set(i, j, get(j, i));
            }
        }
        return transposedMatrix;
    }
}
