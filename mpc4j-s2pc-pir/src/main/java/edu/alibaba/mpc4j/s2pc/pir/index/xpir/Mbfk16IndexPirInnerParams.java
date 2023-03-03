package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirUtils;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * XPIR协议内部参数。
 *
 * @author Liqiang Peng
 * @date 2023/3/2
 */
public class Mbfk16IndexPirInnerParams {

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

    public Mbfk16IndexPirInnerParams(Mbfk16IndexPirParams params, int serverElementSize, int elementByteLength) {
        // 一个多项式可表示的字节长度
        this.binMaxByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        // 数据库分块数量
        this.binNum = (elementByteLength + binMaxByteLength - 1) / binMaxByteLength;
        this.lastBinByteLength = elementByteLength - (binNum - 1) * binMaxByteLength;
        this.elementSizeOfPlaintext = new int[this.binNum];
        this.plaintextSize = new int[this.binNum];
        this.dimensionsLength = new int[this.binNum][];
        IntStream.range(0, this.binNum).forEach(i -> {
            int byteLength = i == binNum - 1 ? lastBinByteLength : binMaxByteLength;
            // 一个多项式可以包含的元素数量
            elementSizeOfPlaintext[i] =
                IndexPirUtils.elementSizeOfPlaintext(byteLength, params.getPolyModulusDegree(), params.getPlainModulusBitLength());
            // 多项式数量
            this.plaintextSize[i] = (int) Math.ceil((double) serverElementSize / this.elementSizeOfPlaintext[i]);
            // 各维度的向量长度
            this.dimensionsLength[i] = computeDimensionLength(this.plaintextSize[i], params.getDimension());
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
    private int[] computeDimensionLength(int elementSize, int dimension) {
        int[] dimensionLength = IntStream.range(0, dimension)
            .map(i -> (int) Math.max(2, Math.floor(Math.pow(elementSize, 1.0 / dimension))))
            .toArray();
        int product = 1;
        int j = 0;
        // if plaintext_num is not a d-power
        if (dimensionLength[0] != Math.pow(elementSize, 1.0 / dimension)) {
            while (product < elementSize && j < dimension) {
                product = 1;
                dimensionLength[j++]++;
                for (int i = 0; i < dimension; i++) {
                    product *= dimensionLength[i];
                }
            }
        }
        return dimensionLength;
    }

    @Override
    public String toString() {
        return
            "XPIR Parameters :" + "\n" +
            "  - elements per BFV plaintext : " + Arrays.toString(elementSizeOfPlaintext) + "\n" +
            "  - dimensions for d-dimensional hyperrectangle : " + dimensionsLength[0].length + "\n" +
            "  - number of BFV plaintexts (before padding) : " + Arrays.toString(plaintextSize);
    }
}
