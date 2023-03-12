package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirUtils;

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
     * 各维度的向量长度
     */
    private final int dimensionsLength;
    /**
     * 数据库分块数量
     */
    private final int binNum;
    /**
     * 卡槽数目
     */
    private final int slotNum;
    /**
     * 分块的最长字节长度
     */
    private final int binMaxByteLength;
    /**
     * 最后一个分块的字节长度
     */
    private final int lastBinByteLength;

    public Mr23IndexPirInnerParams(Mr23IndexPirParams params, int serverElementSize, int elementByteLength) {
        this.binMaxByteLength = params.getPlainModulusBitLength()/ Byte.SIZE;
        // 数据库分块数量
        this.binNum = (elementByteLength + binMaxByteLength - 1) / binMaxByteLength;
        this.lastBinByteLength = elementByteLength - (binNum - 1) * binMaxByteLength;
        this.dimensionsLength = computeDimensionLength(serverElementSize, params.getDimension());
        this.slotNum = IndexPirUtils.getNextPowerOfTwo(this.dimensionsLength);
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
}
