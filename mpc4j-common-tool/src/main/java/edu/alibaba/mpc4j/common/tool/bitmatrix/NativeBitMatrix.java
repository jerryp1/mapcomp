package edu.alibaba.mpc4j.common.tool.bitmatrix;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.BitMatrixFactory.BitMatrixType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.stream.IntStream;

/**
 * 本地布尔矩阵实现。
 *
 * @author Weiran Liu
 * @date 2021/06/22
 */
class NativeBitMatrix extends AbstractBitMatrix {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * 用二维字节数组表示的矩阵
     */
    private final byte[][] data;
    /**
     * 字节行数
     */
    private final int rowBytes;
    /**
     * 规约到字节的行数
     */
    private final int roundByteRow;
    /**
     * 行偏移量
     */
    private final int rowOffset;
    /**
     * 字节列数
     */
    private final int columnBytes;
    /**
     * 规约到字节的列数
     */
    private final int roundByteColumn;
    /**
     * 列偏移量
     */
    private final int columnOffset;

    NativeBitMatrix(int rows, int columns) {
        super(rows, columns);
        rowBytes = CommonUtils.getByteLength(rows);
        roundByteRow = rowBytes * Byte.SIZE;
        rowOffset = roundByteRow - rows;
        columnBytes = CommonUtils.getByteLength(columns);
        roundByteColumn = columnBytes * Byte.SIZE;
        data = new byte[roundByteColumn][rowBytes];
        columnOffset = roundByteColumn - columns;
    }

    @Override
    public boolean get(int x, int y) {
        assert (x >= 0 && x < rows);
        assert (y >= 0 && y < columns);
        return BinaryUtils.getBoolean(data[y + this.columnOffset], x + rowOffset);
    }

    @Override
    public byte[] getColumn(int y) {
        assert (y >= 0 && y < columns);
        return data[y + columnOffset];
    }

    @Override
    public void setColumn(int y, byte[] byteArray) {
        assert (y >= 0 && y < columns);
        assert byteArray.length == rowBytes;
        assert BytesUtils.isReduceByteArray(byteArray, rows);
        this.data[y + this.columnOffset] = byteArray;
    }

    @Override
    public BitMatrix transpose() {
        // 将矩阵数据打平
        byte[] flattenMatrix = new byte[this.roundByteColumn * this.rowBytes];
        IntStream.range(0, this.roundByteColumn).forEach(column ->
            System.arraycopy(this.data[column], 0, flattenMatrix, column * this.rowBytes, this.rowBytes)
        );
        // 本地转置
        byte[] transposeFlattenMatrix = this.nativeTranspose(flattenMatrix, this.roundByteRow, this.roundByteColumn);
        // 将打平数据变回为矩阵
        byte[][] bMatrix = new byte[this.roundByteRow][this.columnBytes];
        IntStream.range(0, this.roundByteRow).forEach(row ->
            System.arraycopy(transposeFlattenMatrix, row * this.columnBytes, bMatrix[row], 0, this.columnBytes)
        );
        NativeBitMatrix b = new NativeBitMatrix(this.columns, this.rows);
        IntStream.range(0, this.roundByteRow).forEach(row -> b.data[row] = bMatrix[row]);

        return b;
    }

    @Override
    public BitMatrixType getBitMatrixType() {
        return BitMatrixType.NATIVE;
    }

    private native byte[] nativeTranspose(byte[] byteArrayMatrix, int rows, int columns);
}
