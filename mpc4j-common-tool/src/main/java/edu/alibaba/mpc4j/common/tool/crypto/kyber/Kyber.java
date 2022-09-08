package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j.KyberKeyPairJava;

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
    byte[] getRandomKyberPk();

    /**
     * 将{@code short[][]}表示的数据映射到一个byte上
     *
     * @param inputBytes 数据
     * @return Hash后的公钥
     */
    byte[] hashToByte(byte[] inputBytes);


    /**
     * 将{@code short[][]}表示的数据映射到Kyber部分公钥域上
     *
     * @param k                  加密的消息
     * @param publicKey          部分公钥（As+e)
     * @param publicKeyGenerator 生成元
     * @return 密文
     */
    byte[] encaps(byte[] k, short[][] publicKey, byte[] publicKeyGenerator);

    /**
     * 将{@code short[][]}表示的数据映射到Kyber部分公钥域上
     *
     * @param k                  加密的消息
     * @param publicKey          部分公钥（As+e)
     * @param publicKeyGenerator 生成元
     * @return 密文
     */
    byte[] encaps(byte[] k, byte[] publicKey, byte[] publicKeyGenerator);

    /**
     * Decrypt the given byte array using the Kyber public-key encryption scheme
     *
     * @param packedCipherText 压缩，打包后的密文
     * @param privateKey       私钥
     * @param publicKeyBytes 公钥
     * @param publicKeyGenerator 生成元
     * @return 秘密值k
     */
    byte[] decaps(byte[] packedCipherText, short[][] privateKey, byte[] publicKeyBytes, byte[] publicKeyGenerator);

    /**
     * Generates public and private keys for the CPA-secure public-key
     * encryption scheme underlying Kyber.
     *
     * @return 论文中的公钥（As+e,p）和私钥s
     */
    KyberKeyPairJava generateKyberVecKeys();

    /**
     * 将公钥打包传输，担心公钥的长度不一定，所以都放在了Kyber类里面
     *
     * @param publicKeyBytes     遮掩后的As+e
     * @param randomKeyByte      随机的As+e
     * @param publicKeyGenerator 生成元
     * @param sigma              2选一OT的选择
     * @return 多项式向量
     */
    byte[][] packageTwoKeys(byte[] publicKeyBytes, byte[] randomKeyByte, byte[] publicKeyGenerator, int sigma);

    /**
     * 将公钥打包传输，担心公钥的长度不一定，所以都放在了Kyber类里面
     *
     * @param publicKeyBytes     遮掩后的As+e
     * @param randomKeyByte      随机的As+e
     * @param publicKeyGenerator 生成元
     * @param choice             n选一OT的选择
     * @param n                  n选一OT的n
     * @return 多项式向量
     */
    byte[][] packageNumKeys(byte[] publicKeyBytes, byte[][] randomKeyByte, byte[] publicKeyGenerator, int choice, int n);
}
