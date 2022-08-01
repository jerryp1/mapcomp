package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 用byte[]表示的布尔方阵。
 *
 * @author Weiran Liu
 * @date 2021/12/20
 */
public class ByteSquareDenseBitMatrix implements SquareDenseBitMatrix {
    /**
     * 布尔方阵的大小
     */
    private final int size;
    /**
     * 布尔方阵的字节大小
     */
    private final int byteSize;
    /**
     * 偏移量
     */
    private final int offset;
    /**
     * 布尔方阵
     */
    private final byte[][] byteBitMatrix;

    /**
     * 构建布尔方阵。
     *
     * @param size 布尔方阵的大小。
     * @param positions 布尔方阵中取值为1的位置。
     */
    public ByteSquareDenseBitMatrix(int size, int[][] positions) throws ArithmeticException {
        assert size > 0 : "Size of SquareBitMatrix must be greater than 0";
        this.size = size;
        byteSize = CommonUtils.getByteLength(size);
        offset = byteSize * Byte.SIZE - size;
        byteBitMatrix = new byte[size][byteSize];
        assert positions.length == size;
        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            int[] rows = positions[rowIndex];
            for (int position : rows) {
                assert position >= 0 && position < size;
                // 将每个所需的位置设置为1
                BinaryUtils.setBoolean(byteBitMatrix[rowIndex], position + offset, true);
            }
        }
    }

    /**
     * 构建布尔方阵。
     *
     * @param bitMatrix 布尔方阵描述。
     */
    public ByteSquareDenseBitMatrix(byte[][] bitMatrix) {
        assert bitMatrix.length > 0 : "Size of SquareBitMatrix must be greater than 0";
        size = bitMatrix.length;
        byteSize = CommonUtils.getByteLength(size);
        offset = byteSize * Byte.SIZE - size;
        for (byte[] row : bitMatrix) {
            assert row.length == byteSize;
            assert BytesUtils.isReduceByteArray(row, size);
        }
        byteBitMatrix = bitMatrix;
    }

    /**
     * 构建随机布尔方阵，得到的随机布尔方阵不一定可逆。
     *
     * @param size 布尔方阵的大小。
     * @param secureRandom 随机状态。
     */
    public ByteSquareDenseBitMatrix(int size, SecureRandom secureRandom) {
        assert size > 0 : "Size of SquareBitMatrix must be greater than 0";
        this.size = size;
        byteSize = CommonUtils.getByteLength(size);
        offset = byteSize * Byte.SIZE - size;
        byteBitMatrix = IntStream.range(0, size)
            .mapToObj(rowIndex -> {
                byte[] row = new byte[byteSize];
                secureRandom.nextBytes(row);
                BytesUtils.reduceByteArray(row, size);
                return row;
            })
            .toArray(byte[][]::new);
    }

    @Override
    public SquareDenseBitMatrix inverse() {
        // 构造布尔矩阵
        boolean[][] matrix = Arrays.stream(byteBitMatrix)
            .map(row -> BinaryUtils.byteArrayToBinary(row, size))
            .toArray(boolean[][]::new);
        // 构造逆矩阵，先将逆矩阵初始化为单位阵
        boolean[][] inverseMatrix = new boolean[size][size];
        IntStream.range(0, size).forEach(i -> inverseMatrix[i][i] = true);
        // 利用初等变换计算逆矩阵。首先将左矩阵转换为上三角矩阵
        for (int p = 0; p < size; p++) {
            if (!matrix[p][p]) {
                // 找到一个不为0的行
                int other = p + 1;
                while (other < size && !matrix[other][p]) {
                    other++;
                }
                if (other >= size) {
                    throw new ArithmeticException("Cannot invert bit matrix");
                } else {
                    // 左侧矩阵行swap
                    boolean[] matrixRowTemp = matrix[p];
                    matrix[p] = matrix[other];
                    matrix[other] = matrixRowTemp;
                    // 右侧矩阵行swap
                    boolean[] inverseMatrixRowTemp = inverseMatrix[p];
                    inverseMatrix[p] = inverseMatrix[other];
                    inverseMatrix[other] = inverseMatrixRowTemp;
                }
            }
            // 左右侧矩阵高斯消元
            for (int i = p + 1; i < size; i++) {
                if (matrix[i][p]) {
                    for (int j = 0; j < size; j++) {
                        matrix[i][j] = matrix[i][j] ^ matrix[p][j];
                        inverseMatrix[i][j] = inverseMatrix[i][j] ^ inverseMatrix[p][j];
                    }
                }
            }
        }
        // 将左侧矩阵转为单位矩阵，此时右侧得到的矩阵就是左侧矩阵的逆矩阵
        for (int p = size - 1; p >= 0; p--) {
            for (int r = 0; r < p; r++) {
                if (matrix[r][p]) {
                    // 如果有1的，则进行相加
                    for (int j = 0; j < size; j++) {
                        matrix[r][j] ^= matrix[p][j];
                        inverseMatrix[r][j] ^= inverseMatrix[p][j];
                    }
                }
            }
        }
        // 返回逆矩阵
        byte[][] invertByteBitMatrix = Arrays.stream(inverseMatrix)
            .map(BinaryUtils::binaryToRoundByteArray)
            .toArray(byte[][]::new);
        return new ByteSquareDenseBitMatrix(invertByteBitMatrix);
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
    public byte[] multiply(final byte[] input) {
        assert input.length == byteSize;
        assert BytesUtils.isReduceByteArray(input, size);
        byte[] output = new byte[byteSize];
        for (int columnIndex = 0; columnIndex < size; columnIndex++) {
            if (BinaryUtils.getBoolean(input, columnIndex + offset)) {
                BytesUtils.xori(output, byteBitMatrix[columnIndex]);
            }
        }
        return output;
    }

    @Override
    public SquareDenseBitMatrix transpose() {
        byte[][] transBitMatrix = new byte[size][byteSize];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (BinaryUtils.getBoolean(byteBitMatrix[row], column + offset)) {
                    BinaryUtils.setBoolean(transBitMatrix[column], row + offset, true);
                }
            }
        }
        return new ByteSquareDenseBitMatrix(transBitMatrix);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(byteBitMatrix).forEach(row -> {
            BigInteger rowBigInteger = new BigInteger(1, row);
            StringBuilder rowStringBuilder = new StringBuilder(rowBigInteger.toString(2));
            while (rowStringBuilder.length() < size) {
                rowStringBuilder.insert(0, "0");
            }
            stringBuilder.append(rowStringBuilder).append("\n");
        });
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ByteSquareDenseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ByteSquareDenseBitMatrix that = (ByteSquareDenseBitMatrix)obj;
        return new EqualsBuilder().append(this.byteBitMatrix, that.byteBitMatrix).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(byteBitMatrix).toHashCode();
    }
}
