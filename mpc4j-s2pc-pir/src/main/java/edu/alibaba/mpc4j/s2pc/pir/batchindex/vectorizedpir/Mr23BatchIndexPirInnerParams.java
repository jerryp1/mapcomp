package edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir;

import edu.alibaba.mpc4j.s2pc.pir.PirUtils;

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
     * 各维度长度
     */
    private final int[] dimensionsSize;
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
    /**
     * 分桶内的多项式数目
     */
    private final int totalSize;

    public Mr23BatchIndexPirInnerParams(Mr23BatchIndexPirParams params, int binNum, int binSize) {
        checkDimensionLength(
            binSize, params.getFirstTwoDimensionSize(), params.getThirdDimensionSize()
        );
        this.dimensionsSize = new int[] {
            params.getThirdDimensionSize(), params.getFirstTwoDimensionSize(), params.getFirstTwoDimensionSize()
        };
        this.binNum = binNum;
        this.binSize = binSize;
        this.groupBinSize = (params.getPolyModulusDegree() / 2) / params.getFirstTwoDimensionSize();
        this.totalSize = params.getFirstTwoDimensionSize() * params.getThirdDimensionSize();
    }

    /**
     * 返回各维度的向量长度。
     *
     * @return 各维度的向量长度。
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
            "  - dimensions for d-dimensional hyperrectangle : " + Arrays.toString(dimensionsSize);
    }

    /**
     * 返回分桶数目。
     *
     * @return 分桶数目。
     */
    public int getBinNum() {
        return binNum;
    }

    /**
     * 返回桶内元素数目。
     *
     * @return 桶内元素数目。
     */
    public int getBinSize() {
        return binSize;
    }

    /**
     * 返回群分桶数目。
     *
     * @return 群分桶数目。
     */
    public int getGroupBinSize() {
        return groupBinSize;
    }

    /**
     * 返回分桶内的多项式数目。
     *
     * @return 分桶内的多项式数目。
     */
    public int getTotalSize() {
        return totalSize;
    }
}
