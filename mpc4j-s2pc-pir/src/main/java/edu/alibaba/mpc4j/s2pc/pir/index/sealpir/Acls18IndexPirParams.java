package edu.alibaba.mpc4j.s2pc.pir.index.sealpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirParams;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * SEAL PIR协议参数。
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class Acls18IndexPirParams extends AbstractIndexPirParams {
    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 明文模数比特长度
     */
    private final int plainModulusBitLength;
    /**
     * 多项式阶
     */
    private final int polyModulusDegree;
    /**
     * 维数
     */
    private final int dimension;
    /**
     * 加密方案参数
     */
    private final byte[] encryptionParams;
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
    private final int bundleNum;
    /**
     * 密文和明文的比例。
     */
    private final int expansionRatio;

    public Acls18IndexPirParams(int serverElementSize, int elementByteLength, int polyModulusDegree,
                                int plainModulusBitLength, int dimension) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        this.dimension = dimension;
        // 一个多项式可表示的字节长度
        int maxElementByteLength = polyModulusDegree * plainModulusBitLength / Byte.SIZE;
        // 数据库分块数量
        this.bundleNum = (elementByteLength + maxElementByteLength) / maxElementByteLength;
        this.elementSizeOfPlaintext = new int[this.bundleNum];
        this.plaintextSize = new int[this.bundleNum];
        this.dimensionsLength = new int[this.bundleNum][];
        // 生成加密方案参数
        this.encryptionParams = Acls18IndexPirNativeUtils.generateSealContext(polyModulusDegree, (1L << plainModulusBitLength) + 1);
        this.expansionRatio = Acls18IndexPirNativeUtils.expansionRatio(this.encryptionParams);
        IntStream.range(0, this.bundleNum).forEach(index -> {
            int bundleElementByteLength = index == this.bundleNum - 1 ? elementByteLength % maxElementByteLength : maxElementByteLength;
            // 一个多项式可以包含的元素数量
            this.elementSizeOfPlaintext[index] = elementSizeOfPlaintext(bundleElementByteLength, polyModulusDegree, plainModulusBitLength);
            // 多项式数量
            this.plaintextSize[index] = (int) Math.ceil((double) serverElementSize / this.elementSizeOfPlaintext[index]);
            // 各维度的向量长度
            this.dimensionsLength[index] = computeDimensionLength(this.plaintextSize[index]);
        });
    }

    /**
     * 返回明文模数比特长度。
     *
     * @return 明文模数比特长度。
     */
    public int getPlainModulusBitLength() {
        return plainModulusBitLength;
    }

    /**
     * 返回多项式阶。
     *
     * @return 多项式阶。
     */
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * 返回维数。
     *
     * @return 维数。
     */
    public int getDimension() {
        return dimension;
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
     * 返回分块数目。
     *
     * @return RGSW密文参数。
     */
    public int getBundleNum() {
        return this.bundleNum;
    }

    /**
     * 返回密文和明文的比例。
     *
     * @return 密文和明文的比例。
     */
    public int getExpansionRatio() {
        return expansionRatio;
    }

    /**
     * 返回数据库编码后每个维度的长度。
     *
     * @param elementSize 元素数量。
     * @return 数据库编码后每个维度的长度。
     */
    private int[] computeDimensionLength(int elementSize) {
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
        return "SealPIR Parameters :" + "\n" +
            "  - elements per BFV plaintext : " + Arrays.toString(elementSizeOfPlaintext) + "\n" +
            "  - dimensions for d-dimensional hyperrectangle : " + dimension + "\n" +
            "  - number of BFV plaintexts (before padding) : " + Arrays.toString(plaintextSize) + "\n" +
            "\n" +
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
            " - size of plaintext modulus : " + plainModulusBitLength + "\n";
    }
}
