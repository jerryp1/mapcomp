package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Vectorized PIR协议内部参数。
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class Mr23IndexPirInnerParams {

    /**
     * 数据库分块数量
     */
    private final int binNum;
    /**
     * 分块的最长字节长度
     */
    private final int binMaxByteLength;
    /**
     * 最后一个分块的字节长度
     */
    private final int lastBinByteLength;
    /**
     * 各维度长度
     */
    private final int[] dimensionsSize;

    public Mr23IndexPirInnerParams(Mr23IndexPirParams params, int serverElementSize, int elementByteLength) {
        this.binMaxByteLength = params.getPlainModulusBitLength()/ Byte.SIZE;
        // 数据库分块数量
        this.binNum = CommonUtils.getUnitNum(elementByteLength, binMaxByteLength);
        this.lastBinByteLength = elementByteLength - (binNum - 1) * binMaxByteLength;
        assert params.getDimension() == 3;
        checkDimensionLength(
            serverElementSize, params.getFirstTwoDimensionSize(), params.getThirdDimensionSize()
        );
        this.dimensionsSize = new int[] {
            params.getThirdDimensionSize(), params.getFirstTwoDimensionSize(), params.getFirstTwoDimensionSize()
        };
    }

    /**
     * 返回分块数目。
     *
     * @return RGSW密文参数。
     */
    public int getBinNum() {
        return this.binNum;
    }

    /**
     * 返回分块的最大字节长度。
     *
     * @return 分块的最大字节长度。
     */
    public int getBinMaxByteLength() {
        return binMaxByteLength;
    }

    /**
     * 返回最后一个分块的字节长度。
     *
     * @return 最后一个分块的字节长度。
     */
    public int getLastBinByteLength() {
        return lastBinByteLength;
    }

    /**
     * 返回各维度长度。
     *
     * @return 各维度长度。
     */
    public int[] getDimensionsSize() {
        return dimensionsSize;
    }

    /**
     * 检查数据库的维度长度是否合理。
     *
     * @param elementSize           元素数量。
     * @param firstTwoDimensionSize 前两维长度。
     * @param thirdDimensionSize    第三维长度。
     */
    private void checkDimensionLength(int elementSize, int firstTwoDimensionSize, int thirdDimensionSize) {
        int product = firstTwoDimensionSize * firstTwoDimensionSize * thirdDimensionSize;
        assert product >= elementSize;
    }

    @Override
    public String toString() {
        return
            "Vectorized PIR Parameters :" + "\n" +
            "  - dimensions for 3-dimensional hyperrectangle : " + Arrays.toString(dimensionsSize);
    }

}
