package edu.alibaba.mpc4j.common.tool.bitmatrix;

import edu.alibaba.mpc4j.common.tool.bitmatrix.BitMatrixFactory.BitMatrixType;

/**
 * 本地行切分布尔矩阵。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
public class NativeSplitRowBitMatrix extends AbstractSplitRowBitMatrix {

    NativeSplitRowBitMatrix(int rows, int columns) {
        super(BitMatrixType.NATIVE, rows, columns);
    }

    @Override
    public BitMatrixType getBitMatrixType() {
        return BitMatrixType.NATIVE_SPLIT_ROW;
    }
}
