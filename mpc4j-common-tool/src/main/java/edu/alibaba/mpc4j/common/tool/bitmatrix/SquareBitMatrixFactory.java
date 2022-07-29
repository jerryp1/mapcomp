package edu.alibaba.mpc4j.common.tool.bitmatrix;

/**
 * 布尔方阵工厂。
 *
 * @author Weiran Liu
 * @date 2022/01/16
 */
public class SquareBitMatrixFactory {

    /**
     * 布尔方阵类型
     */
    public enum SquareBitMatrixType {
        /**
         * 用长整数数组描述的布尔矩阵
         */
        LONG_MATRIX,
        /**
         * 用字节数组描述的布尔矩阵
         */
        BYTE_MATRIX,
    }

    /**
     * 私有构造函数
     */
    private SquareBitMatrixFactory() {
        // empty
    }

    /**
     * 创建布尔方阵实例。
     *
     * @param type 布尔方阵类型。
     * @param bitMatrix 布尔方阵。
     * @return 布尔方阵实例。
     */
    public static SquareBitMatrix createInstance(SquareBitMatrixType type, byte[][] bitMatrix) {
        switch (type) {
            case BYTE_MATRIX:
                return new SquareByteBitMatrix(bitMatrix);
            case LONG_MATRIX:
                return new SquareLongBitMatrix(bitMatrix);
            default:
                throw new IllegalArgumentException("Invalid SquareBitMatrixType: " + type);
        }
    }

    /**
     * 创建布尔方阵实例。
     *
     * @param type 布尔方阵类型。
     * @param size 布尔方阵的大小。
     * @param positions 布尔方阵中取值为1的位置。
     * @throws ArithmeticException 如果布尔方阵不可逆。
     */
    public static SquareBitMatrix createInstance(SquareBitMatrixType type, int size, int[][] positions)
        throws ArithmeticException {
        switch (type) {
            case BYTE_MATRIX:
                return new SquareByteBitMatrix(size, positions);
            case LONG_MATRIX:
                return new SquareLongBitMatrix(size, positions);
            default:
                throw new IllegalArgumentException("Invalid SquareBitMatrixType: " + type);
        }
    }
}
