package edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir;

import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirUtils;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Vectorized Batch PIR协议内部参数。
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class Mr23BatchIndexPirInnerParams {

    /**
     * 各维度的向量长度
     */
    private final int dimensionsLength;
    /**
     * 卡槽数目
     */
    private final int slotNum;
    /**
     * 分桶数目
     */
    private final int binNum;
    /**
     * 桶内元素数目
     */
    private final int binSize;
    /**
     * 群分桶数目
     */
    private final int groupBinSize;

    public Mr23BatchIndexPirInnerParams(Mr23BatchIndexPirParams params, int binNum, int binSize) {
        this.dimensionsLength = computeDimensionLength(binSize, params.getDimension());
        this.binNum = binNum;
        this.binSize = binSize;
        this.slotNum = IndexPirUtils.getNextPowerOfTwo(this.dimensionsLength);
        this.groupBinSize = params.getPolyModulusDegree() / slotNum;
    }

    /**
     * 返回各维度的向量长度。
     *
     * @return 各维度的向量长度。
     */
    public int getDimensionsLength() {
        return dimensionsLength;
    }

    /**
     * 返回数据库编码后每个维度的长度。
     *
     * @param elementSize 元素数量。
     * @return 数据库编码后每个维度的长度。
     */
    private int computeDimensionLength(int elementSize, int dimension) {
        int[] dimensionLength = IntStream.range(0, dimension)
            .map(i -> (int) Math.max(2, Math.floor(Math.pow(elementSize, 1.0 / dimension))))
            .toArray();
        int product = Arrays.stream(dimensionLength, 0, dimension).reduce(1, (a, b) -> a * b);
        if (product < elementSize) {
            IntStream.range(0, dimension).forEach(i -> dimensionLength[i] = dimensionLength[i] + 1);
        }
        product = Arrays.stream(dimensionLength, 0, dimension).reduce(1, (a, b) -> a * b);
        assert (product >= elementSize);
        return dimensionLength[0];
    }

    @Override
    public String toString() {
        return
            "Vectorized PIR Parameters :" + "\n" +
            "  - dimensions for d-dimensional hyperrectangle : " + dimensionsLength;
    }

    /**
     * 返回卡槽数目。
     *
     * @return 卡槽数目。
     */
    public int getSlotNum() {
        return slotNum;
    }

    public int getBinNum() {
        return binNum;
    }

    public int getBinSize() {
        return binSize;
    }

    public int getGroupBinSize() {
        return groupBinSize;
    }
}
