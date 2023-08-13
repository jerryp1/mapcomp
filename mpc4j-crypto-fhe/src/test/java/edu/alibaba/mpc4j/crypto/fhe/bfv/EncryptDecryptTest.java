package edu.alibaba.mpc4j.crypto.fhe.bfv;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * @author Qixian Zhou
 * @date 2023/7/17
 */
@RunWith(Parameterized.class)
public class EncryptDecryptTest {


    private static int LOOP_NUMS = 1;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();


        configurations.add(new Object[]{
                "TinyEncryptDecrypt",
                new BfvParameters(5, 60, 50000),
        });

        configurations.add(new Object[]{
                "EncryptDecrypt(N=2048)",
                new BfvParameters(2048, 256, 0x3fffffff000001L),
        });

        configurations.add(new Object[]{
                "EncryptDecrypt(N=4096)",
                new BfvParameters(4096, 256, 0x3fffffff000001L),
        });
        configurations.add(new Object[]{
                "EncryptDecrypt(N=8192)",
                new BfvParameters(8192, 256, 0x3fffffff000001L),
        });
        configurations.add(new Object[]{
                "EncryptDecrypt(N=16384)",
                new BfvParameters(16384, 256, 0x3fffffff000001L),
        });

        return configurations;
    }

    private final BfvParameters param;
    private final BfvEncryptor encryptor;
    private final BfvDecryptor decryptor;

    public EncryptDecryptTest(String name, BfvParameters param) {
        BfvKeyGenerator keyGenerator = new BfvKeyGenerator(param);
        this.param = param;
        PublicKey publicKey = keyGenerator.publicKey;
        SecretKey secretKey = keyGenerator.secretKey;
        this.encryptor = new BfvEncryptor(param, publicKey);
        this.decryptor = new BfvDecryptor(param, secretKey);
    }


    @Test
    public void testEncryptDecrypt() {
        for (int i = 0; i < LOOP_NUMS; i++) {
            long[] coeffs = randomCoeffs();
            encryptDecrypt(coeffs);
        }
    }


    public void encryptDecrypt(long[] coeffs) {

        Plaintext pt = new Plaintext(param.polyModulusDegree, coeffs);
        Ciphertext ct = this.encryptor.encrypt(pt);
        Plaintext ptDecrypt = decryptor.decrypt(ct);

        assert pt.equals(ptDecrypt);
    }

    public long[] randomCoeffs() {

        Random random = new Random();

        long[] coeffs = new long[(int) param.polyModulusDegree];
        for (int i = 0; i < param.polyModulusDegree; i++) {
            coeffs[i] = random.nextInt((int) param.plainModulus);
        }
        return coeffs;
    }


//    @Test
//    public void testMathRound() {
//        assert Math.round(1.5) == 2;
//        assert Math.round(0) == 0;
//        assert Math.round(2.4) == 2;
//        assert Math.round(-1.5) == -1;
//        assert Math.round(-1.4) == -1;
//        assert Math.round(-1.55) == -2;
//    }


}
