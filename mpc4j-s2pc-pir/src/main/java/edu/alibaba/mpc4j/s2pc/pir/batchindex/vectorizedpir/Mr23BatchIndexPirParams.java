package edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;

/**
 * Vectorized Batch PIR协议参数。
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
public class Mr23BatchIndexPirParams implements IndexPirParams {

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
     * SEAL上下文参数
     */
    private final byte[] encryptionParams;
    /**
     * 哈希数目
     */
    private final int hashNum;
    /**
     * 前两维长度
     */
    private final int firstTwoDimensionSize;
    /**
     * 第三维长度
     */
    private final int thirdDimensionSize;

    public Mr23BatchIndexPirParams(int polyModulusDegree, int plainModulusBitLength, int[] coeffModulusBitLength,
                                   int firstTwoDimensionSize, int thirdDimensionSize, int hashNum) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        assert firstTwoDimensionSize == PirUtils.getNextPowerOfTwo(firstTwoDimensionSize);
        this.firstTwoDimensionSize = firstTwoDimensionSize;
        this.thirdDimensionSize = thirdDimensionSize;
        // 生成加密方案参数
        this.encryptionParams = Mr23BatchIndexPirNativeUtils.generateSealContext(
            polyModulusDegree, plainModulusBitLength, coeffModulusBitLength
        );
        this.hashNum = hashNum;
    }

    /**
     * 默认参数，适合分桶数目256
     */
    public static Mr23BatchIndexPirParams DEFAULT_PARAMS_BIN_SIZE_256 = new Mr23BatchIndexPirParams(
        8192,
        20,
        new int[]{50, 55, 55, 50},
        32, 9, 3
    );

    /**
     * 默认参数，适合分桶数目512
     */
    public static Mr23BatchIndexPirParams DEFAULT_PARAMS_BIN_SIZE_512 = new Mr23BatchIndexPirParams(
        8192,
        20,
        new int[]{50, 55, 55, 50},
        32, 5, 3
    );

    /**
     * 默认参数，适合分桶数目1024
     */
    public static Mr23BatchIndexPirParams DEFAULT_PARAMS_BIN_SIZE_1024 = new Mr23BatchIndexPirParams(
        8192,
        20,
        new int[]{50, 55, 55, 50},
        16, 9, 3
    );

    /**
     * 默认参数，适合分桶数目1024
     */
    public static Mr23BatchIndexPirParams DEFAULT_PARAMS_BIN_SIZE_2048 = new Mr23BatchIndexPirParams(
        8192,
        20,
        new int[]{50, 55, 55, 50},
        16, 5, 3
    );

    @Override
    public int getPlainModulusBitLength() {
        return plainModulusBitLength;
    }

    @Override
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    @Override
    public int getDimension() {
        return 3;
    }

    @Override
    public byte[] getEncryptionParams() {
        return encryptionParams;
    }

    @Override
    public String toString() {
        return
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
            " - size of plaintext modulus : " + plainModulusBitLength;
    }

    public int getFirstTwoDimensionSize() {
        return firstTwoDimensionSize;
    }

    public int getThirdDimensionSize() {
        return thirdDimensionSize;
    }

    /**
     * 返回哈希数目。
     *
     * @return 哈希数目。
     */
    public int getHashNum() {
        return hashNum;
    }
}
