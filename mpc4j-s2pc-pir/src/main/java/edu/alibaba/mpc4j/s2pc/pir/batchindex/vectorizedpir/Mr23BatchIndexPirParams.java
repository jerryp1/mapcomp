package edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;

/**
 * Vectorized PIR协议参数。
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
     * 维数
     */
    private final int dimension;
    /**
     * SEAL上下文参数
     */
    private final byte[] encryptionParams;
    /**
     * 哈希个数
     */
    private final int hashNum;

    public Mr23BatchIndexPirParams(int polyModulusDegree, int plainModulusBitLength, int[] coeffModulusBitLength,
                                   int dimension, int hashNum) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        this.dimension = dimension;
        // 生成加密方案参数
        this.encryptionParams = Mr23BatchIndexPirNativeUtils.generateSealContext(
            polyModulusDegree, plainModulusBitLength, coeffModulusBitLength
        );
        this.hashNum = hashNum;
    }

    /**
     * 默认参数
     */
    public static Mr23BatchIndexPirParams DEFAULT_PARAMS = new Mr23BatchIndexPirParams(
        8192,
        20,
        new int[]{60, 50, 50, 50},
        3,
        3
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
        return dimension;
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

    public int getHashNum() {
        return hashNum;
    }
}
