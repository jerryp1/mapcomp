package edu.alibaba.mpc4j.common.tool.bitmatrix;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * 布尔矩阵工厂类。
 *
 * @author Weiran Liu
 * @date 2021/11/29
 */
public class BitMatrixFactory {
    /**
     * 私有构造函数
     */
    private BitMatrixFactory() {
        // empty
    }

    /**
     * 比特矩阵类型
     */
    public enum BitMatrixType {
        /**
         * 朴素转置布尔矩阵
         */
        NAIVE,
        /**
         * Eklundh转置布尔矩阵
         */
        EKLUNDH,
        /**
         * 本地布尔矩阵
         */
        NATIVE,
        /**
         * Java行切分布尔矩阵
         */
        JDK_SPLIT_ROW,
        /**
         * 最优行切分布尔矩阵
         */
        NATIVE_SPLIT_ROW,
        /**
         * Java列切分布尔矩阵
         */
        JDK_SPLIT_COL,
        /**
         * 最优列切分布尔矩阵
         */
        NATIVE_SPLIT_COL,
    }

    /**
     * 创建布尔矩阵实例。
     *
     * @param type 布尔矩阵类型。
     * @param rows 行数。
     * @param columns 列数。
     * @return 创建好的布尔矩阵。
     */
    public static BitMatrix createInstance(BitMatrixType type, int rows, int columns) {
        switch (type) {
            case NAIVE:
                return new NaiveBitMatrix(rows, columns);
            case EKLUNDH:
                return new EklundhBitMatrix(rows, columns);
            case NATIVE:
                return new NativeBitMatrix(rows, columns);
            case JDK_SPLIT_ROW:
                return new JdkSplitRowBitMatrix(rows, columns);
            case NATIVE_SPLIT_ROW:
                return new NativeSplitRowBitMatrix(rows, columns);
            case JDK_SPLIT_COL:
                return new JdkSplitColBitMatrix(rows, columns);
            case NATIVE_SPLIT_COL:
                return new NativeSplitColBitMatrix(rows, columns);
            default:
                throw new IllegalArgumentException("Invalid BitMatrixType: " + type.name());
        }
    }

    /**
     * 创建布尔矩阵实例。
     *
     * @param envType 环境类型。
     * @param rows 行数。
     * @param columns 列数。
     * @param parallel 是否并发处理。
     * @return 创建好的布尔矩阵。
     */
    public static BitMatrix createInstance(EnvType envType, int rows, int columns, boolean parallel) {
        switch (envType) {
            case STANDARD:
            case INLAND:
                if (parallel) {
                    // 并发处理，返回行切分或列切分
                    if (rows >= columns) {
                        return createInstance(BitMatrixType.NATIVE_SPLIT_ROW, rows, columns);
                    } else {
                        return createInstance(BitMatrixType.NATIVE_SPLIT_COL, rows, columns);
                    }
                } else {
                    // 串行处理
                    return createInstance(BitMatrixType.NATIVE, rows, columns);
                }
            case STANDARD_JDK:
            case INLAND_JDK:
                if (parallel) {
                    // 并发处理，返回行切分或列切分
                    if (rows >= columns) {
                        return createInstance(BitMatrixType.JDK_SPLIT_ROW, rows, columns);
                    } else {
                        return createInstance(BitMatrixType.JDK_SPLIT_COL, rows, columns);
                    }
                } else {
                    return createInstance(BitMatrixType.NAIVE, rows, columns);
                }
            default:
                throw new IllegalArgumentException("Invalid EnvType: " + envType.name());
        }
    }
}
