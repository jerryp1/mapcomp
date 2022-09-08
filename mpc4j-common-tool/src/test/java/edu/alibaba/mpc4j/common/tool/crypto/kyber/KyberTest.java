package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j.KyberKeyPairJava;
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
     * 测试IDN-CPA方案
     */
    @Test
    public void testKyberCpa() {
        SecureRandom secureRandom = new SecureRandom();
        for (int k = 2; k <= 4; k++) {
            byte[] testBytes = new byte[32];
            secureRandom.nextBytes(testBytes);
            Kyber kyber = KyberFactory.createInstance(KyberFactory.KyberType.KYBER_CPA, k);
            KyberKeyPairJava keyPair = kyber.generateKyberVecKeys();
            byte[] cipherText = kyber.encaps(testBytes, keyPair.getPublicKeyBytes(), keyPair.getPublicKeyGenerator());
            byte[] plainText = kyber.decaps(cipherText, keyPair.getPrivateKeyVec(), keyPair.getPublicKeyBytes(), keyPair.getPublicKeyGenerator());
            for (int index = 0; index < 32; index++) {
                Assert.assertEquals(testBytes[index], plainText[index]);
            }
            Assert.assertEquals(Arrays.toString(testBytes), Arrays.toString(plainText));
        }
    }
    /**
     * 测试CCA_KEM方案
     */
    @Test
    public void testKyberCca() {
        SecureRandom secureRandom = new SecureRandom();
        for (int k = 2; k <= 4; k++) {
            Kyber kyber = KyberFactory.createInstance(KyberFactory.KyberType.KYBER_CCA, k);
            byte[] testBytes = new byte[16];
            secureRandom.nextBytes(testBytes);
            KyberKeyPairJava keyPair = kyber.generateKyberVecKeys();
            byte[] cipherText = kyber.encaps(testBytes, keyPair.getPublicKeyBytes(), keyPair.getPublicKeyGenerator());
            byte[] secretText = kyber.decaps(cipherText, keyPair.getPrivateKeyVec(), keyPair.getPublicKeyBytes(), keyPair.getPublicKeyGenerator());
            Assert.assertEquals(Arrays.toString(testBytes), Arrays.toString(secretText));
        }
    }
}
