package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;


/**
 * OnionPIR协议参数。
 *
 * @author Liqiang Peng
 * @date 2022/11/11
 */
public class Mcr21IndexPirParams implements IndexPirParams {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 第一维度向量长度
     */
    private final int firstDimensionSize;
    /**
     * 其余维度向量长度
     */
    private static final int SUBSEQUENT_DIMENSION_SIZE = 4;
    /**
     * 明文模数比特长度
     */
    private static final int PLAIN_MODULUS_BIT_LENGTH = 54;
    /**
     * 多项式阶
     */
    private static final int POLY_MODULUS_DEGREE = 4096;
    /**
     * 维数
     */
    private int[] dimension;
    /**
     * 加密方案参数
     */
    private final byte[] encryptionParams;
    /**
     * 多项式里的元素数量
     */
    private int[] elementSizeOfPlaintext;
    /**
     * 多项式数量
     */
    private int[] plaintextSize;
    /**
     * 各维度的向量长度
     */
    private int[][] dimensionsLength;
    /**
     * 数据库分块数量
     */
    private int binNum;
    /**
     * 分块的最长字节长度
     */
    private int binMaxByteLength;
    /**
     * 最后一个分块的字节长度
     */
    private int lastBinByteLength;

    public Mcr21IndexPirParams(int firstDimensionSize) {
        assert (firstDimensionSize <= 512) : "first dimension is too large";
        this.firstDimensionSize = firstDimensionSize;
        // 生成加密方案参数
        this.encryptionParams = Mcr21IndexPirNativeUtils.generateSealContext(
            POLY_MODULUS_DEGREE, PLAIN_MODULUS_BIT_LENGTH
        );
    }

    /**
     * 初始化参数。
     *
     * @param serverElementSize 服务端元素数量。
     * @param elementByteLength 元素字节长度。
     */
    public synchronized void initMcr21IndexPirParams(int serverElementSize, int elementByteLength) {
        // 一个多项式可表示的字节长度
        this.binMaxByteLength = POLY_MODULUS_DEGREE * PLAIN_MODULUS_BIT_LENGTH / Byte.SIZE;
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
            elementSizeOfPlaintext[i] =
                IndexPirUtils.elementSizeOfPlaintext(byteLength, POLY_MODULUS_DEGREE, PLAIN_MODULUS_BIT_LENGTH);
            // 多项式数量
            plaintextSize[i] = (int) Math.ceil((double)serverElementSize / elementSizeOfPlaintext[i]);
            // 各维度的向量长度
            dimensionsLength[i] = computeDimensionLength(plaintextSize[i]);
            dimension[i] = dimensionsLength[i].length;
        });
    }

    /**
     * 默认参数
     */
    public static Mcr21IndexPirParams DEFAULT_PARAMS = new Mcr21IndexPirParams(128);

    /**
     * 返回明文模数比特长度。
     *
     * @return 明文模数比特长度。
     */
    @Override
    public int getPlainModulusBitLength() {
        return PLAIN_MODULUS_BIT_LENGTH;
    }

    /**
     * 返回多项式阶。
     *
     * @return 多项式阶。
     */
    @Override
    public int getPolyModulusDegree() {
        return POLY_MODULUS_DEGREE;
    }

    @Override
    public int getDimension() {
        return dimension[0];
    }

    /**
     * 返回加密方案参数。
     *
     * @return 加密方案参数。
     */
    public byte[] getEncryptionParams() {
        return encryptionParams;
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
     * 返回GSW密文参数。
     *
     * @return RGSW密文参数。
     */
    public int getGswDecompSize() {
        return 7;
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
    private int[] computeDimensionLength(int elementSize) {
        ArrayList<Integer> dimensionLength = new ArrayList<>();
        dimensionLength.add(firstDimensionSize);
        int product = firstDimensionSize;
        for (int i = elementSize / firstDimensionSize; i >= SUBSEQUENT_DIMENSION_SIZE; i /= SUBSEQUENT_DIMENSION_SIZE) {
            dimensionLength.add(SUBSEQUENT_DIMENSION_SIZE);
            product *= SUBSEQUENT_DIMENSION_SIZE;
        }
        int dimensionSize = dimensionLength.size();
        int[] dimensionArray = IntStream.range(0, dimensionSize).map(dimensionLength::get).toArray();
        while (product < elementSize) {
            dimensionArray[dimensionSize - 1]++;
            product = 1;
            product *= Arrays.stream(dimensionArray, 0, dimensionSize).reduce(1, (a, b) -> a * b);
        }
        if (dimensionSize == 1 && dimensionArray[0] > firstDimensionSize) {
            dimensionArray = new int[] {firstDimensionSize, SUBSEQUENT_DIMENSION_SIZE};
        }
        return dimensionArray;
    }

    @Override
    public String toString() {
        return "OnionPIR Parameters :" + "\n" +
            "  - elements per BFV plaintext : " + Arrays.toString(elementSizeOfPlaintext) + "\n" +
            "  - dimensions for d-dimensional hyperrectangle : " + Arrays.toString(dimension) + "\n" +
            "  - number of BFV plaintexts (before padding) : " + Arrays.toString(plaintextSize) + "\n" +
            "\n" +
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + POLY_MODULUS_DEGREE + "\n" +
            " - size of plaintext modulus : " + PLAIN_MODULUS_BIT_LENGTH + "\n";
    }
}