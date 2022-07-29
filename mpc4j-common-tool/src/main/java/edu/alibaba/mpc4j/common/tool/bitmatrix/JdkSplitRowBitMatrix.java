package edu.alibaba.mpc4j.common.tool.bitmatrix;

import edu.alibaba.mpc4j.common.tool.bitmatrix.BitMatrixFactory.BitMatrixType;

/**
 * JDK行切分布尔矩阵。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
class JdkSplitRowBitMatrix extends AbstractSplitRowBitMatrix {

    JdkSplitRowBitMatrix(int rows, int columns) {
        super(BitMatrixType.NAIVE, rows, columns);
    }

    @Override
    public BitMatrixType getBitMatrixType() {
        return BitMatrixType.JDK_SPLIT_ROW;
    }
}
