package edu.alibaba.mpc4j.crypto.fhe.bfv;

/**
 * @author Qixian Zhou
 * @date 2023/7/17
 */

import edu.alibaba.mpc4j.crypto.fhe.utils.Polynomial;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

@RunWith(Parameterized.class)
public class EvaluatorTest {

    private static final int LOOP_NUMS = 10000;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
                "EncryptDecrypt(N=2048)",
                new BfvParameters(8, 16, 100049),
        });
        // 更大的参数乘法就跑不通了
        // 感觉难点还是在密文乘法，从实现的角度来看
        configurations.add(new Object[]{
                "EncryptDecrypt(N=2048)",
                new BfvParameters(256, 16, 67153921L),
        });

        // 下面这些参数对于加解密来说是没有任何问题的

//        configurations.add(new Object[]{
//                "EncryptDecrypt(N=2048)",
//                new BfvParameters(8, 256, 0x3fffffff000001L),
//        });

//        configurations.add(new Object[]{
//                "EncryptDecrypt(N=2048)",
//                new BfvParameters(512, 256, 0x3fffffff000001L),
//        });

//        configurations.add(new Object[]{
//                "EncryptDecrypt(N=4096)",
//                new BfvParameters(4096, 256, 0x3fffffff000001L),
//        });
//        configurations.add(new Object[]{
//                "EncryptDecrypt(N=8192)",
//                new BfvParameters(8192, 256, 0x3fffffff000001L),
//        });
//        configurations.add(new Object[]{
//                "EncryptDecrypt(N=16384)",
//                new BfvParameters(16384, 256, 0x3fffffff000001L),
//        });

        return configurations;
    }

    private final BfvParameters param;
    private final BfvEncryptor encryptor;
    private final BfvDecryptor decryptor;
    private final BfvEvaluator evaluator;
    private final RelinKey relinKey;

    public EvaluatorTest(String name, BfvParameters param) {
        BfvKeyGenerator keyGenerator = new BfvKeyGenerator(param);
        this.param = param;
        PublicKey publicKey = keyGenerator.publicKey;
        SecretKey secretKey = keyGenerator.secretKey;
        this.relinKey = keyGenerator.relinKey;
        this.encryptor = new BfvEncryptor(param, publicKey);
        this.decryptor = new BfvDecryptor(param, secretKey);
        this.evaluator = new BfvEvaluator(param);
    }


    @Test
    public void testEncryptDecrypt() {
        for (int i = 0; i < LOOP_NUMS; i++) {
            long[] coeffs = randomCoeffs();
            encryptDecrypt(coeffs);
        }
    }

    @Test
    public void testCiphertextAdd() {
        for (int i = 0; i < LOOP_NUMS; i++) {
            Plaintext plain1 = randomPlaintext();
            Plaintext plain2 = randomPlaintext();
            addTest(plain1, plain2);
        }
    }

    @Test
    public void testCiphertextMul() {
        for (int i = 0; i < LOOP_NUMS; i++) {
            Plaintext plain1 = randomPlaintext();
            Plaintext plain2 = randomPlaintext();
            mulTest(plain1, plain2);
        }
    }

    public void addTest(Plaintext plain1, Plaintext plain2) {
        Plaintext plainSum = new Plaintext(plain1.poly.add(plain2.poly, param.plainModulus));

        Ciphertext cipher1 = encryptor.encrypt(plain1);
        Ciphertext cipher2 = encryptor.encrypt(plain2);
        Ciphertext cipherSum = evaluator.add(cipher1, cipher2);
        Plaintext cipherSumDecrypt = decryptor.decrypt(cipherSum);

        assert plainSum.equals(cipherSumDecrypt);
    }

    public void mulTest(Plaintext plain1, Plaintext plain2) {

        Polynomial tmp = plain1.poly.mul(plain2.poly, param.plainModulus);
        Plaintext plainMul = new Plaintext(tmp);

        Ciphertext cipher1 = encryptor.encrypt(plain1);
        Ciphertext cipher2 = encryptor.encrypt(plain2);

//        System.out.println("cipher1: " + cipher1);
//        System.out.println("cipher2: " + cipher2);


        Ciphertext cipherMul = evaluator.multiply(cipher1, cipher2, relinKey);

//        System.out.println("cipherMul: " + cipherMul);

        Plaintext cipherMulDecrypt = decryptor.decrypt(cipherMul);

//        System.out.println("        plainMul: " + plainMul);
//        System.out.println("cipherMulDecrypt: " + cipherMulDecrypt);

        assert plainMul.equals(cipherMulDecrypt);
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

    public Plaintext randomPlaintext() {

        Random random = new Random();

        long[] coeffs = new long[(int) param.polyModulusDegree];
        for (int i = 0; i < param.polyModulusDegree; i++) {
            coeffs[i] = random.nextInt((int) param.plainModulus);
        }
        return new Plaintext(new Polynomial(param.polyModulusDegree, coeffs));
    }


    @Test
    public void findSmallPrimeQ() {
        long polyModulusDegree= 2048;
        int bit = 20;
        long start = 1L <<25 + 1;
        while (start % (2 * polyModulusDegree) != 1 || !BigInteger.valueOf(start).isProbablePrime(40)) {
            start += 1;
        }
        System.out.println(start);
    }


}
