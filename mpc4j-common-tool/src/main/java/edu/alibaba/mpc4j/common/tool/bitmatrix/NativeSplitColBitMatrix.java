package edu.alibaba.mpc4j.common.tool.bitmatrix;

import edu.alibaba.mpc4j.common.tool.bitmatrix.BitMatrixFactory.BitMatrixType;

/**
 * 本地列切分布尔矩阵。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
class NativeSplitColBitMatrix extends AbstractSplitColBitMatrix {

    public NativeSplitColBitMatrix(int rows, int columns) {
        super(BitMatrixType.NATIVE, rows, columns);
    }

    @Override
    public BitMatrixType getBitMatrixType() {
        return BitMatrixType.NATIVE_SPLIT_COL;
    }
}
