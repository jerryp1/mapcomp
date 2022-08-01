package edu.alibaba.mpc4j.common.tool.bitmatrix.dense;

import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 用long[]维护的布尔方阵。
 *
 * @author Weiran Liu
 * @date 2022/01/16
 */
public class LongSquareDenseBitMatrix implements SquareDenseBitMatrix {
    /**
     * 布尔方阵的大小
     */
    private final int size;
    /**
     * 布尔方阵的字节大小
     */
    private final int byteSize;
    /**
     * 布尔方阵的长整数大小
     */
    private final int longSize;
    /**
     * 偏移量
     */
    private final int offset;
    /**
     * 布尔方阵
     */
    private final long[][] longBitMatrix;

    /**
     * 构建布尔方阵。
     *
     * @param size      布尔方阵的大小。
     * @param positions 布尔方阵中取值为1的位置。
     */
    public LongSquareDenseBitMatrix(int size, int[][] positions) throws ArithmeticException {
        assert size > 0 : "Size of SquareBitMatrix must be greater than 0";
        this.size = size;
        byteSize = CommonUtils.getByteLength(size);
        longSize = CommonUtils.getLongLength(size);
        offset = longSize * Long.SIZE - size;
        longBitMatrix = new long[size][longSize];
        assert positions.length == size;
        for (int row = 0; row < size; row++) {
            int[] rows = positions[row];
            for (int position : rows) {
                assert position >= 0 && position < size;
                // 将每个所需的位置设置为1
                BinaryUtils.setBoolean(longBitMatrix[row], position + offset, true);
            }
        }
    }

    /**
     * 构建布尔方阵。
     *
     * @param bitMatrix 布尔方阵描述。
     */
    public LongSquareDenseBitMatrix(byte[][] bitMatrix) {
        assert bitMatrix.length > 0 : "Size of SquareBitMatrix must be greater than 0";
        size = bitMatrix.length;
        byteSize = CommonUtils.getByteLength(size);
        longSize = CommonUtils.getLongLength(size);
        offset = longSize * Long.SIZE - size;
        int byteSize = CommonUtils.getByteLength(size);
        longBitMatrix = Arrays.stream(bitMatrix)
            .map(rowByteArray -> {
                assert rowByteArray.length == byteSize;
                assert BytesUtils.isReduceByteArray(rowByteArray, size);
                return LongUtils.byteArrayToRoundLongArray(rowByteArray);
            })
            .toArray(long[][]::new);
    }

    /**
     * 构建布尔方阵。
     *
     * @param bitMatrix 布尔方阵描述。
     */
    private LongSquareDenseBitMatrix(long[][] bitMatrix) {
        assert bitMatrix.length > 0 : "Size of SquareBitMatrix must be greater than 0";
        size = bitMatrix.length;
        byteSize = CommonUtils.getByteLength(size);
        longSize = CommonUtils.getLongLength(size);
        offset = longSize * Long.SIZE - size;
        for (long[] row : bitMatrix) {
            assert row.length == longSize;
            assert LongUtils.isReduceLongArray(row, size);
        }
        longBitMatrix = bitMatrix;
    }

    /**
     * 构建随机布尔方阵，得到的随机布尔方阵不一定可逆。
     *
     * @param size         布尔方阵的大小。
     * @param secureRandom 随机状态。
     */
    public LongSquareDenseBitMatrix(int size, SecureRandom secureRandom) {
        assert size > 0 : "Size of SquareBitMatrix must be greater than 0";
        this.size = size;
        byteSize = CommonUtils.getByteLength(size);
        longSize = CommonUtils.getLongLength(size);
        offset = longSize * Long.SIZE - size;
        longBitMatrix = IntStream.range(0, size)
            .mapToObj(rowIndex -> {
                long[] row = new long[longSize];
                for (int columnIndex = 0; columnIndex < longSize; columnIndex++) {
                    row[columnIndex] = secureRandom.nextLong();
                }
                LongUtils.reduceLongArray(row, size);
                return row;
            })
            .toArray(long[][]::new);
    }

    @Override
    public SquareDenseBitMatrix inverse() {
        // 构造布尔矩阵
        boolean[][] matrix = Arrays.stream(longBitMatrix)
            .map(row -> BinaryUtils.longArrayToBinary(row, size))
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
        long[][] invertLongBitMatrix = Arrays.stream(inverseMatrix)
            .map(BinaryUtils::binaryToRoundLongArray)
            .toArray(long[][]::new);
        return new LongSquareDenseBitMatrix(invertLongBitMatrix);
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getByteSize() {
        return byteSize;
    }

    /**
     * 返回布尔方阵的长整数大小。
     *
     * @return 布尔方阵的长整数大小。
     */
    public int getLongSize() {
        return longSize;
    }

    @Override
    public byte[] multiply(final byte[] input) {
        assert input.length == byteSize : "input.length must be equal to " + byteSize + ": " + input.length;
        assert BytesUtils.isReduceByteArray(input, size);
        int byteArrayOffset = input.length * Byte.SIZE - size;
        long[] longOutput = new long[longSize];
        for (int binaryIndex = 0; binaryIndex < size; binaryIndex++) {
            if (BinaryUtils.getBoolean(input, binaryIndex + byteArrayOffset)) {
                LongUtils.xori(longOutput, longBitMatrix[binaryIndex]);
            }
        }
        return LongUtils.longArrayToByteArray(longOutput, byteSize);
    }

    /**
     * 计算输入向量乘以布尔方阵。
     *
     * @param input 输入向量。
     * @return 相乘结果。
     */
    public long[] multiply(final long[] input) {
        assert input.length == longSize;
        assert LongUtils.isReduceLongArray(input, size);
        long[] longOutput = new long[longSize];
        for (int binaryIndex = offset; binaryIndex < size + offset; binaryIndex++) {
            if (BinaryUtils.getBoolean(input, binaryIndex)) {
                LongUtils.xori(longOutput, longBitMatrix[binaryIndex]);
            }
        }
        return longOutput;
    }

    @Override
    public SquareDenseBitMatrix transpose() {
        long[][] transBitMatrix = new long[size][longSize];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (BinaryUtils.getBoolean(longBitMatrix[row], column + offset)) {
                    BinaryUtils.setBoolean(transBitMatrix[column], row + offset, true);
                }
            }
        }
        return new LongSquareDenseBitMatrix(transBitMatrix);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(longBitMatrix).forEach(longRow -> {
            BigInteger rowBigInteger = new BigInteger(1, LongUtils.longArrayToByteArray(longRow));
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
        if (!(obj instanceof LongSquareDenseBitMatrix)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        LongSquareDenseBitMatrix that = (LongSquareDenseBitMatrix) obj;
        return new EqualsBuilder().append(this.longBitMatrix, that.longBitMatrix).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(longBitMatrix).toHashCode();
    }
}
