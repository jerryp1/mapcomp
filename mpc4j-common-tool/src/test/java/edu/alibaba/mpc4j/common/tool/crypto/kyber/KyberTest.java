package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Kyber加密解密测试。
 *
 * @author Sheng Hu
 * @date 2021/12/31
 */


public class KyberTest {
    /**
     * 测试传递的密钥为byte时的加密/解密函数
     */
    @Test
    public void testKyberEncryptionDecryption_1(){
        for(int k = 2;k <= 4;k++){
            byte[] testBytes = new byte[32];
            byte[] coins = new byte[32];
            KyberPackedPki packedPki = Indcpa.generateKyberKeys(k);
            SecureRandom sr = new SecureRandom();
            sr.nextBytes(testBytes);
            byte[] cipherText = Indcpa.encrypt(testBytes,packedPki.getPackedPublicKey(),coins,k);
            byte[] plainText = Indcpa.decrypt(cipherText,packedPki.getPackedPrivateKey(),k);
            for(int index = 0;index < 32;index++){
                Assert.assertEquals(testBytes[index],plainText[index]);
            }
            Assert.assertEquals(Arrays.toString(testBytes),Arrays.toString(plainText));
        }
    }
    /**
     * 测试传递的密钥为short时的加密/解密函数
     */
    @Test
    public void testKyberEncryptionDecryption_2(){
        for(int k = 2;k <= 4;k++){
            byte[] testBytes = new byte[32];
            byte[] coins = new byte[32];
            KyberVecPki packedPki = KyberKeyOps.generateKyberKeys(k);
            SecureRandom sr = new SecureRandom();
            sr.nextBytes(testBytes);
            byte[] cipherText = KyberKeyOps.encrypt(testBytes,packedPki.getPublicKeyVec(),packedPki.getPublicKeyGenerator(),coins,k);
            byte[] plainText = KyberKeyOps.decrypt(cipherText,packedPki.getPrivateKeyVec(),k);
            Assert.assertEquals(Arrays.toString(testBytes),Arrays.toString(plainText));
        }
    }
}
