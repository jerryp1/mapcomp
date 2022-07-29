package edu.alibaba.mpc4j.common.tool.bitmatrix;

import edu.alibaba.mpc4j.common.tool.bitmatrix.BitMatrixFactory.BitMatrixType;

/**
 * 比特矩阵抽象接口。比特矩阵可以读取指定行和列的布尔值，也可以读取整列布尔值。
 *
 * @author Weiran Liu
 * @date 2019/10/17
 */
public interface BitMatrix {
    /**
     * 得到(x, y)坐标对应的布尔值。
     *
     * @param x 行坐标。
     * @param y 列坐标。
     * @return (x, y)坐标对应的布尔值。
     */
    boolean get(int x, int y);

    /**
     * 得到第{@code y}列。
     *
     * @param y 列索引值。
     * @return 第{@code y}列。
     */
    byte[] getColumn(int y);

    /**
     * 将第{@code y}列设置为{@code byteArray}。
     *
     * @param y         列索引值。
     * @param byteArray 列值。
     */
    void setColumn(int y, byte[] byteArray);

    /**
     * 返回布尔矩阵行数量。
     *
     * @return 布尔矩阵行数量。
     */
    int getRows();

    /**
     * 返回布尔矩阵列数量。
     *
     * @return 布尔矩阵列数量。
     */
    int getColumns();

    /**
     * 对布尔矩阵转置。
     *
     * @return 转置结果。
     */
    BitMatrix transpose();

    /**
     * 返回布尔矩阵类型。
     *
     * @return 布尔矩阵类型。
     */
    BitMatrixType getBitMatrixType();
}
