package edu.alibaba.mpc4j.s2pc.pir.index.sealpir;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirParams;

/**
 * SEAL PIR协议参数。
 *
 * @author Liqiang Peng
 * @date 2023/1/17
 */
public class Acls18IndexPirParams implements IndexPirParams {
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
     * 密文和明文的比例。
     */
    private final int expansionRatio;


    public Acls18IndexPirParams(int polyModulusDegree, int plainModulusBitLength, int dimension) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        this.dimension = dimension;
        // 生成加密方案参数
        this.encryptionParams = Acls18IndexPirNativeUtils.generateSealContext(
            polyModulusDegree, (1L << plainModulusBitLength) + 1
        );
        this.expansionRatio = Acls18IndexPirNativeUtils.expansionRatio(this.encryptionParams);
    }

    /**
     * 默认参数
     */
    public static Acls18IndexPirParams DEFAULT_PARAMS = new Acls18IndexPirParams(4096, 20, 2);

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

    /**
     * 返回密文和明文的比例。
     *
     * @return 密文和明文的比例。
     */
    public int getExpansionRatio() {
        return expansionRatio;
    }

    @Override
    public String toString() {
        return
            "SEAL encryption parameters : " + "\n" +
            " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
            " - size of plaintext modulus : " + plainModulusBitLength;
    }
}
