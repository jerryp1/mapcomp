package edu.alibaba.mpc4j.common.tool.crypto.kyber;

/**
 * Kyber运算接口。
 *
 * @author Sheng Hu
 * @date 2022/09/01
 */
public interface Kyber {
    /**
     * 返回1个随机的Kyber部分公钥（As+e）
     *
     * @return 随机Kyber公钥。
     */
    short[][] getRandomKyberPk();

    /**
     * 将{@code short[][]}表示的数据映射到Kyber部分公钥域上
     *
     * @param inputVector 数据
     * @return Hash后的公钥
     */
    short[][] hashToKyberPk(short[][] inputVector);

    /**
     * 将{@code short[][]}表示的数据映射到Kyber部分公钥域上
     *
     * @param inputVector 数据
     * @return Hash后的公钥
     */
    short[][] hashToKyberPk(byte[] inputVector);

    /**
     * 将{@code short[][]}表示的数据映射到Kyber部分公钥域上
     *
     * @param m         加密的消息
     * @param publicKey 公钥（（As+e），p）
     * @return 密文
     */
    byte[] encrypt(byte[] m, byte[] publicKey);

    /**
     * 将{@code short[][]}表示的数据映射到Kyber部分公钥域上
     *
     * @param m                  加密的消息
     * @param publicKey          部分公钥（As+e)
     * @param publicKeyGenerator 生成元
     * @return 密文
     */
    byte[] encrypt(byte[] m, short[][] publicKey, byte[] publicKeyGenerator);

    /**
     * Decrypt the given byte array using the Kyber public-key encryption scheme
     *
     * @param packedCipherText 压缩，打包后的密文
     * @param privateKey       私钥
     * @return 消息m
     */
    byte[] decrypt(byte[] packedCipherText, byte[] privateKey);

    /**
     * Decrypt the given byte array using the Kyber public-key encryption scheme
     *
     * @param packedCipherText 压缩，打包后的密文
     * @param privateKey       私钥
     * @return 消息m
     */
    byte[] decrypt(byte[] packedCipherText, short[][] privateKey);

    /**
     * 计算两个公钥的和
     *
     * @param keyA 多项式A的参数
     * @param keyB 多项式B的参数
     * @return 多项式A+B的参数
     */
    short[][] kyberPkAdd(short[][] keyA, short[][] keyB);

    /**
     * 计算两个公钥的和
     *
     * @param keyA 多项式A的参数
     * @param keyB 多项式B的参数
     * @return 多项式A+B的参数
     */
    void kyberPkAddi(short[][] keyA, short[][] keyB);

    /**
     * 计算两个公钥的差
     *
     * @param keyA 多项式A的参数
     * @param keyB 多项式B的参数
     * @return 多项式A-B的参数
     */
    short[][] kyberPkSub(short[][] keyA, short[][] keyB);

    /**
     * 计算两个公钥的差,返回值为A
     *
     * @param keyA 多项式A的参数
     * @param keyB 多项式B的参数
     */
    void kyberPkSubi(short[][] keyA, short[][] keyB);

    /**
     * Generates public and private keys for the CPA-secure public-key
     * encryption scheme underlying Kyber.
     *
     * @return 论文中的公钥（As+e,p）和私钥s
     */
    KyberPackedPki generateKyberByteKeys();

    /**
     * Generates public and private keys for the CPA-secure public-key
     * encryption scheme underlying Kyber.
     *
     * @return 论文中的公钥（As+e,p）和私钥s
     */
    KyberVecKeyPair generateKyberVecKeys();
}
