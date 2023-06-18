package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.stream.IntStream;

/**
 * 用byte[]表示的布尔方阵。
 *
 * @author Weiran Liu, Hanwen Feng
 * @date 2021/12/20
 */
public class ByteSquareDenseBitMatrix extends AbstractByteDenseBitMatrix implements SquareDenseBitMatrix {
    /**
     * 布尔方阵的大小
     */
    private final int size;
    /**
     * 布尔方阵的字节大小
     */
    private final int byteSize;
    /**
     * 字节偏移量
     */
    private final int byteOffset;

    /**
     * 构建布尔方阵。
     *
     * @param positions 布尔方阵中取值为1的位置。
     */
    public static ByteSquareDenseBitMatrix fromSparse(int[][] positions) {
        ByteSquareDenseBitMatrix squareDenseBitMatrix = new ByteSquareDenseBitMatrix(positions.length);
        int size = positions.length;
        // verify square
        for (int i = 0; i < size; i++) {
            for (int j : positions[i]) {
                assert j >= 0 && j < size : "j must be in range [0, " + size + ") for row " + i + " :" + j;
            }
        }
        squareDenseBitMatrix.initFromSparse(positions.length, positions);
        return squareDenseBitMatrix;
    }

    /**
     * 构建布尔方阵。
     *
     * @param bitMatrix 布尔方阵描述。
     */
    public static ByteSquareDenseBitMatrix fromDense(byte[][] bitMatrix) {
        ByteSquareDenseBitMatrix squareDenseBitMatrix = new ByteSquareDenseBitMatrix(bitMatrix.length);
        int size = bitMatrix.length;
        // verify square
        for (int i = 0; i < size; i++) {
            assert bitMatrix[i].length == squareDenseBitMatrix.byteSize
                : "row " + i + " byte length must be " + squareDenseBitMatrix.byteSize + ": " + bitMatrix[i].length;
            assert BytesUtils.isReduceByteArray(bitMatrix[i], size);
        }
        squareDenseBitMatrix.initFromDense(bitMatrix.length, bitMatrix);
        return squareDenseBitMatrix;
    }

    /**
     * private constructor.
     */
    private ByteSquareDenseBitMatrix(int size) {
        assert size > 0 : "size must be greater than 0: " + size;
        this.size = size;
        this.byteSize = CommonUtils.getByteLength(size);
        byteOffset = byteSize * Byte.SIZE - size;
    }

    @Override
    public SquareDenseBitMatrix add(DenseBitMatrix that) {
        return fromDense(super.addToBytes(that));
    }

    @Override
    public DenseBitMatrix multiply(DenseBitMatrix that) {
        return that.getColumns() == rows
            ? fromDense(super.multiplyToBytes(that))
            : ByteDenseBitMatrix.fromDense(that.getColumns(), super.multiplyToBytes(that));
    }

    @Override
    public SquareDenseBitMatrix transpose(EnvType envType, boolean parallel) {
        return fromDense(super.transposeToBytes(envType, parallel));
    }

    @Override
    public SquareDenseBitMatrix inverse() {
        byte[][] matrix = BytesUtils.clone(byteBitMatrix);
        byte[][] inverseMatrix = new byte[size][byteSize];
        IntStream.range(0, size).forEach(i -> BinaryUtils.setBoolean(inverseMatrix[i], i + byteOffset, true));
        // 利用初等变换计算逆矩阵。首先将左矩阵转换为上三角矩阵
        for (int p = 0; p < size; p++) {
            if (!BinaryUtils.getBoolean(matrix[p], p + byteOffset)) {
                // 找到一个不为0的行
                int other = p + 1;
                while (other < size && !BinaryUtils.getBoolean(matrix[other], p + byteOffset)) {
                    other++;
                }
                if (other >= size) {
                    throw new ArithmeticException("Cannot invert bit matrix");
                } else {
                    // 左侧矩阵行swap
                    byte[] matrixRowTemp = matrix[p];
                    matrix[p] = matrix[other];
                    matrix[other] = matrixRowTemp;
                    // 右侧矩阵行swap
                    byte[] inverseMatrixRowTemp = inverseMatrix[p];
                    inverseMatrix[p] = inverseMatrix[other];
                    inverseMatrix[other] = inverseMatrixRowTemp;
                }
            }
            // 左右侧矩阵高斯消元
            for (int i = p + 1; i < size; i++) {
                if (BinaryUtils.getBoolean(matrix[i], p + byteOffset)) {
                    BytesUtils.xori(matrix[i], matrix[p]);
                    BytesUtils.xori(inverseMatrix[i], inverseMatrix[p]);
                }
            }
        }
        // 将左侧矩阵转为单位矩阵，此时右侧得到的矩阵就是左侧矩阵的逆矩阵
        for (int p = size - 1; p >= 0; p--) {
            for (int r = 0; r < p; r++) {
                if (BinaryUtils.getBoolean(matrix[r], p + byteOffset)) {
                    // 如果有1的，则进行相加
                    BytesUtils.xori(matrix[r], matrix[p]);
                    BytesUtils.xori(inverseMatrix[r], inverseMatrix[p]);
                }
            }
        }
        // 返回逆矩阵
        return ByteSquareDenseBitMatrix.fromDense(inverseMatrix);
    }


    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getByteSize() {
        return byteSize;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ByteSquareDenseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ByteSquareDenseBitMatrix that = (ByteSquareDenseBitMatrix) obj;
        return new EqualsBuilder().append(this.byteBitMatrix, that.byteBitMatrix).isEquals();
    }
}
