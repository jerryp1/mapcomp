package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.s2pc.pir.PirUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * OnionPIR协议内部参数。
 *
 * @author Liqiang Peng
 * @date 2023/3/2
 */
public class Mcr21IndexPirInnerParams {
    /**
     * 维数
     */
    private final int[] dimension;
    /**
     * 多项式里的元素数量
     */
    private final int[] elementSizeOfPlaintext;
    /**
     * 多项式数量
     */
    private final int[] plaintextSize;
    /**
     * 各维度的向量长度
     */
    private final int[][] dimensionsLength;
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

    public Mcr21IndexPirInnerParams(Mcr21IndexPirParams params, int serverElementSize, int elementByteLength) {
        // 一个多项式可表示的字节长度
        this.binMaxByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        // 数据库分块数量
        this.binNum = (elementByteLength + binMaxByteLength - 1) / binMaxByteLength;
        this.lastBinByteLength = elementByteLength - (binNum - 1) * binMaxByteLength;
        this.elementSizeOfPlaintext = new int[this.binNum];
        this.plaintextSize = new int[this.binNum];
        this.dimensionsLength = new int[this.binNum][];
        this.dimension = new int[this.binNum];
        IntStream.range(0, this.binNum).forEach(i -> {
            int byteLength = i == binNum - 1 ? lastBinByteLength : binMaxByteLength;
            // 一个多项式可以包含的元素数量
            elementSizeOfPlaintext[i] = PirUtils.elementSizeOfPlaintext(
                byteLength, params.getPolyModulusDegree(), params.getPlainModulusBitLength()
            );
            // 多项式数量
            plaintextSize[i] = (int) Math.ceil((double)serverElementSize / elementSizeOfPlaintext[i]);
            // 各维度的向量长度
            dimensionsLength[i] = computeDimensionLength(
                plaintextSize[i], params.getFirstDimensionSize(), params.getSubsequentDimensionSize()
            );
            dimension[i] = dimensionsLength[i].length;
        });
    }

    /**
     * 返回各维度的向量长度。
     *
     * @return 各维度的向量长度。
     */
    public int[][] getDimensionsLength() {
        return dimensionsLength;
    }

    /**
     * 返回多项式数量。
     *
     * @return 多项式数量。
     */
    public int[] getPlaintextSize() {
        return plaintextSize;
    }

    /**
     * 返回多项式里的元素数量。
     *
     * @return 多项式里的元素数量。
     */
    public int[] getElementSizeOfPlaintext() {
        return elementSizeOfPlaintext;
    }

    /**
     * 返回分块数目。
     *
     * @return 分块数目。
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
    private int[] computeDimensionLength(int elementSize, int firstDimensionSize, int subsequentDimensionSize) {
        ArrayList<Integer> dimensionLength = new ArrayList<>();
        dimensionLength.add(firstDimensionSize);
        int product = firstDimensionSize;
        for (int i = elementSize / firstDimensionSize; i >= subsequentDimensionSize; i /= subsequentDimensionSize) {
            dimensionLength.add(subsequentDimensionSize);
            product *= subsequentDimensionSize;
        }
        int dimensionSize = dimensionLength.size();
        int[] dimensionArray = IntStream.range(0, dimensionSize).map(dimensionLength::get).toArray();
        while (product < elementSize) {
            dimensionArray[dimensionSize - 1]++;
            product = 1;
            product *= Arrays.stream(dimensionArray, 0, dimensionSize).reduce(1, (a, b) -> a * b);
        }
        if (dimensionSize == 1 && dimensionArray[0] > firstDimensionSize) {
            dimensionArray = new int[] {firstDimensionSize, subsequentDimensionSize};
        }
        return dimensionArray;
    }

    @Override
    public String toString() {
        return
            "OnionPIR Parameters :" + "\n" +
            "  - elements per BFV plaintext : " + Arrays.toString(elementSizeOfPlaintext) + "\n" +
            "  - dimensions for d-dimensional hyperrectangle : " + Arrays.toString(dimension) + "\n" +
            "  - number of BFV plaintexts (before padding) : " + Arrays.toString(plaintextSize);
    }
}
