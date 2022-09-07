package edu.alibaba.mpc4j.common.tool.crypto.kyber;

/**
 * Kyber密钥接口
 *
 * @author Sheng Hu
 * @date 2022/09/06
 */
public interface KyberKey {
    /**
     * 返回kyber的公钥
     *
     * @return kyber的公钥
     */
    byte[] getPublicKeyBytes();

    /**
     * 返回kyber的私钥
     *
     * @return kyber的私钥
     */
    short[][] getPrivateKeyVec();

    /**
     * 返回kyber的公钥生成元
     *
     * @return kyber的公钥生成元
     */
    byte[] getPublicKeyGenerator();

    /**
     * 返回kyber的公钥
     */
    void generateKyberKeys();
}
