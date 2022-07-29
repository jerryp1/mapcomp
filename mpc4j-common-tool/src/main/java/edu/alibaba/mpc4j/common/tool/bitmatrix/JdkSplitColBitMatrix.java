package edu.alibaba.mpc4j.common.tool.bitmatrix;

import edu.alibaba.mpc4j.common.tool.bitmatrix.BitMatrixFactory.BitMatrixType;

/**
 * JDK环境列切分布尔矩阵。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
class JdkSplitColBitMatrix extends AbstractSplitColBitMatrix {

    JdkSplitColBitMatrix(int rows, int columns) {
        super(BitMatrixType.NAIVE, rows, columns);
    }

    @Override
    public BitMatrixType getBitMatrixType() {
        return BitMatrixType.JDK_SPLIT_COL;
    }
}
