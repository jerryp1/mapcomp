package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19;

import java.util.ArrayList;

/**
 * RSS19-核zp64三元组生成协议本地发送方。
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public class Rss19Zp64CoreMtgNativeSender {

    /**
     * 密钥生成。
     *
     * @param polyModulusDegree 模多项式阶。
     * @param plainModulus      明文模数。
     * @return 加密方案参数和密钥。
     */
    static native ArrayList<byte[]> keyGen(int polyModulusDegree, long plainModulus);

    /**
     * 加密函数。
     *
     * @param encryptionParams 加密方案参数。
     * @param publicKey        公钥。
     * @param secretKey        私钥。
     * @param plain1           明文1。
     * @param plain2           明文2。
     * @return 密文。
     */
    static native ArrayList<byte[]> encryption(byte[] encryptionParams, byte[] publicKey, byte[] secretKey, long[] plain1,
                                    long[] plain2);

    /**
     * 解密函数。
     *
     * @param encryptionParams 加密方案参数。
     * @param secretKey        私钥。
     * @param ciphertext       密文。
     * @return 明文。
     */
    static native long[] decryption(byte[] encryptionParams, byte[] secretKey, byte[] ciphertext);
}
