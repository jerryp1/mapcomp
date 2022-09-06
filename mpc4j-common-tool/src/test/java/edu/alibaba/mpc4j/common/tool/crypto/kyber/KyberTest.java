package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
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
    public void testKyberEncryptionDecryption_1() {
        SecureRandom secureRandom = new SecureRandom();
        Hash hashFunction = HashFactory.createInstance(HashFactory.HashType.BC_BLAKE_2B_160, 16);
        for (int k = 2; k <= 4; k++) {
            byte[] testBytes = new byte[32];
            secureRandom.nextBytes(testBytes);
            Kyber kyber = KyberFactory.createInstance(KyberFactory.KyberType.KYBER_CPA, k, secureRandom,hashFunction);
            KyberKey keyPair = kyber.generateKyberVecKeys();
            byte[] cipherText = kyber.encrypt(testBytes, keyPair.getPublicKeyBytes(), keyPair.getPublicKeyGenerator());
            byte[] plainText = kyber.decrypt(cipherText, keyPair.getPrivateKeyVec());
            for (int index = 0; index < 32; index++) {
                Assert.assertEquals(testBytes[index], plainText[index]);
            }
            Assert.assertEquals(Arrays.toString(testBytes), Arrays.toString(plainText));
        }
    }

    @Test
    public void testKyberRandom() {
        SecureRandom secureRandom = new SecureRandom();
        Hash hashFunction = HashFactory.createInstance(HashFactory.HashType.BC_BLAKE_2B_160, 16);
        for (int k = 2; k <= 4; k++) {
            byte[] testBytes = new byte[32];
            secureRandom.nextBytes(testBytes);
            Kyber kyber = KyberFactory.createInstance(KyberFactory.KyberType.KYBER_CPA, k, secureRandom,hashFunction);
            for (int index = 0; index < 32; index++) {
                //测试一下是不是每次都不一样hh
                System.out.println(Arrays.toString(kyber.getRandomKyberPk()));
            }
        }
    }
}
