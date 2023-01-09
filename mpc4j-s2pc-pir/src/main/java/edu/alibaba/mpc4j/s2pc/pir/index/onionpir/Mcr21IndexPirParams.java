package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.AbstractIndexPirParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;


/**
 * OnionPIR协议参数。
 *
 * @author Liqiang Peng
 * @date 2022/11/11
 */
public class Mcr21IndexPirParams extends AbstractIndexPirParams {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 第一维度向量长度
     */
    private static final int FIRST_DIMENSION_SIZE = 128;
    /**
     * 其余维度向量长度
     */
    private static final int SUBSEQUENT_DIMENSION_SIZE = 4;
    /**
     * 明文模数比特长度
     */
    private final int plainModulusBitLength = 40;
    /**
     * 多项式阶
     */
    private final int polyModulusDegree = 8192;
    /**
     * GSW密文参数
     */
    private final int gswDecompSize = 7;
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
    private final int elementSizeOfPlaintext;
    /**
     * 多项式数量
     */
    private final int plaintextSize;
    /**
     * 各维度的向量长度
     */
    private final int[] dimensionsLength;


    public Mcr21IndexPirParams(int serverElementSize, int elementByteLength) {
        // 生成加密方案参数
        this.encryptionParams = Mcr21IndexPirNativeUtils.generateSealContext(polyModulusDegree, plainModulusBitLength);
        // 一个多项式可以包含的元素数量
        this.elementSizeOfPlaintext = elementSizeOfPlaintext(elementByteLength, polyModulusDegree, plainModulusBitLength);
        // 多项式数量
        this.plaintextSize = (int) Math.ceil((double) serverElementSize / this.elementSizeOfPlaintext);
        // 各维度的向量长度
        this.dimensionsLength = computeDimensionLength();
        this.dimension = this.dimensionsLength.length;
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
    public int[] getDimensionsLength() {
        return dimensionsLength;
    }

    /**
     * 返回多项式数量。
     *
     * @return 多项式数量。
     */
    public int getPlaintextSize() {
        return plaintextSize;
    }

    /**
     * 返回多项式里的元素数量。
     *
     * @return 多项式里的元素数量。
     */
    public int getElementSizeOfPlaintext() {
        return elementSizeOfPlaintext;
    }

    /**
     * 返回数据库编码后每个维度的长度。
     *
     * @return 数据库编码后每个维度的长度。
     */
    private int[] computeDimensionLength() {
        ArrayList<Integer> dimensionLength = new ArrayList<>();
        dimensionLength.add(FIRST_DIMENSION_SIZE);
        int product = FIRST_DIMENSION_SIZE;
        for (int i = plaintextSize / FIRST_DIMENSION_SIZE; i >= SUBSEQUENT_DIMENSION_SIZE; i /= SUBSEQUENT_DIMENSION_SIZE) {
            dimensionLength.add(SUBSEQUENT_DIMENSION_SIZE);
            product *= SUBSEQUENT_DIMENSION_SIZE;
        }
        int dimensionSize = dimensionLength.size();
        int[] dimensionArray = IntStream.range(0, dimensionSize).map(dimensionLength::get).toArray();
        while (product < plaintextSize) {
            dimensionArray[dimensionSize - 1]++;
            product = 1;
            product *= Arrays.stream(dimensionArray, 0, dimensionSize).reduce(1, (a, b) -> a * b);
        }
        return dimensionArray;
    }

    @Override
    public String toString() {
        int product = Arrays.stream(dimensionsLength).reduce(1, (a, b) -> a * b);
        return "OnionPIR Parameters :" + "\n" +
            "  - elements per BFV plaintext : " + elementSizeOfPlaintext + "\n" +
            "  - dimensions for d-dimensional hyperrectangle : " + dimension + "\n" +
            "  - number of BFV plaintexts (before padding) : " + plaintextSize + "\n" +
            "  - number of BFV plaintexts after padding (to fill d-dimensional hyperrectangle) : " + product + "\n" +
            "\n" +
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
            " - size of plaintext modulus : " + plainModulusBitLength + "\n";
    }

    /**
     * 返回GSW密文参数。
     *
     * @return RGSW密文参数。
     */
    public int getGswDecompSize() {
        return gswDecompSize;
    }
}
