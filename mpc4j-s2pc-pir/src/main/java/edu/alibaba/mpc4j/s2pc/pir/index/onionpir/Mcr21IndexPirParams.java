package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;

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
    public final int SUBSEQUENT_DIMENSION_SIZE = 4;
    /**
     * 明文模数比特长度
     */
    private static final int PLAIN_MODULUS_BIT_LENGTH = 54;
    /**
     * 多项式阶
     */
    private static final int POLY_MODULUS_DEGREE = 4096;
    /**
     * 加密方案参数
     */
    private final byte[] encryptionParams;

    public Mcr21IndexPirParams(int firstDimensionSize) {
        assert (firstDimensionSize <= 512) : "first dimension is too large";
        this.firstDimensionSize = firstDimensionSize;
        // 生成加密方案参数
        this.encryptionParams = Mcr21IndexPirNativeUtils.generateSealContext(
            POLY_MODULUS_DEGREE, PLAIN_MODULUS_BIT_LENGTH
        );
    }

    /**
     * 默认参数
     */
    public static Mcr21IndexPirParams DEFAULT_PARAMS = new Mcr21IndexPirParams(128);

    @Override
    public int getPlainModulusBitLength() {
        return PLAIN_MODULUS_BIT_LENGTH;
    }

    @Override
    public int getPolyModulusDegree() {
        return POLY_MODULUS_DEGREE;
    }

    @Override
    public int getDimension() {
        return firstDimensionSize;
    }

    public int getFirstDimensionSize() {
        return firstDimensionSize;
    }

    public int getSubsequentDimensionSize() {
        return SUBSEQUENT_DIMENSION_SIZE;
    }

    @Override
    public byte[] getEncryptionParams() {
        return encryptionParams;
    }

    /**
     * 返回GSW密文参数。
     *
     * @return RGSW密文参数。
     */
    public int getGswDecompSize() {
        return 7;
    }

    @Override
    public String toString() {
        return
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + POLY_MODULUS_DEGREE + "\n" +
            " - size of plaintext modulus : " + PLAIN_MODULUS_BIT_LENGTH;
    }
}