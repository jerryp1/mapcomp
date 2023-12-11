package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.keys.GaloisKeys;
import edu.alibaba.mpc4j.crypto.fhe.keys.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.keys.RelinKeys;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.PlainModulus;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
/**
 * @author Qixian Zhou
 * @date 2023/10/22
 */
public class EvaluatorParallelTest {


    private static final int MAX_LOOP = 1;

    @Test
    public void bfvEncryptRotateMatrixDecrypt() {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(257);
        parms.setPolyModulusDegree(8);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(8, new int[]{40, 40}));


        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);
        GaloisKeys galoisKeys = new GaloisKeys();
        keyGenerator.createGaloisKeys(galoisKeys);

        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());
        BatchEncoder batchEncoder = new BatchEncoder(context);


        Ciphertext encrypted = new Ciphertext(context);
        Plaintext plain = new Plaintext();
        long[] plainVec = new long[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        // 视为 uint64_t 进行编码
        batchEncoder.encode(plainVec, plain);
        encryptor.encrypt(plain, encrypted);
        // 列旋转是什么意思？
        evaluatorParallel.rotateColumnsInplace(encrypted, galoisKeys);


        decryptor.decrypt(encrypted, plain);
        batchEncoder.decode(plain, plainVec);
        Assert.assertArrayEquals(
                plainVec,
                new long[]{ 5, 6, 7, 8, 1, 2, 3, 4 }
        );

        evaluatorParallel.rotateRowsInplace(encrypted, -1, galoisKeys);

        System.out.println("rotateRowsInplace -1 noise budget: "
                + decryptor.invariantNoiseBudget(encrypted)
        );

        decryptor.decrypt(encrypted, plain);
        batchEncoder.decode(plain, plainVec);
        Assert.assertArrayEquals(
                plainVec,
                new long[]{ 8, 5, 6, 7, 4, 1, 2, 3}
        );

        evaluatorParallel.rotateRowsInplace(encrypted, 2, galoisKeys);

        System.out.println("rotateRowsInplace 2 noise budget: "
                + decryptor.invariantNoiseBudget(encrypted)
        );

        decryptor.decrypt(encrypted, plain);
        batchEncoder.decode(plain, plainVec);
        Assert.assertArrayEquals(
                plainVec,
                new long[]{ 6, 7, 8, 5, 2, 3, 4, 1}
        );

        evaluatorParallel.rotateColumnsInplace(encrypted, galoisKeys);

        System.out.println("rotateColumnsInplace  noise budget: "
                + decryptor.invariantNoiseBudget(encrypted)
        );

        decryptor.decrypt(encrypted, plain);
        batchEncoder.decode(plain, plainVec);
        Assert.assertArrayEquals(
                plainVec,
                new long[]{ 2, 3, 4, 1, 6, 7, 8, 5}
        );

        evaluatorParallel.rotateRowsInplace(encrypted, 0, galoisKeys);

        System.out.println("rotateRowsInplace 0  noise budget: "
                + decryptor.invariantNoiseBudget(encrypted)
        );

        decryptor.decrypt(encrypted, plain);
        batchEncoder.decode(plain, plainVec);
        Assert.assertArrayEquals(
                plainVec,
                new long[]{ 2, 3, 4, 1, 6, 7, 8, 5}
        );

    }


    @Test
    public void bfvEncryptApplyGaloisDecrypt() {
        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(257);
        parms.setPolyModulusDegree(8);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(8, new int[]{40, 40}));


        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);
        GaloisKeys galoisKeys = new GaloisKeys();
        keyGenerator.createGaloisKeys(new int[] {1, 3, 5, 15}, galoisKeys);

        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());


        Ciphertext encrypted = new Ciphertext(context);
        Plaintext plain = new Plaintext();

        plain.fromHexPoly("1");
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.applyGaloisInplace(encrypted, 1, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1"
        );
        evaluatorParallel.applyGaloisInplace(encrypted, 3, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1"
        );
        evaluatorParallel.applyGaloisInplace(encrypted, 5, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1"
        );
        evaluatorParallel.applyGaloisInplace(encrypted, 15, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1"
        );

        plain.fromHexPoly("1x^1");
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.applyGaloisInplace(encrypted, 1, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1x^1"
        );


        evaluatorParallel.applyGaloisInplace(encrypted, 3, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1x^3"
        );


        evaluatorParallel.applyGaloisInplace(encrypted, 5, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "100x^7"
        );

        evaluatorParallel.applyGaloisInplace(encrypted, 15, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1x^1"
        );


        plain.fromHexPoly("1x^2");
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.applyGaloisInplace(encrypted, 1, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1x^2"
        );


        evaluatorParallel.applyGaloisInplace(encrypted, 3, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1x^6"
        );


        evaluatorParallel.applyGaloisInplace(encrypted, 5, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "100x^6"
        );

        evaluatorParallel.applyGaloisInplace(encrypted, 15, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1x^2"
        );


        plain.fromHexPoly("1x^3 + 2x^2 + 1x^1 + 1");
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.applyGaloisInplace(encrypted, 1, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1x^3 + 2x^2 + 1x^1 + 1"
        );


        evaluatorParallel.applyGaloisInplace(encrypted, 3, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "2x^6 + 1x^3 + 100x^1 + 1"
        );


        evaluatorParallel.applyGaloisInplace(encrypted, 5, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "100x^7 + FFx^6 + 100x^5 + 1"
        );

        evaluatorParallel.applyGaloisInplace(encrypted, 15, galoisKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1x^3 + 2x^2 + 1x^1 + 1"
        );


    }


    @Test
    public void bfvEncryptModSwitchToDecrypt() {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{30, 30, 30, 30}));


        Context context = new Context(parms, true, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());
        ParmsIdType parmsId = context.getFirstParmsId();

        Ciphertext encrypted = new Ciphertext(context);
        Plaintext plain = new Plaintext();

        plain.fromHexPoly("0");
        encryptor.encrypt(plain, encrypted);
        // id 不变
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "0"
        );

        parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "0"
        );

        parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "0"
        );

        parmsId = context.getFirstParmsId();
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "0"
        );

        parmsId = context.getFirstParmsId();
        plain.fromHexPoly(
                "1"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "1"
        );

        parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
        plain.fromHexPoly(
                "1"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "1"
        );

        parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
        plain.fromHexPoly(
                "1"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "1"
        );



        parmsId = context.getFirstParmsId();
        plain.fromHexPoly(
                "1x^127"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "1x^127"
        );

        parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
        plain.fromHexPoly(
                "1x^127"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "1x^127"
        );

        parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
        plain.fromHexPoly(
                "1x^127"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "1x^127"
        );


        parmsId = context.getFirstParmsId();
        plain.fromHexPoly(
                "5x^64 + Ax^5"
        );
        encryptor.encrypt(plain, encrypted);

        parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "5x^64 + Ax^5"
        );

        parmsId = context.getFirstParmsId();
        plain.fromHexPoly(
                "5x^64 + Ax^5"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "5x^64 + Ax^5"
        );

        parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
        plain.fromHexPoly(
                "5x^64 + Ax^5"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "5x^64 + Ax^5"
        );

        parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
        plain.fromHexPoly(
                "5x^64 + Ax^5"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "5x^64 + Ax^5"
        );


        parmsId = context.getFirstParmsId();
        plain.fromHexPoly(
                "5x^64 + Ax^5"
        );
        encryptor.encrypt(plain, encrypted);

        parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
        evaluatorParallel.modSwitchToInplace(encrypted, parmsId);
        decryptor.decrypt(encrypted, plain);
        Assert.assertTrue(encrypted.getParmsId().equals(parmsId));
        Assert.assertEquals(
                plain.toString(),
                "5x^64 + Ax^5"
        );




    }


    @Test
    public void bfvEncryptExponentiateDecrypt() {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40, 40}));


        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);
        RelinKeys relinKeys = new RelinKeys();
        keyGenerator.createRelinKeys(relinKeys);

        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

        Ciphertext encrypted = new Ciphertext();
        Plaintext plain = new Plaintext();

        plain.fromHexPoly(
                "1x^2 + 1"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.exponentiateInplace(encrypted, 1, relinKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1x^2 + 1"
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


        plain.fromHexPoly(
                "1x^2 + 1x^1 + 1"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.exponentiateInplace(encrypted, 2, relinKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1x^4 + 2x^3 + 3x^2 + 2x^1 + 1"
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

        plain.fromHexPoly(
                "3Fx^2 + 3Fx^1 + 3F"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.exponentiateInplace(encrypted, 3, relinKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "3Fx^6 + 3Dx^5 + 3Ax^4 + 39x^3 + 3Ax^2 + 3Dx^1 + 3F"
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


        plain.fromHexPoly(
                "1x^8"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.exponentiateInplace(encrypted, 4, relinKeys);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                plain.toString(),
                "1x^32"
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


    }


    @Test
    public void bfvEncryptMultiplyManyDecrypt() {


        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40, 40}));


        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);
        RelinKeys relinKeys = new RelinKeys();
        keyGenerator.createRelinKeys(relinKeys);

        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());


        Ciphertext encrypted1 = new Ciphertext();
        Ciphertext encrypted2 = new Ciphertext();
        Ciphertext encrypted3 = new Ciphertext();
        Ciphertext encrypted4 = new Ciphertext();
        Ciphertext product = new Ciphertext();

        Plaintext plain = new Plaintext();
        Plaintext plain1 = new Plaintext();
        Plaintext plain2 = new Plaintext();
        Plaintext plain3 = new Plaintext();
        Plaintext plain4 = new Plaintext();

        plain1.fromHexPoly(
                "1x^2 + 1"
        );
        plain2.fromHexPoly(
                "1x^2 + 1x^1"
        );
        plain3.fromHexPoly(
                "1x^2 + 1x^1 + 1"
        );
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        Ciphertext[] encrypteds = new Ciphertext[] {encrypted1, encrypted2, encrypted3};
        evaluatorParallel.multiplyMany(encrypteds, relinKeys, product);
        Assert.assertEquals(3, encrypteds.length);
        decryptor.decrypt(product, plain);
        Assert.assertEquals(
                plain.toString(),
                "1x^6 + 2x^5 + 3x^4 + 3x^3 + 2x^2 + 1x^1"
        );

        Assert.assertTrue(encrypted1.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(encrypted2.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(encrypted3.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(product.getParmsId().equals(context.getFirstParmsId()));

        plain1.fromHexPoly(
                "3Fx^3 + 3F" // -1x^3 + (-1)
        );
        plain2.fromHexPoly(
                "3Fx^4 + 3F" // (-1)x^4 + (-1)
        );
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encrypteds = new Ciphertext[] {encrypted1, encrypted2};
        evaluatorParallel.multiplyMany(encrypteds, relinKeys, product);
        Assert.assertEquals(2, encrypteds.length);
        decryptor.decrypt(product, plain);
        Assert.assertEquals(
                plain.toString(),
                "1x^7 + 1x^4 + 1x^3 + 1"
        );

        Assert.assertTrue(encrypted1.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(encrypted2.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(product.getParmsId().equals(context.getFirstParmsId()));


        plain1.fromHexPoly(
                "1x^1"
        );
        plain2.fromHexPoly(
                "3Fx^4 + 3Fx^3 + 3Fx^2 + 3Fx^1 + 3F"
        );
        plain3.fromHexPoly(
                "1x^2 + 1x^1 + 1"
        );
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        encrypteds = new Ciphertext[] {encrypted1, encrypted2, encrypted3};
        evaluatorParallel.multiplyMany(encrypteds, relinKeys, product);
        Assert.assertEquals(3, encrypteds.length);
        decryptor.decrypt(product, plain);
        Assert.assertEquals(
                plain.toString(),
                "3Fx^7 + 3Ex^6 + 3Dx^5 + 3Dx^4 + 3Dx^3 + 3Ex^2 + 3Fx^1"
        );

        Assert.assertTrue(encrypted1.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(encrypted2.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(encrypted3.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(product.getParmsId().equals(context.getFirstParmsId()));


        plain1.fromHexPoly(
                "1"
        );
        plain2.fromHexPoly(
                "3F"
        );
        plain3.fromHexPoly(
                "1"
        );
        plain4.fromHexPoly(
                "3F"
        );
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        encryptor.encrypt(plain4, encrypted4);
        encrypteds = new Ciphertext[] {encrypted1, encrypted2, encrypted3, encrypted4};
        evaluatorParallel.multiplyMany(encrypteds, relinKeys, product);
        Assert.assertEquals(4, encrypteds.length);
        decryptor.decrypt(product, plain);
        Assert.assertEquals(
                plain.toString(), // 1 * -1 * 1 * -1 = 1
                "1"
        );

        Assert.assertTrue(encrypted1.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(encrypted2.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(encrypted3.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(encrypted4.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(product.getParmsId().equals(context.getFirstParmsId()));


        plain1.fromHexPoly(
                "1x^16 + 1x^15 + 1x^8 + 1x^7 + 1x^6 + 1x^3 + 1x^2 + 1"
        );
        plain2.fromHexPoly(
                "0"
        );
        plain3.fromHexPoly(
                "1x^13 + 1x^12 + 1x^5 + 1x^4 + 1x^3 + 1"
        );
        plain4.fromHexPoly(
                "1x^15 + 1x^10 + 1x^9 + 1x^8 + 1x^2 + 1x^1 + 1"
        );
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        encryptor.encrypt(plain4, encrypted4);
        encrypteds = new Ciphertext[] {encrypted1, encrypted2, encrypted3, encrypted4};
        evaluatorParallel.multiplyMany(encrypteds, relinKeys, product);
        Assert.assertEquals(4, encrypteds.length);
        decryptor.decrypt(product, plain);
        Assert.assertEquals(
                plain.toString(), // 0 * any = 0
                "0"
        );

        Assert.assertTrue(encrypted1.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(encrypted2.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(encrypted3.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(encrypted4.getParmsId().equals(product.getParmsId()));
        Assert.assertTrue(product.getParmsId().equals(context.getFirstParmsId()));



    }


    @Test
    public void bfvEncryptModSwitchToNextDecrypt() {
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = new Modulus(1 << 6);
            parms.setPolyModulusDegree(128);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{30, 30, 30, 30}));


            Context context = new Context(parms, true, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());
            ParmsIdType parmsId = context.getFirstParmsId();

            Ciphertext encrypted = new Ciphertext(context);
            Ciphertext encryptedRes = new Ciphertext();
            Plaintext plain = new Plaintext();

            plain.fromHexPoly("0");
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.modSwitchToNext(encrypted, encryptedRes);
            decryptor.decrypt(encryptedRes, plain);
            parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
            Assert.assertTrue(encryptedRes.getParmsId().equals(parmsId));
            Assert.assertEquals(
                    plain.toString(),
                    "0"
            );

            evaluatorParallel.modSwitchToNextInplace(encryptedRes);
            decryptor.decrypt(encryptedRes, plain);
            parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
            Assert.assertTrue(encryptedRes.getParmsId().equals(parmsId));
            Assert.assertEquals(
                    plain.toString(),
                    "0"
            );

            parmsId =  context.getFirstParmsId();
            plain.fromHexPoly("1");
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.modSwitchToNext(encrypted, encryptedRes);
            decryptor.decrypt(encryptedRes, plain);
            parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
            Assert.assertTrue(encryptedRes.getParmsId().equals(parmsId));
            Assert.assertEquals(
                    plain.toString(),
                    "1"
            );

            evaluatorParallel.modSwitchToNextInplace(encryptedRes);
            decryptor.decrypt(encryptedRes, plain);
            parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
            Assert.assertTrue(encryptedRes.getParmsId().equals(parmsId));
            Assert.assertEquals(
                    plain.toString(),
                    "1"
            );

            parmsId =  context.getFirstParmsId();
            plain.fromHexPoly("1x^127");
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.modSwitchToNext(encrypted, encryptedRes);
            decryptor.decrypt(encryptedRes, plain);
            parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
            Assert.assertTrue(encryptedRes.getParmsId().equals(parmsId));
            Assert.assertEquals(
                    plain.toString(),
                    "1x^127"
            );

            evaluatorParallel.modSwitchToNextInplace(encryptedRes);
            decryptor.decrypt(encryptedRes, plain);
            parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
            Assert.assertTrue(encryptedRes.getParmsId().equals(parmsId));
            Assert.assertEquals(
                    plain.toString(),
                    "1x^127"
            );

            parmsId =  context.getFirstParmsId();
            plain.fromHexPoly("5x^64 + Ax^5");
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.modSwitchToNext(encrypted, encryptedRes);
            decryptor.decrypt(encryptedRes, plain);
            parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
            Assert.assertTrue(encryptedRes.getParmsId().equals(parmsId));
            Assert.assertEquals(
                    plain.toString(),
                    "5x^64 + Ax^5"
            );

            evaluatorParallel.modSwitchToNextInplace(encryptedRes);
            decryptor.decrypt(encryptedRes, plain);
            parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
            Assert.assertTrue(encryptedRes.getParmsId().equals(parmsId));
            Assert.assertEquals(
                    plain.toString(),
                    "5x^64 + Ax^5"
            );
        }

        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = new Modulus(786433);
            parms.setPolyModulusDegree(8192);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.bfvDefault(8192));

            Context context = new Context(parms, true, CoeffModulus.SecurityLevelType.TC128);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());
            ParmsIdType parmsId = context.getFirstParmsId();

            Ciphertext encrypted = new Ciphertext(context);
            Ciphertext encryptedRes = new Ciphertext();
            Plaintext plain = new Plaintext();

            plain.fromHexPoly("1");
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.modSwitchToNext(encrypted, encryptedRes);
            decryptor.decrypt(encryptedRes, plain);
            parmsId = context.getContextData(parmsId).getNextContextData().getParmsId();
            Assert.assertTrue(encryptedRes.getParmsId().equals(parmsId));
            Assert.assertEquals(
                    plain.toString(),
                    "1"
            );
        }




    }

    @Test
    public void bfvReLinearize() {
        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40, 40, 40}));


        Context context = new Context(parms, true, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);
        RelinKeys relinKeys = new RelinKeys();
        keyGenerator.createRelinKeys(relinKeys);

        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

        Ciphertext encrypted = new Ciphertext();
        Plaintext plain = new Plaintext();
        Plaintext plain2 = new Plaintext();

        plain.fromHexPoly("0");
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.squareInplace(encrypted);
        evaluatorParallel.reLinearizeInplace(encrypted, relinKeys);
        decryptor.decrypt(encrypted, plain2);
        Assert.assertEquals(plain, plain2);

        plain.fromHexPoly("1x^10 + 2");
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.squareInplace(encrypted);
        evaluatorParallel.reLinearizeInplace(encrypted, relinKeys);
        decryptor.decrypt(encrypted, plain2);
        Assert.assertEquals(
                plain2.toString(),
                "1x^20 + 4x^10 + 4");

        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.squareInplace(encrypted);
        evaluatorParallel.reLinearizeInplace(encrypted, relinKeys);
        // 连续两次
        evaluatorParallel.squareInplace(encrypted);
        evaluatorParallel.reLinearizeInplace(encrypted, relinKeys);

        decryptor.decrypt(encrypted, plain2);
        Assert.assertEquals(
                plain2.toString(),
                "1x^40 + 8x^30 + 18x^20 + 20x^10 + 10");

        // Relinearization with modulus switching
        plain.fromHexPoly("1x^10 + 2");
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.squareInplace(encrypted);
        evaluatorParallel.reLinearizeInplace(encrypted, relinKeys);
        // mod switch
        evaluatorParallel.modSwitchToNextInplace(encrypted);
        decryptor.decrypt(encrypted, plain2);
        Assert.assertEquals(
                plain2.toString(),
                "1x^20 + 4x^10 + 4");

        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.squareInplace(encrypted);
        evaluatorParallel.reLinearizeInplace(encrypted, relinKeys);
        evaluatorParallel.modSwitchToNextInplace(encrypted);
        // 连续两次
        evaluatorParallel.squareInplace(encrypted);
        evaluatorParallel.reLinearizeInplace(encrypted, relinKeys);
        evaluatorParallel.modSwitchToNextInplace(encrypted);

        decryptor.decrypt(encrypted, plain2);
        Assert.assertEquals(
                plain2.toString(),
                "1x^40 + 8x^30 + 18x^20 + 20x^10 + 10");

    }


    @Test
    public void bfvEncryptSquareDecrypt() {
        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 8);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40, 40}));

        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

        Ciphertext encrypted = new Ciphertext();
        Plaintext plain = new Plaintext();

        plain.fromHexPoly(
                "1"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                "1",
                plain.toString()
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

        plain.fromHexPoly(
                "0"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                "0",
                plain.toString()
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


        plain.fromHexPoly(
                "FFx^2 + FF"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                "1x^4 + 2x^2 + 1",
                plain.toString()
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

        plain.fromHexPoly(
                "FF"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                "1",
                plain.toString()
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


        plain.fromHexPoly(
                "1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^1 + 1"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                "1x^12 + 2x^11 + 3x^10 + 4x^9 + 3x^8 + 4x^7 + 5x^6 + 4x^5 + 4x^4 + 2x^3 + 1x^2 + 2x^1 + 1",
                plain.toString()
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


        plain.fromHexPoly(
                "1x^16"
        );
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                "1x^32",
                plain.toString()
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

        plain.fromHexPoly(
                "1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^1 + 1"
        );
        encryptor.encrypt(plain, encrypted);
        // 连续2次
        evaluatorParallel.squareInplace(encrypted);
        evaluatorParallel.squareInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                "1x^24 + 4x^23 + Ax^22 + 14x^21 + 1Fx^20 + 2Cx^19 + 3Cx^18 + 4Cx^17 + 5Fx^16 + 6Cx^15 + 70x^14 + 74x^13 + "
                        + "71x^12 + 6Cx^11 + 64x^10 + 50x^9 + 40x^8 + 34x^7 + 26x^6 + 1Cx^5 + 11x^4 + 8x^3 + 6x^2 + 4x^1 + 1",
                plain.toString()
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

    }




    @Test
    public void bfvEncryptMultiplyDecryptRandom() {
        for (int i = 0; i < MAX_LOOP; i++) {
            bfvEncryptMultiplyDecrypt();
        }
    }



    @Test
    public void bfvEncryptMultiplyDecrypt() {

        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = new Modulus(1 << 6);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly(
                    "0"
            );
            plain2.fromHexPoly(
                    "0"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2.fromHexPoly(
                    "1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^46 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 2x^39 + 1x^38 + 2x^37 + 3x^36 + 1x^35 + "
                            + "3x^34 + 2x^33 + 2x^32 + 4x^30 + 2x^29 + 5x^28 + 2x^27 + 4x^26 + 3x^25 + 2x^24 + "
                            + "4x^23 + 3x^22 + 4x^21 + 4x^20 + 4x^19 + 4x^18 + 3x^17 + 2x^15 + 4x^14 + 2x^13 + "
                            + "3x^12 + 2x^11 + 2x^10 + 2x^9 + 1x^8 + 1x^6 + 1x^5 + 1x^4 + 1x^3",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly(
                    "0"
            );
            plain2.fromHexPoly(
                    "1x^2 + 1"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly(
                    "1x^2 + 1x^1 + 1"
            );
            plain2.fromHexPoly(
                    "1"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^2 + 1x^1 + 1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "1x^2 + 1"
            );
            plain2.fromHexPoly(
                    "3Fx^1 + 3F"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "3Fx^3 + 3Fx^2 + 3Fx^1 + 3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly(
                    "1x^16"
            );
            plain2.fromHexPoly(
                    "1x^8"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^24",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));
        }
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = new Modulus((1L << 60) - 1L);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60, 60, 60}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly(
                    "0"
            );
            plain2.fromHexPoly(
                    "0"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2.fromHexPoly(
                    "1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^46 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 2x^39 + 1x^38 + 2x^37 + 3x^36 + 1x^35 + "
                            + "3x^34 + 2x^33 + 2x^32 + 4x^30 + 2x^29 + 5x^28 + 2x^27 + 4x^26 + 3x^25 + 2x^24 + "
                            + "4x^23 + 3x^22 + 4x^21 + 4x^20 + 4x^19 + 4x^18 + 3x^17 + 2x^15 + 4x^14 + 2x^13 + "
                            + "3x^12 + 2x^11 + 2x^10 + 2x^9 + 1x^8 + 1x^6 + 1x^5 + 1x^4 + 1x^3",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly(
                    "0"
            );
            plain2.fromHexPoly(
                    "1x^2 + 1"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly(
                    "1x^2 + 1x^1 + 1"
            );
            plain2.fromHexPoly(
                    "1"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^2 + 1x^1 + 1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "1x^2 + 1"
            );
            // -1x^1 - 1
            plain2.fromHexPoly(
                    "FFFFFFFFFFFFFFEx^1 + FFFFFFFFFFFFFFE"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "FFFFFFFFFFFFFFEx^3 + FFFFFFFFFFFFFFEx^2 + FFFFFFFFFFFFFFEx^1 + FFFFFFFFFFFFFFE",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly(
                    "1x^16"
            );
            plain2.fromHexPoly(
                    "1x^8"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^24",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));
        }
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = new Modulus(1L << 6);
            parms.setPolyModulusDegree(128);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly(
                    "0"
            );
            plain2.fromHexPoly(
                    "0"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2.fromHexPoly(
                    "1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^46 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 2x^39 + 1x^38 + 2x^37 + 3x^36 + 1x^35 + "
                            + "3x^34 + 2x^33 + 2x^32 + 4x^30 + 2x^29 + 5x^28 + 2x^27 + 4x^26 + 3x^25 + 2x^24 + "
                            + "4x^23 + 3x^22 + 4x^21 + 4x^20 + 4x^19 + 4x^18 + 3x^17 + 2x^15 + 4x^14 + 2x^13 + "
                            + "3x^12 + 2x^11 + 2x^10 + 2x^9 + 1x^8 + 1x^6 + 1x^5 + 1x^4 + 1x^3",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly(
                    "0"
            );
            plain2.fromHexPoly(
                    "1x^2 + 1"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly(
                    "1x^2 + 1x^1 + 1"
            );
            plain2.fromHexPoly(
                    "1"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^2 + 1x^1 + 1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "1x^2 + 1"
            );
            // -1x^1 - 1
            plain2.fromHexPoly(
                    "3Fx^1 + 3F"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "3Fx^3 + 3Fx^2 + 3Fx^1 + 3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly(
                    "1x^16"
            );
            plain2.fromHexPoly(
                    "1x^8"
            );
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.multiplyInplace(encrypted1, encrypted2);


            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^24",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));
        }
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = new Modulus(1L << 8);
            parms.setPolyModulusDegree(128);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40, 40}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly(
                    "1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^1 + 1"
            );
            encryptor.encrypt(plain1, encrypted1);

            // 注意这里的参数, 连续两次乘法
            evaluatorParallel.multiply(encrypted1, encrypted1, encrypted1);
            evaluatorParallel.multiply(encrypted1, encrypted1, encrypted1);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^24 + 4x^23 + Ax^22 + 14x^21 + 1Fx^20 + 2Cx^19 + 3Cx^18 + 4Cx^17 + 5Fx^16 + "
                            + "6Cx^15 + 70x^14 + 74x^13 + 71x^12 + 6Cx^11 + 64x^10 + 50x^9 + 40x^8 + 34x^7 + "
                            + "26x^6 + 1Cx^5 + 11x^4 + 8x^3 + 6x^2 + 4x^1 + 1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));
        }
    }
    // 测试 密文在Ntt 和 非Ntt 之间的转换
    private void evaluatorParallelTransformEncryptedToFromNtt(SchemeType scheme) {

        EncryptionParams parms = new EncryptionParams(scheme);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40}));

        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Encryptor encryptor = new Encryptor(context, pk);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

        Ciphertext encrypted = new Ciphertext();
        Plaintext plain = new Plaintext();

        plain.fromHexPoly("0");
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.transformToNttInplace(encrypted);
        evaluatorParallel.transformFromNttInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                "0",
                plain.toString()
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

        plain.fromHexPoly("1");
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.transformToNttInplace(encrypted);
        evaluatorParallel.transformFromNttInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                "1",
                plain.toString()
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


        plain.fromHexPoly("Fx^10 + Ex^9 + Dx^8 + Cx^7 + Bx^6 + Ax^5 + 1x^4 + 2x^3 + 3x^2 + 4x^1 + 5");
        encryptor.encrypt(plain, encrypted);
        evaluatorParallel.transformToNttInplace(encrypted);
        evaluatorParallel.transformFromNttInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(
                "Fx^10 + Ex^9 + Dx^8 + Cx^7 + Bx^6 + Ax^5 + 1x^4 + 2x^3 + 3x^2 + 4x^1 + 5",
                plain.toString()
        );
        Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));
    }


    @Test
    public void transformEncryptedToFromNTT() {
        evaluatorParallelTransformEncryptedToFromNtt(SchemeType.BFV);
    }


    private void evaluatorParallelTransformPlainToNtt(SchemeType scheme) {

        EncryptionParams parms = new EncryptionParams(scheme);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40, 40}));

        Context context = new Context(parms, true, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);

        Plaintext plain = new Plaintext("0");
        Assert.assertFalse(plain.isNttForm());
        evaluatorParallel.transformToNttInplace(plain, context.getFirstParmsId());
        Assert.assertTrue(plain.isZero());
        Assert.assertTrue(plain.isNttForm());
        Assert.assertTrue(plain.getParmsId().equals(context.getFirstParmsId()));

        plain.release();
        plain.fromHexPoly("0");
        Assert.assertFalse(plain.isNttForm());
        ParmsIdType nextParmsId = context.firstContextData().getNextContextData().getParmsId();
        evaluatorParallel.transformToNttInplace(plain, nextParmsId);
        Assert.assertTrue(plain.isZero());
        Assert.assertTrue(plain.isNttForm());
        Assert.assertTrue(plain.getParmsId().equals(nextParmsId));

        plain.release();
        plain.fromHexPoly("1");
        Assert.assertFalse(plain.isNttForm());
        evaluatorParallel.transformToNttInplace(plain, context.getFirstParmsId());
        Assert.assertEquals(256, plain.getCoeffCount());
        for (int i = 0; i < 256; i++) {
            Assert.assertEquals(plain.at(i), 1);
        }
        Assert.assertTrue(plain.isNttForm());
        Assert.assertTrue(plain.getParmsId().equals(context.getFirstParmsId()));

        plain.release();
        plain.fromHexPoly("1");
        Assert.assertFalse(plain.isNttForm());
        evaluatorParallel.transformToNttInplace(plain, nextParmsId);
        Assert.assertEquals(128, plain.getCoeffCount());
        for (int i = 0; i < 128; i++) {
            Assert.assertEquals(plain.at(i), 1);
        }
        Assert.assertTrue(plain.isNttForm());
        Assert.assertTrue(plain.getParmsId().equals(nextParmsId));

        plain.release();
        plain.fromHexPoly("2");
        Assert.assertFalse(plain.isNttForm());
        evaluatorParallel.transformToNttInplace(plain, context.getFirstParmsId());
        Assert.assertEquals(256, plain.getCoeffCount());
        for (int i = 0; i < 256; i++) {
            Assert.assertEquals(plain.at(i), 2);
        }
        Assert.assertTrue(plain.isNttForm());
        Assert.assertTrue(plain.getParmsId().equals(context.getFirstParmsId()));

        plain.release();
        plain.fromHexPoly("2");
        Assert.assertFalse(plain.isNttForm());
        evaluatorParallel.transformToNttInplace(plain, nextParmsId);
        Assert.assertEquals(128, plain.getCoeffCount());
        for (int i = 0; i < 128; i++) {
            Assert.assertEquals(plain.at(i), 2);
        }
        Assert.assertTrue(plain.isNttForm());
        Assert.assertTrue(plain.getParmsId().equals(nextParmsId));

    }


    @Test
    public void transformPlainToNTT() {
        evaluatorParallelTransformPlainToNtt(SchemeType.BFV);
    }

    // test non-in-place
    @Test
    public void bfvEncryptMultiplyPlainNTTDecrypt() {
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = new Modulus(1 << 6);
            parms.setPolyModulusDegree(128);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

            Ciphertext encrypted = new Ciphertext();
            Ciphertext encryptedDestination = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plainMultiplier = new Plaintext();

            plain.fromHexPoly("0");
            encryptor.encrypt(plain, encrypted);
            // 密文转换为 Ntt
            evaluatorParallel.transformToNttInplace(encrypted);
            plainMultiplier.fromHexPoly("1");
            // 明文转换为 Ntt
            evaluatorParallel.transformToNttInplace(plainMultiplier, context.getFirstParmsId());
            // 密文 * 明文
            evaluatorParallel.multiplyPlain(encrypted, plainMultiplier, encryptedDestination);
            // 密文转换回非 Ntt
            evaluatorParallel.transformFromNttInplace(encryptedDestination);
            // 解密
            decryptor.decrypt(encryptedDestination, plain);
            // 验证
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            plain.fromHexPoly("2");
            encryptor.encrypt(plain, encrypted);
            // 密文转换为 Ntt
            evaluatorParallel.transformToNttInplace(encrypted);
            plainMultiplier.release();
            plainMultiplier.fromHexPoly("3");
            // 明文转换为 Ntt
            evaluatorParallel.transformToNttInplace(plainMultiplier, context.getFirstParmsId());
            // 密文 * 明文
            evaluatorParallel.multiplyPlain(encrypted, plainMultiplier, encryptedDestination);
            // 密文转换回非 Ntt
            evaluatorParallel.transformFromNttInplace(encryptedDestination);
            // 解密
            decryptor.decrypt(encryptedDestination, plain);
            // 验证
            Assert.assertEquals(
                    "6",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain.fromHexPoly("1");
            encryptor.encrypt(plain, encrypted);
            // 密文转换为 Ntt
            evaluatorParallel.transformToNttInplace(encrypted);
            plainMultiplier.release();
            plainMultiplier.fromHexPoly("Fx^10 + Ex^9 + Dx^8 + Cx^7 + Bx^6 + Ax^5 + 1x^4 + 2x^3 + 3x^2 + 4x^1 + 5");
            // 明文转换为 Ntt
            evaluatorParallel.transformToNttInplace(plainMultiplier, context.getFirstParmsId());
            // 密文 * 明文
            evaluatorParallel.multiplyPlain(encrypted, plainMultiplier, encryptedDestination);
            // 密文转换回非 Ntt
            evaluatorParallel.transformFromNttInplace(encryptedDestination);
            // 解密
            decryptor.decrypt(encryptedDestination, plain);
            // 验证
            Assert.assertEquals(
                    "Fx^10 + Ex^9 + Dx^8 + Cx^7 + Bx^6 + Ax^5 + 1x^4 + 2x^3 + 3x^2 + 4x^1 + 5",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain.fromHexPoly("1x^20");
            encryptor.encrypt(plain, encrypted);
            // 密文转换为 Ntt
            evaluatorParallel.transformToNttInplace(encrypted);
            plainMultiplier.release();
            plainMultiplier.fromHexPoly("Fx^10 + Ex^9 + Dx^8 + Cx^7 + Bx^6 + Ax^5 + 1x^4 + 2x^3 + 3x^2 + 4x^1 + 5");
            // 明文转换为 Ntt
            evaluatorParallel.transformToNttInplace(plainMultiplier, context.getFirstParmsId());
            // 密文 * 明文
            evaluatorParallel.multiplyPlain(encrypted, plainMultiplier, encryptedDestination);
            // 密文转换回非 Ntt
            evaluatorParallel.transformFromNttInplace(encryptedDestination);
            // 解密
            decryptor.decrypt(encryptedDestination, plain);
            // 验证
            Assert.assertEquals(
                    "Fx^30 + Ex^29 + Dx^28 + Cx^27 + Bx^26 + Ax^25 + 1x^24 + 2x^23 + 3x^22 + 4x^21 + 5x^20",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


        }


    }

    // 都是 in-place 接口
    @Test
    public void bfvEncryptMultiplyPlainNTTInplaceDecrypt() {
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = new Modulus(1 << 6);
            parms.setPolyModulusDegree(128);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

            Ciphertext encrypted = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plainMultiplier = new Plaintext();

            plain.fromHexPoly("0");
            encryptor.encrypt(plain, encrypted);
            // 密文转换为 Ntt
            evaluatorParallel.transformToNttInplace(encrypted);
            plainMultiplier.fromHexPoly("1");
            // 明文转换为 Ntt
            evaluatorParallel.transformToNttInplace(plainMultiplier, context.getFirstParmsId());
            // 密文 * 明文
            evaluatorParallel.multiplyPlainInplace(encrypted, plainMultiplier);
            // 密文转换回非 Ntt
            evaluatorParallel.transformFromNttInplace(encrypted);
            // 解密
            decryptor.decrypt(encrypted, plain);
            // 验证
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            plain.fromHexPoly("2");
            encryptor.encrypt(plain, encrypted);
            // 密文转换为 Ntt
            evaluatorParallel.transformToNttInplace(encrypted);
            plainMultiplier.release();
            plainMultiplier.fromHexPoly("3");
            // 明文转换为 Ntt
            evaluatorParallel.transformToNttInplace(plainMultiplier, context.getFirstParmsId());
            // 密文 * 明文
            evaluatorParallel.multiplyPlainInplace(encrypted, plainMultiplier);
            // 密文转换回非 Ntt
            evaluatorParallel.transformFromNttInplace(encrypted);
            // 解密
            decryptor.decrypt(encrypted, plain);
            // 验证
            Assert.assertEquals(
                    "6",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain.fromHexPoly("1");
            encryptor.encrypt(plain, encrypted);
            // 密文转换为 Ntt
            evaluatorParallel.transformToNttInplace(encrypted);
            plainMultiplier.release();
            plainMultiplier.fromHexPoly("Fx^10 + Ex^9 + Dx^8 + Cx^7 + Bx^6 + Ax^5 + 1x^4 + 2x^3 + 3x^2 + 4x^1 + 5");
            // 明文转换为 Ntt
            evaluatorParallel.transformToNttInplace(plainMultiplier, context.getFirstParmsId());
            // 密文 * 明文
            evaluatorParallel.multiplyPlainInplace(encrypted, plainMultiplier);
            // 密文转换回非 Ntt
            evaluatorParallel.transformFromNttInplace(encrypted);
            // 解密
            decryptor.decrypt(encrypted, plain);
            // 验证
            Assert.assertEquals(
                    "Fx^10 + Ex^9 + Dx^8 + Cx^7 + Bx^6 + Ax^5 + 1x^4 + 2x^3 + 3x^2 + 4x^1 + 5",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain.fromHexPoly("1x^20");
            encryptor.encrypt(plain, encrypted);
            // 密文转换为 Ntt
            evaluatorParallel.transformToNttInplace(encrypted);
            plainMultiplier.release();
            plainMultiplier.fromHexPoly("Fx^10 + Ex^9 + Dx^8 + Cx^7 + Bx^6 + Ax^5 + 1x^4 + 2x^3 + 3x^2 + 4x^1 + 5");
            // 明文转换为 Ntt
            evaluatorParallel.transformToNttInplace(plainMultiplier, context.getFirstParmsId());
            // 密文 * 明文
            evaluatorParallel.multiplyPlainInplace(encrypted, plainMultiplier);
            // 密文转换回非 Ntt
            evaluatorParallel.transformFromNttInplace(encrypted);
            // 解密
            decryptor.decrypt(encrypted, plain);
            // 验证
            Assert.assertEquals(
                    "Fx^30 + Ex^29 + Dx^28 + Cx^27 + Bx^26 + Ax^25 + 1x^24 + 2x^23 + 3x^22 + 4x^21 + 5x^20",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


        }
    }

    // non-in-place test
    @Test
    public void bfvEncryptMultiplyPlainDecrypt() {
        // 参数1
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = new Modulus(1 << 6);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

            Ciphertext encrypted = new Ciphertext();
            Ciphertext destination = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2.fromHexPoly(
                    "1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1"
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^46 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 2x^39 + 1x^38 + 2x^37 + 3x^36 + 1x^35 + "
                            + "3x^34 + 2x^33 + 2x^32 + 4x^30 + 2x^29 + 5x^28 + 2x^27 + 4x^26 + 3x^25 + 2x^24 + "
                            + "4x^23 + 3x^22 + 4x^21 + 4x^20 + 4x^19 + 4x^18 + 3x^17 + 2x^15 + 4x^14 + 2x^13 + "
                            + "3x^12 + 2x^11 + 2x^10 + 2x^9 + 1x^8 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
                    ,
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "0"
            );
            plain2.fromHexPoly(
                    "1x^2 + 1"
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "1x^2 + 1x^1 + 1"
            );
            plain2.fromHexPoly(
                    "1x^2"
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^4 + 1x^3 + 1x^2",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly(
                    "1x^2 + 1x^1 + 1"
            );
            plain2.fromHexPoly(
                    "1x^1"
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^3 + 1x^2 + 1x^1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "1x^2 + 1"
            );
            plain2.fromHexPoly(
                    "3Fx^1 + 3F"
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3Fx^3 + 3Fx^2 + 3Fx^1 + 3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "3Fx^2 + 3Fx^1 + 3F"
            );
            plain2.fromHexPoly(
                    "1x^1"
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3Fx^3 + 3Fx^2 + 3Fx^1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

        }
        // 参数2
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            // 明文模 < each qi
            Modulus plainModulus = new Modulus((1L << 20) - 1);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 60, 60}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

            Ciphertext encrypted = new Ciphertext();
            Ciphertext destination = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2.fromHexPoly(
                    "1" // 单项式，有特殊优化
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
                    ,
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain2.fromHexPoly(
                    "5" // 单项式，有特殊优化
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "5x^28 + 5x^25 + 5x^21 + 5x^20 + 5x^18 + 5x^14 + 5x^12 + 5x^10 + 5x^9 + 5x^6 + 5x^5 + 5x^4 + 5x^3"
                    ,
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));
        }

        // 参数3
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            // 明文模 并不小于 每一个 qi，和上面的测试 会进入到不同的分支
            Modulus plainModulus = new Modulus((1L << 40) - 1);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 60, 60}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

            Ciphertext encrypted = new Ciphertext();
            Ciphertext destination = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2.fromHexPoly(
                    "1" // 单项式，有特殊优化
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
                    ,
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain2.fromHexPoly(
                    "5" // 单项式，有特殊优化
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "5x^28 + 5x^25 + 5x^21 + 5x^20 + 5x^18 + 5x^14 + 5x^12 + 5x^10 + 5x^9 + 5x^6 + 5x^5 + 5x^4 + 5x^3"
                    ,
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

        }

        // 参数4
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = PlainModulus.batching(64, 20);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 30, 30}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());
            BatchEncoder batchEncoder = new BatchEncoder(context);

            Ciphertext encrypted = new Ciphertext();
            Ciphertext destination = new Ciphertext();
            Plaintext plain = new Plaintext();

            long[] data = new long[batchEncoder.slotCount()];
            Arrays.fill(data, 7);
            // 注意这里的 data 是 i64
            batchEncoder.encodeInt64(data, plain);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain, destination);
            decryptor.decrypt(destination, plain);
            long[] result = new long[batchEncoder.slotCount()];
            batchEncoder.decodeInt64(plain, result);

            long[] truth = new long[batchEncoder.slotCount()];
            // 7 * 7 = 49
            Arrays.fill(truth, 49);
            Assert.assertArrayEquals(
                    truth,
                    result
            );

            // 注意这里的 data 是 i64
            Arrays.fill(data, -7);
            batchEncoder.encodeInt64(data, plain);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain, destination);
            decryptor.decrypt(destination, plain);
            result = new long[batchEncoder.slotCount()];
            batchEncoder.decodeInt64(plain, result);

            truth = new long[batchEncoder.slotCount()];
            // -7 * -7 = 49
            Arrays.fill(truth, 49);
            Assert.assertArrayEquals(
                    truth,
                    result
            );
        }

        // 参数4
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = PlainModulus.batching(64, 40);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 30, 30, 30, 30}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());
            BatchEncoder batchEncoder = new BatchEncoder(context);

            Ciphertext encrypted = new Ciphertext();
            Ciphertext destination = new Ciphertext();
            Plaintext plain = new Plaintext();
            // First test with constant plaintext
            long[] data = new long[batchEncoder.slotCount()];
            Arrays.fill(data, 7);
            // 注意这里的 data 是 i64
            batchEncoder.encodeInt64(data, plain);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain, destination);
            decryptor.decrypt(destination, plain);
            long[] result = new long[batchEncoder.slotCount()];
            batchEncoder.decodeInt64(plain, result);

            long[] truth = new long[batchEncoder.slotCount()];
            // 7 * 7 = 49
            Arrays.fill(truth, 49);
            Assert.assertArrayEquals(
                    truth,
                    result
            );

            // 注意这里的 data 是 i64
            Arrays.fill(data, -7);
            batchEncoder.encodeInt64(data, plain);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain, destination);
            decryptor.decrypt(destination, plain);
            result = new long[batchEncoder.slotCount()];
            batchEncoder.decodeInt64(plain, result);

            truth = new long[batchEncoder.slotCount()];
            // -7 * -7 = 49
            Arrays.fill(truth, 49);
            Assert.assertArrayEquals(
                    truth,
                    result
            );

            // Now test a non-constant plaintext
            long[] input = new long[batchEncoder.slotCount()];
            Arrays.fill(input, 7);
            input[input.length - 1] = 1;
            long[] truthResult = new long[batchEncoder.slotCount()];
            Arrays.fill(truthResult, 49);
            truthResult[truthResult.length - 1] = 1;

            batchEncoder.encodeInt64(input, plain);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain, destination);
            decryptor.decrypt(destination, plain);
            batchEncoder.decode(plain, result);
            Assert.assertArrayEquals(
                    truthResult,
                    result
            );

            // Now test a non-constant plaintext
            input = new long[batchEncoder.slotCount()];
            Arrays.fill(input, -7);
            input[input.length - 1] = 1;
            truthResult = new long[batchEncoder.slotCount()];
            Arrays.fill(truthResult, 49);
            truthResult[truthResult.length - 1] = 1;

            batchEncoder.encodeInt64(input, plain);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.multiplyPlain(encrypted, plain, destination);
            decryptor.decrypt(destination, plain);
            batchEncoder.decode(plain, result);
            Assert.assertArrayEquals(
                    truthResult,
                    result
            );
        }
    }


    // in-place test
    @Test
    public void bfvEncryptMultiplyPlainInplaceDecrypt() {
        // 参数1
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = new Modulus(1 << 6);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

            Ciphertext encrypted = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2.fromHexPoly(
                    "1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1"
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain2);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "1x^46 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 2x^39 + 1x^38 + 2x^37 + 3x^36 + 1x^35 + "
                            + "3x^34 + 2x^33 + 2x^32 + 4x^30 + 2x^29 + 5x^28 + 2x^27 + 4x^26 + 3x^25 + 2x^24 + "
                            + "4x^23 + 3x^22 + 4x^21 + 4x^20 + 4x^19 + 4x^18 + 3x^17 + 2x^15 + 4x^14 + 2x^13 + "
                            + "3x^12 + 2x^11 + 2x^10 + 2x^9 + 1x^8 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
                    ,
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "0"
            );
            plain2.fromHexPoly(
                    "1x^2 + 1"
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain2);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "1x^2 + 1x^1 + 1"
            );
            plain2.fromHexPoly(
                    "1x^2"
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain2);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "1x^4 + 1x^3 + 1x^2",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly(
                    "1x^2 + 1x^1 + 1"
            );
            plain2.fromHexPoly(
                    "1x^1"
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain2);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "1x^3 + 1x^2 + 1x^1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "1x^2 + 1"
            );
            plain2.fromHexPoly(
                    "3Fx^1 + 3F"
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain2);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "3Fx^3 + 3Fx^2 + 3Fx^1 + 3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain1.fromHexPoly(
                    "3Fx^2 + 3Fx^1 + 3F"
            );
            plain2.fromHexPoly(
                    "1x^1"
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain2);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "3Fx^3 + 3Fx^2 + 3Fx^1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

        }
        // 参数2
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            // 明文模 < each qi
            Modulus plainModulus = new Modulus((1L << 20) - 1);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 60, 60}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

            Ciphertext encrypted = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2.fromHexPoly(
                    "1" // 单项式，有特殊优化
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain2);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
                    ,
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain2.fromHexPoly(
                    "5" // 单项式，有特殊优化
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain2);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "5x^28 + 5x^25 + 5x^21 + 5x^20 + 5x^18 + 5x^14 + 5x^12 + 5x^10 + 5x^9 + 5x^6 + 5x^5 + 5x^4 + 5x^3"
                    ,
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));
        }

        // 参数3
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            // 明文模 并不小于 每一个 qi，和上面的测试 会进入到不同的分支
            Modulus plainModulus = new Modulus((1L << 40) - 1);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 60, 60}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

            Ciphertext encrypted = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
            );
            plain2.fromHexPoly(
                    "1" // 单项式，有特殊优化
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain2);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3"
                    ,
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));


            plain2.fromHexPoly(
                    "5" // 单项式，有特殊优化
            );
            encryptor.encrypt(plain1, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain2);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "5x^28 + 5x^25 + 5x^21 + 5x^20 + 5x^18 + 5x^14 + 5x^12 + 5x^10 + 5x^9 + 5x^6 + 5x^5 + 5x^4 + 5x^3"
                    ,
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

        }

        // 参数4
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = PlainModulus.batching(64, 20);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 30, 30}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());
            BatchEncoder batchEncoder = new BatchEncoder(context);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();

            long[] data = new long[batchEncoder.slotCount()];
            Arrays.fill(data, 7);
            // 注意这里的 data 是 i64
            batchEncoder.encodeInt64(data, plain);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain);
            decryptor.decrypt(encrypted, plain);
            long[] result = new long[batchEncoder.slotCount()];
            batchEncoder.decodeInt64(plain, result);

            long[] truth = new long[batchEncoder.slotCount()];
            // 7 * 7 = 49
            Arrays.fill(truth, 49);
            Assert.assertArrayEquals(
                    truth,
                    result
            );

            // 注意这里的 data 是 i64
            Arrays.fill(data, -7);
            batchEncoder.encodeInt64(data, plain);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain);
            decryptor.decrypt(encrypted, plain);
            result = new long[batchEncoder.slotCount()];
            batchEncoder.decodeInt64(plain, result);

            truth = new long[batchEncoder.slotCount()];
            // -7 * -7 = 49
            Arrays.fill(truth, 49);
            Assert.assertArrayEquals(
                    truth,
                    result
            );
        }

        // 参数4
        {
            EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
            Modulus plainModulus = PlainModulus.batching(64, 40);
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(plainModulus);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 30, 30, 30, 30}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keyGenerator.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
            Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());
            BatchEncoder batchEncoder = new BatchEncoder(context);

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            // First test with constant plaintext
            long[] data = new long[batchEncoder.slotCount()];
            Arrays.fill(data, 7);
            // 注意这里的 data 是 i64
            batchEncoder.encodeInt64(data, plain);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain);
            decryptor.decrypt(encrypted, plain);
            long[] result = new long[batchEncoder.slotCount()];
            batchEncoder.decodeInt64(plain, result);

            long[] truth = new long[batchEncoder.slotCount()];
            // 7 * 7 = 49
            Arrays.fill(truth, 49);
            Assert.assertArrayEquals(
                    truth,
                    result
            );

            // 注意这里的 data 是 i64
            Arrays.fill(data, -7);
            batchEncoder.encodeInt64(data, plain);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain);
            decryptor.decrypt(encrypted, plain);
            result = new long[batchEncoder.slotCount()];
            batchEncoder.decodeInt64(plain, result);

            truth = new long[batchEncoder.slotCount()];
            // -7 * -7 = 49
            Arrays.fill(truth, 49);
            Assert.assertArrayEquals(
                    truth,
                    result
            );

            // Now test a non-constant plaintext
            long[] input = new long[batchEncoder.slotCount()];
            Arrays.fill(input, 7);
            input[input.length - 1] = 1;
            long[] truthResult = new long[batchEncoder.slotCount()];
            Arrays.fill(truthResult, 49);
            truthResult[truthResult.length - 1] = 1;

            batchEncoder.encodeInt64(input, plain);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain);
            decryptor.decrypt(encrypted, plain);
            batchEncoder.decode(plain, result);
            Assert.assertArrayEquals(
                    truthResult,
                    result
            );

            // Now test a non-constant plaintext
            input = new long[batchEncoder.slotCount()];
            Arrays.fill(input, -7);
            input[input.length - 1] = 1;
            truthResult = new long[batchEncoder.slotCount()];
            Arrays.fill(truthResult, 49);
            truthResult[truthResult.length - 1] = 1;

            batchEncoder.encodeInt64(input, plain);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.multiplyPlainInplace(encrypted, plain);
            decryptor.decrypt(encrypted, plain);
            batchEncoder.decode(plain, result);
            Assert.assertArrayEquals(
                    truthResult,
                    result
            );
        }
    }


    @Test
    public void bfvEncryptSubPlainDecrypt() {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(64);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());
        // in-place
        {
            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly("1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3");
            plain2.fromHexPoly("1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1");
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.subPlainInplace(encrypted1, plain2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 3Fx^16 + 1x^12 + 1x^10 + 3Fx^8 + 1x^6 + 1x^4 + 1x^3 + 3F",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("0");
            plain2.fromHexPoly("0");
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.subPlainInplace(encrypted1, plain2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("0");
            plain2.fromHexPoly("1x^2 + 1");
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.subPlainInplace(encrypted1, plain2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "3Fx^2 + 3F",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("1x^2 + 1");
            plain2.fromHexPoly("3Fx^1 + 3F");
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.subPlainInplace(encrypted1, plain2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^2 + 1x^1 + 2",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("3Fx^2 + 3Fx^1 + 3F");
            plain2.fromHexPoly("1x^1");
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.subPlainInplace(encrypted1, plain2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "3Fx^2 + 3Ex^1 + 3F",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

        }
        // non in-place
        {
            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Ciphertext destination = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly("1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3");
            plain2.fromHexPoly("1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1");
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.subPlain(encrypted1, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 3Fx^16 + 1x^12 + 1x^10 + 3Fx^8 + 1x^6 + 1x^4 + 1x^3 + 3F",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("0");
            plain2.fromHexPoly("0");
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.subPlain(encrypted1, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("0");
            plain2.fromHexPoly("1x^2 + 1");
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.subPlain(encrypted1, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3Fx^2 + 3F",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("1x^2 + 1");
            plain2.fromHexPoly("3Fx^1 + 3F");
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.subPlain(encrypted1, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^2 + 1x^1 + 2",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("3Fx^2 + 3Fx^1 + 3F");
            plain2.fromHexPoly("1x^1");
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.subPlain(encrypted1, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3Fx^2 + 3Ex^1 + 3F",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

        }
    }

    @Test
    public void bfvEncryptAddPlainDecrypt() {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(64);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());
        // in-place
        {
            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            String hexPoly1;
            String hexPoly2;

            hexPoly1 = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
            hexPoly2 = "1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlainInplace(encrypted1, plain2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 2x^18 + 1x^16 + 2x^14 + 1x^12 + 1x^10 + 2x^9 + 1x^8 + "
                            + "1x^6 + 2x^5 + 1x^4 + 1x^3 + 1",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "0";
            hexPoly2 = "0";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlainInplace(encrypted1, plain2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));


            hexPoly1 = "0";
            hexPoly2 = "1x^2 + 1";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlainInplace(encrypted1, plain2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^2 + 1",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "1x^2 + 1";
            hexPoly2 = "3Fx^1 + 3F";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlainInplace(encrypted1, plain2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^2 + 3Fx^1",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "3Fx^2 + 3Fx^1 + 3F";
            hexPoly2 = "1x^1";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlainInplace(encrypted1, plain2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "3Fx^2 + 3F",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "2x^2 + 1x^1 + 3";
            hexPoly2 = "3x^3 + 4x^2 + 5x^1 + 6";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlainInplace(encrypted1, plain2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "3x^3 + 6x^2 + 6x^1 + 9",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "3x^5 + 1x^4 + 4x^3 + 1";
            hexPoly2 = "5x^2 + 9x^1 + 2";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlainInplace(encrypted1, plain2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "3x^5 + 1x^4 + 4x^3 + 5x^2 + 9x^1 + 3",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));
        }

        // non in-place
        {
            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Ciphertext destination = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            String hexPoly1;
            String hexPoly2;

            hexPoly1 = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
            hexPoly2 = "1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlain(encrypted1, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 2x^18 + 1x^16 + 2x^14 + 1x^12 + 1x^10 + 2x^9 + 1x^8 + "
                            + "1x^6 + 2x^5 + 1x^4 + 1x^3 + 1",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "0";
            hexPoly2 = "0";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlain(encrypted1, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));


            hexPoly1 = "0";
            hexPoly2 = "1x^2 + 1";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlain(encrypted1, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^2 + 1",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "1x^2 + 1";
            hexPoly2 = "3Fx^1 + 3F";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlain(encrypted1, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^2 + 3Fx^1",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "3Fx^2 + 3Fx^1 + 3F";
            hexPoly2 = "1x^1";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlain(encrypted1, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3Fx^2 + 3F",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "2x^2 + 1x^1 + 3";
            hexPoly2 = "3x^3 + 4x^2 + 5x^1 + 6";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlain(encrypted1, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3x^3 + 6x^2 + 6x^1 + 9",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "3x^5 + 1x^4 + 4x^3 + 1";
            hexPoly2 = "5x^2 + 9x^1 + 2";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);

            evaluatorParallel.addPlain(encrypted1, plain2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3x^5 + 1x^4 + 4x^3 + 5x^2 + 9x^1 + 3",
                    plain.toString()
            );

            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));
        }

    }


    @Test
    public void bfvEncryptSubDecrypt() {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(64);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());
        // in-place
        {
            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly("1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3");
            plain2.fromHexPoly("1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.subInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 3Fx^16 + 1x^12 + 1x^10 + 3Fx^8 + 1x^6 + 1x^4 + 1x^3 + 3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("0");
            plain2.fromHexPoly("0");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.subInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("0");
            plain2.fromHexPoly("1x^2 + 1");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.subInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "3Fx^2 + 3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("1x^2 + 1");
            plain2.fromHexPoly("3Fx^1 + 3F");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.subInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^2 + 1x^1 + 2",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("3Fx^2 + 3Fx^1 + 3F");
            plain2.fromHexPoly("1x^1");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.subInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "3Fx^2 + 3Ex^1 + 3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

        }
        // non in-place
        {
            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Ciphertext destination = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();

            plain1.fromHexPoly("1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3");
            plain2.fromHexPoly("1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.sub(encrypted1, encrypted2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 3Fx^16 + 1x^12 + 1x^10 + 3Fx^8 + 1x^6 + 1x^4 + 1x^3 + 3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("0");
            plain2.fromHexPoly("0");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.sub(encrypted1, encrypted2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("0");
            plain2.fromHexPoly("1x^2 + 1");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.sub(encrypted1, encrypted2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3Fx^2 + 3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("1x^2 + 1");
            plain2.fromHexPoly("3Fx^1 + 3F");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.sub(encrypted1, encrypted2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^2 + 1x^1 + 2",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            plain1.fromHexPoly("3Fx^2 + 3Fx^1 + 3F");
            plain2.fromHexPoly("1x^1");
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.sub(encrypted1, encrypted2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3Fx^2 + 3Ex^1 + 3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

        }
    }


    @Test
    public void bfvEncryptAddManyDecrypt() {
        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(128);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40}));

        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

        Ciphertext encrypted1 = new Ciphertext();
        Ciphertext encrypted2 = new Ciphertext();
        Ciphertext encrypted3 = new Ciphertext();
        Ciphertext encrypted4 = new Ciphertext();
        Ciphertext sum = new Ciphertext();

        Plaintext plain = new Plaintext();
        Plaintext plain1 = new Plaintext();
        Plaintext plain2 = new Plaintext();
        Plaintext plain3 = new Plaintext();
        Plaintext plain4 = new Plaintext();

        plain1.fromHexPoly("1x^2 + 1");
        plain2.fromHexPoly("1x^2 + 1x^1");
        plain3.fromHexPoly("1x^2 + 1x^1 + 1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);

        Ciphertext[] encrypteds = new Ciphertext[]{encrypted1, encrypted2, encrypted3};
        evaluatorParallel.addMany(encrypteds, sum);
        decryptor.decrypt(sum, plain);
        Assert.assertEquals(
                "3x^2 + 2x^1 + 2",
                plain.toString()
        );
        Assert.assertTrue(encrypted1.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(encrypted2.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(encrypted3.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(sum.getParmsId().equals(context.getFirstParmsId()));

        plain1.fromHexPoly("3Fx^3 + 3F");
        plain2.fromHexPoly("3Fx^4 + 3F");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);

        encrypteds = new Ciphertext[]{encrypted1, encrypted2};
        evaluatorParallel.addMany(encrypteds, sum);
        decryptor.decrypt(sum, plain);
        Assert.assertEquals(
                "3Fx^4 + 3Fx^3 + 3E",
                plain.toString()
        );
        Assert.assertTrue(encrypted1.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(encrypted2.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(sum.getParmsId().equals(context.getFirstParmsId()));


        plain1.fromHexPoly("1x^1");
        plain2.fromHexPoly("3Fx^4 + 3Fx^3 + 3Fx^2 + 3Fx^1 + 3F");
        plain3.fromHexPoly("1x^2 + 1x^1 + 1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);

        encrypteds = new Ciphertext[]{encrypted1, encrypted2, encrypted3};
        evaluatorParallel.addMany(encrypteds, sum);
        decryptor.decrypt(sum, plain);
        Assert.assertEquals(
                "3Fx^4 + 3Fx^3 + 1x^1",
                plain.toString()
        );
        Assert.assertTrue(encrypted1.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(encrypted2.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(encrypted3.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(sum.getParmsId().equals(context.getFirstParmsId()));


        plain1.fromHexPoly("1");
        plain2.fromHexPoly("3F");
        plain3.fromHexPoly("1");
        plain4.fromHexPoly("3F");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        encryptor.encrypt(plain4, encrypted4);

        encrypteds = new Ciphertext[]{encrypted1, encrypted2, encrypted3, encrypted4};
        evaluatorParallel.addMany(encrypteds, sum);
        decryptor.decrypt(sum, plain);
        Assert.assertEquals(
                "0",
                plain.toString()
        );
        Assert.assertTrue(encrypted1.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(encrypted2.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(encrypted3.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(encrypted4.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(sum.getParmsId().equals(context.getFirstParmsId()));

        plain1.fromHexPoly("1x^16 + 1x^15 + 1x^8 + 1x^7 + 1x^6 + 1x^3 + 1x^2 + 1");
        plain2.fromHexPoly("0");
        plain3.fromHexPoly("1x^13 + 1x^12 + 1x^5 + 1x^4 + 1x^3 + 1");
        plain4.fromHexPoly("1x^15 + 1x^10 + 1x^9 + 1x^8 + 1x^2 + 1x^1 + 1");
        encryptor.encrypt(plain1, encrypted1);
        encryptor.encrypt(plain2, encrypted2);
        encryptor.encrypt(plain3, encrypted3);
        encryptor.encrypt(plain4, encrypted4);

        encrypteds = new Ciphertext[]{encrypted1, encrypted2, encrypted3, encrypted4};
        evaluatorParallel.addMany(encrypteds, sum);
        decryptor.decrypt(sum, plain);
        Assert.assertEquals(
                "1x^16 + 2x^15 + 1x^13 + 1x^12 + 1x^10 + 1x^9 + 2x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 2x^3 + 2x^2 + 1x^1 + 3",
                plain.toString()
        );
        Assert.assertTrue(encrypted1.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(encrypted2.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(encrypted3.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(encrypted4.getParmsId().equals(sum.getParmsId()));
        Assert.assertTrue(sum.getParmsId().equals(context.getFirstParmsId()));


    }






    @Test
    public void bfvEncryptAddDecrypt() {
        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(64);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());
        // in-place
        {
            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            String hexPoly1;
            String hexPoly2;

            hexPoly1 = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
            hexPoly2 = "1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.addInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 2x^18 + 1x^16 + 2x^14 + 1x^12 + 1x^10 + 2x^9 + 1x^8 + "
                            + "1x^6 + 2x^5 + 1x^4 + 1x^3 + 1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "0";
            hexPoly2 = "0";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.addInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));


            hexPoly1 = "0";
            hexPoly2 = "1x^2 + 1";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.addInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^2 + 1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "1x^2 + 1";
            hexPoly2 = "3Fx^1 + 3F";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.addInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "1x^2 + 3Fx^1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "3Fx^2 + 3Fx^1 + 3F";
            hexPoly2 = "1x^1";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.addInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "3Fx^2 + 3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "2x^2 + 1x^1 + 3";
            hexPoly2 = "3x^3 + 4x^2 + 5x^1 + 6";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.addInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "3x^3 + 6x^2 + 6x^1 + 9",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "3x^5 + 1x^4 + 4x^3 + 1";
            hexPoly2 = "5x^2 + 9x^1 + 2";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.addInplace(encrypted1, encrypted2);
            decryptor.decrypt(encrypted1, plain);
            Assert.assertEquals(
                    "3x^5 + 1x^4 + 4x^3 + 5x^2 + 9x^1 + 3",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));
        }

        // non in-place
        {
            Ciphertext encrypted1 = new Ciphertext();
            Ciphertext encrypted2 = new Ciphertext();
            Ciphertext destination = new Ciphertext();

            Plaintext plain = new Plaintext();
            Plaintext plain1 = new Plaintext();
            Plaintext plain2 = new Plaintext();
            String hexPoly1;
            String hexPoly2;

            hexPoly1 = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
            hexPoly2 = "1x^18 + 1x^16 + 1x^14 + 1x^9 + 1x^8 + 1x^5 + 1";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.add(encrypted1, encrypted2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^28 + 1x^25 + 1x^21 + 1x^20 + 2x^18 + 1x^16 + 2x^14 + 1x^12 + 1x^10 + 2x^9 + 1x^8 + "
                            + "1x^6 + 2x^5 + 1x^4 + 1x^3 + 1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "0";
            hexPoly2 = "0";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.add(encrypted1, encrypted2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));


            hexPoly1 = "0";
            hexPoly2 = "1x^2 + 1";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.add(encrypted1, encrypted2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^2 + 1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "1x^2 + 1";
            hexPoly2 = "3Fx^1 + 3F";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.add(encrypted1, encrypted2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^2 + 3Fx^1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "3Fx^2 + 3Fx^1 + 3F";
            hexPoly2 = "1x^1";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.add(encrypted1, encrypted2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3Fx^2 + 3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "2x^2 + 1x^1 + 3";
            hexPoly2 = "3x^3 + 4x^2 + 5x^1 + 6";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.add(encrypted1, encrypted2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3x^3 + 6x^2 + 6x^1 + 9",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));

            hexPoly1 = "3x^5 + 1x^4 + 4x^3 + 1";
            hexPoly2 = "5x^2 + 9x^1 + 2";
            plain1.fromHexPoly(hexPoly1);
            plain2.fromHexPoly(hexPoly2);
            encryptor.encrypt(plain1, encrypted1);
            encryptor.encrypt(plain2, encrypted2);
            evaluatorParallel.add(encrypted1, encrypted2, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3x^5 + 1x^4 + 4x^3 + 5x^2 + 9x^1 + 3",
                    plain.toString()
            );
            Assert.assertTrue(encrypted2.getParmsId().equals(encrypted1.getParmsId()));
            Assert.assertTrue(encrypted1.getParmsId().equals(context.getFirstParmsId()));
        }

    }

    @Test
    public void bfvEncryptNegateDecrypt() {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPolyModulusDegree(64);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));

        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk);
        EvaluatorParallel evaluatorParallel = new EvaluatorParallel(context);
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

        // in-place
        {
            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            String hexPoly;

            hexPoly = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
            // 1 + (3F) mod 64 = 0
            // 1 + (3 * 16 + 15 = 48 + 15 = 1 + 63 mod 64 = 0)
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.negateInplace(encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "3Fx^28 + 3Fx^25 + 3Fx^21 + 3Fx^20 + 3Fx^18 + 3Fx^14 + 3Fx^12 + 3Fx^10 + 3Fx^9 + 3Fx^6 + 3Fx^5 + 3Fx^4 + 3Fx^3",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            hexPoly = "0";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.negateInplace(encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            hexPoly = "1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.negateInplace(encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            hexPoly = "3F";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.negateInplace(encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            hexPoly = "1x^1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.negateInplace(encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "3Fx^1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            hexPoly = "3Fx^2 + 3F";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.negateInplace(encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(
                    "1x^2 + 1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));
        }
        // non in-place
        {
            Ciphertext encrypted = new Ciphertext();
            Ciphertext destination = new Ciphertext();
            Plaintext plain = new Plaintext();
            String hexPoly;

            hexPoly = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
            // 1 + (3F) mod 64 = 0
            // 1 + (3 * 16 + 15 = 48 + 15 = 1 + 63 mod 64 = 0)
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.negate(encrypted, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3Fx^28 + 3Fx^25 + 3Fx^21 + 3Fx^20 + 3Fx^18 + 3Fx^14 + 3Fx^12 + 3Fx^10 + 3Fx^9 + 3Fx^6 + 3Fx^5 + 3Fx^4 + 3Fx^3",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            hexPoly = "0";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.negate(encrypted, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "0",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            hexPoly = "1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.negate(encrypted, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3F",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            hexPoly = "3F";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.negate(encrypted, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            hexPoly = "1x^1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.negate(encrypted, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "3Fx^1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));

            hexPoly = "3Fx^2 + 3F";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            evaluatorParallel.negate(encrypted, destination);
            decryptor.decrypt(destination, plain);
            Assert.assertEquals(
                    "1x^2 + 1",
                    plain.toString()
            );
            Assert.assertTrue(encrypted.getParmsId().equals(context.getFirstParmsId()));
        }
    }



}
