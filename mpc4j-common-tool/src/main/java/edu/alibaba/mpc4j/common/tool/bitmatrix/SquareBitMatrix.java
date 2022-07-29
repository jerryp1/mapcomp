package edu.alibaba.mpc4j.common.tool.bitmatrix;

/**
 * 布尔方阵接口。
 *
 * @author Weiran Liu
 * @date 2022/01/16
 */
public interface SquareBitMatrix {

    /**
     * 返回布尔方阵的大小。
     *
     * @return 布尔方阵的大小。
     */
    int getSize();

    /**
     * 返回布尔仿真的字节大小。
     *
     * @return 布尔仿真的字节大小。
     */
    int getByteSize();

    /**
     * 计算输入向量乘以布尔方阵。
     *
     * @param input 输入向量。
     * @return 相乘结果。
     */
    byte[] multiply(final byte[] input);

    /**
     * 计算布尔方阵的转置。
     *
     * @return 转置布尔方阵。
     */
    SquareBitMatrix transpose();

    /**
     * 计算布尔方阵的逆方阵。
     *
     * @return 布尔方阵的逆方阵。
     * @throws ArithmeticException 如果布尔方阵不可逆。
     */
    SquareBitMatrix inverse() throws ArithmeticException;
}
