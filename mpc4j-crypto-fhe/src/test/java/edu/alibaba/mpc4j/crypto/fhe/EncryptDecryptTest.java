package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.keys.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import org.junit.Assert;
import org.junit.Test;


/**
 * Encrypt and Decrypt Test.
 *
 * @author Qixian Zhou
 * @date 2023/9/27
 */
public class EncryptDecryptTest {

    private static final int MAX_LOOP = 1;

    @Test
    public void bfvEncryptZeroDecryptRandom() {
        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPlainModulus(plainModulus);
        parms.setPolyModulusDegree(64);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40, 40, 40}));
        Context context = new Context(parms, true, CoeffModulus.SecurityLevelType.NONE);

        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk, keyGenerator.getSecretKey());
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

        Ciphertext ct = new Ciphertext();
        Plaintext pt = new Plaintext();
        ParmsIdType nextParms = context.firstContextData().getNextContextData().getParmsId();
        for (int i = 0; i < MAX_LOOP; i++) {
            {
                encryptor.encryptZero(ct);
                Assert.assertFalse(ct.isNttForm());
                Assert.assertFalse(ct.isTransparent());
                Assert.assertTrue(Common.areClose(1.0, ct.getScale()));
                Assert.assertEquals(1, ct.getCorrectionFactor());
                decryptor.decrypt(ct, pt);
                Assert.assertTrue(pt.isZero());

                encryptor.encryptZero(nextParms, ct);
                Assert.assertFalse(ct.isNttForm());
                Assert.assertFalse(ct.isTransparent());
                Assert.assertTrue(Common.areClose(1.0, ct.getScale()));
                Assert.assertEquals(1, ct.getCorrectionFactor());
                Assert.assertEquals(ct.getParmsId(), nextParms);
                decryptor.decrypt(ct, pt);
                Assert.assertTrue(pt.isZero());
            }
            {
                encryptor.encryptZeroSymmetric(ct);
                Assert.assertFalse(ct.isNttForm());
                Assert.assertFalse(ct.isTransparent());
                Assert.assertTrue(Common.areClose(1.0, ct.getScale()));
                Assert.assertEquals(1, ct.getCorrectionFactor());
                decryptor.decrypt(ct, pt);
                Assert.assertTrue(pt.isZero());

                encryptor.encryptZeroSymmetric(nextParms, ct);
                Assert.assertFalse(ct.isNttForm());
                Assert.assertFalse(ct.isTransparent());
                Assert.assertTrue(Common.areClose(1.0, ct.getScale()));
                Assert.assertEquals(1, ct.getCorrectionFactor());
                Assert.assertEquals(ct.getParmsId(), nextParms);
                decryptor.decrypt(ct, pt);
                Assert.assertTrue(pt.isZero());
            }

        }
    }


    @Test
    public void bfvEncryptZeroDecrypt() {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPlainModulus(plainModulus);
        parms.setPolyModulusDegree(64);
        parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40, 40, 40}));
        Context context = new Context(parms, true, CoeffModulus.SecurityLevelType.NONE);


        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk, keyGenerator.getSecretKey());
        Decryptor decryptor = new Decryptor(context, keyGenerator.getSecretKey());

        Ciphertext ct = new Ciphertext();
        Plaintext pt = new Plaintext();
        ParmsIdType nextParms = context.firstContextData().getNextContextData().getParmsId();
        {
            encryptor.encryptZero(ct);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertTrue(Common.areClose(1.0, ct.getScale()));
            Assert.assertEquals(1, ct.getCorrectionFactor());
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());

            encryptor.encryptZero(nextParms, ct);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertTrue(Common.areClose(1.0, ct.getScale()));
            Assert.assertEquals(1, ct.getCorrectionFactor());
            Assert.assertEquals(ct.getParmsId(), nextParms);
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());
        }
        {
            encryptor.encryptZeroSymmetric(ct);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertTrue(Common.areClose(1.0, ct.getScale()));
            Assert.assertEquals(1, ct.getCorrectionFactor());
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());

            encryptor.encryptZeroSymmetric(nextParms, ct);
            Assert.assertFalse(ct.isNttForm());
            Assert.assertFalse(ct.isTransparent());
            Assert.assertTrue(Common.areClose(1.0, ct.getScale()));
            Assert.assertEquals(1, ct.getCorrectionFactor());
            Assert.assertEquals(ct.getParmsId(), nextParms);
            decryptor.decrypt(ct, pt);
            Assert.assertTrue(pt.isZero());
        }

    }

    @Test
    public void bfvEncryptDecryptRandom() {
        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPlainModulus(plainModulus);

        for (int i = 0; i < MAX_LOOP; i++) {
            {
                parms.setPolyModulusDegree(64);
                parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));
                Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
                KeyGenerator keygen = new KeyGenerator(context);
                PublicKey pk = new PublicKey();
                keygen.createPublicKey(pk);

                Encryptor encryptor = new Encryptor(context, pk);
                Decryptor decryptor = new Decryptor(context, keygen.getSecretKey());

                Ciphertext encrypted = new Ciphertext();
                Plaintext plain = new Plaintext();
                String hexPoly;
                hexPoly = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());

                hexPoly = "0";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());


                hexPoly = "1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());

                hexPoly = "1x^1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());


                hexPoly =
                        "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                                + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                                + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                                + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                                + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());


                hexPoly =
                        "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                                + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                                + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                                + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                                + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());


                hexPoly =
                        "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                                + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                                + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                                + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                                + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1 + 1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());

                hexPoly = "1x^28 + 1x^25 + 1x^23 + 1x^21 + 1x^20 + 1x^19 + 1x^16 + 1x^15 + 1x^13 + 1x^12 + 1x^7 + 1x^5 + 1";

                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());
            }

            {
                parms.setPolyModulusDegree(128);
                parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40}));
                Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
                KeyGenerator keygen = new KeyGenerator(context);
                PublicKey pk = new PublicKey();
                keygen.createPublicKey(pk);

                Encryptor encryptor = new Encryptor(context, pk);
                Decryptor decryptor = new Decryptor(context, keygen.getSecretKey());

                Ciphertext encrypted = new Ciphertext();
                Plaintext plain = new Plaintext();
                String hexPoly;
                hexPoly = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());

                hexPoly = "0";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());


                hexPoly = "1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());

                hexPoly = "1x^1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());


                hexPoly =
                        "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                                + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                                + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                                + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                                + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());


                hexPoly =
                        "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                                + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                                + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                                + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                                + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());


                hexPoly =
                        "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                                + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                                + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                                + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                                + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1 + 1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());

                hexPoly = "1x^28 + 1x^25 + 1x^23 + 1x^21 + 1x^20 + 1x^19 + 1x^16 + 1x^15 + 1x^13 + 1x^12 + 1x^7 + 1x^5 + 1";

                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());
            }

            {
                parms.setPolyModulusDegree(256);
                parms.setCoeffModulus(CoeffModulus.create(256, new int[]{40, 40, 40}));

                Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
                KeyGenerator keygen = new KeyGenerator(context);
                PublicKey pk = new PublicKey();
                keygen.createPublicKey(pk);

                Encryptor encryptor = new Encryptor(context, pk);
                Decryptor decryptor = new Decryptor(context, keygen.getSecretKey());

                Ciphertext encrypted = new Ciphertext();
                Plaintext plain = new Plaintext();
                String hexPoly;

                hexPoly = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());

                hexPoly = "0";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());

                hexPoly = "1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());

                hexPoly = "1x^1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());


                hexPoly =
                        "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                                + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                                + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                                + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                                + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());


                hexPoly =
                        "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                                + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                                + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                                + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                                + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());


                hexPoly =
                        "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                                + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                                + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                                + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                                + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1 + 1";
                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());

                hexPoly = "1x^28 + 1x^25 + 1x^23 + 1x^21 + 1x^20 + 1x^19 + 1x^16 + 1x^15 + 1x^13 + 1x^12 + 1x^7 + 1x^5 + 1";

                plain.fromHexPoly(hexPoly);
                encryptor.encrypt(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());
            }

            {
                parms.setPolyModulusDegree(256);
                parms.setCoeffModulus(CoeffModulus.create(256, new int[]{40, 40, 40}));
                Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
                KeyGenerator keygen = new KeyGenerator(context);

                PublicKey pk = new PublicKey();
                keygen.createPublicKey(pk);
                // 注意是用的 sk 实例化
                Encryptor encryptor = new Encryptor(context, keygen.getSecretKey());
                Decryptor decryptor = new Decryptor(context, keygen.getSecretKey());

                Ciphertext encrypted = new Ciphertext();
                Plaintext plain = new Plaintext();
                String hexPoly;

                hexPoly = "1x^28 + 1x^25 + 1x^23 + 1x^21 + 1x^20 + 1x^19 + 1x^16 + 1x^15 + 1x^13 + 1x^12 + 1x^7 + 1x^5 + 1";
                plain.fromHexPoly(hexPoly);
                // 对称加密
                encryptor.encryptSymmetric(plain, encrypted);
                decryptor.decrypt(encrypted, plain);
                Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
                Assert.assertEquals(hexPoly, plain.toString());
            }

        }

    }

    @Test
    public void bfvEncryptDecrypt() {
        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPlainModulus(plainModulus);
        {
            parms.setPolyModulusDegree(64);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{40}));
            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.getSecretKey());

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            String hexPoly;
            hexPoly = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "0";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());


            hexPoly = "1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1x^1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());


            hexPoly =
                    "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                            + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                            + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                            + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                            + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());


            hexPoly =
                    "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                            + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                            + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                            + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                            + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());


            hexPoly =
                    "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                            + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                            + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                            + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                            + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1 + 1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1x^28 + 1x^25 + 1x^23 + 1x^21 + 1x^20 + 1x^19 + 1x^16 + 1x^15 + 1x^13 + 1x^12 + 1x^7 + 1x^5 + 1";

            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());
        }

        {
            parms.setPolyModulusDegree(128);
            parms.setCoeffModulus(CoeffModulus.create(128, new int[]{40, 40}));
            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.getSecretKey());

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            String hexPoly;
            hexPoly = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "0";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());


            hexPoly = "1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1x^1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());


            hexPoly =
                    "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                            + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                            + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                            + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                            + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());


            hexPoly =
                    "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                            + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                            + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                            + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                            + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());


            hexPoly =
                    "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                            + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                            + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                            + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                            + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1 + 1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1x^28 + 1x^25 + 1x^23 + 1x^21 + 1x^20 + 1x^19 + 1x^16 + 1x^15 + 1x^13 + 1x^12 + 1x^7 + 1x^5 + 1";

            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());
        }

        {
            parms.setPolyModulusDegree(256);
            parms.setCoeffModulus(CoeffModulus.create(256, new int[]{40, 40, 40}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);
            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);

            Encryptor encryptor = new Encryptor(context, pk);
            Decryptor decryptor = new Decryptor(context, keygen.getSecretKey());

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            String hexPoly;

            hexPoly = "1x^28 + 1x^25 + 1x^21 + 1x^20 + 1x^18 + 1x^14 + 1x^12 + 1x^10 + 1x^9 + 1x^6 + 1x^5 + 1x^4 + 1x^3";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "0";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1x^1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());


            hexPoly =
                    "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                            + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                            + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                            + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                            + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());


            hexPoly =
                    "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                            + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                            + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                            + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                            + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());


            hexPoly =
                    "1x^62 + 1x^61 + 1x^60 + 1x^59 + 1x^58 + 1x^57 + 1x^56 + 1x^55 + 1x^54 + 1x^53 + 1x^52 + 1x^51 + 1x^50 "
                            + "+ 1x^49 + 1x^48 + 1x^47 + 1x^46 + 1x^45 + 1x^44 + 1x^43 + 1x^42 + 1x^41 + 1x^40 + 1x^39 + 1x^38 + "
                            + "1x^37 + 1x^36 + 1x^35 + 1x^34 + 1x^33 + 1x^32 + 1x^31 + 1x^30 + 1x^29 + 1x^28 + 1x^27 + 1x^26 + 1x^25 "
                            + "+ 1x^24 + 1x^23 + 1x^22 + 1x^21 + 1x^20 + 1x^19 + 1x^18 + 1x^17 + 1x^16 + 1x^15 + 1x^14 + 1x^13 + "
                            + "1x^12 + 1x^11 + 1x^10 + 1x^9 + 1x^8 + 1x^7 + 1x^6 + 1x^5 + 1x^4 + 1x^3 + 1x^2 + 1x^1 + 1";
            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());

            hexPoly = "1x^28 + 1x^25 + 1x^23 + 1x^21 + 1x^20 + 1x^19 + 1x^16 + 1x^15 + 1x^13 + 1x^12 + 1x^7 + 1x^5 + 1";

            plain.fromHexPoly(hexPoly);
            encryptor.encrypt(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());
        }

        {
            parms.setPolyModulusDegree(256);
            parms.setCoeffModulus(CoeffModulus.create(256, new int[]{40, 40, 40}));
            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keygen = new KeyGenerator(context);

            PublicKey pk = new PublicKey();
            keygen.createPublicKey(pk);
            // 注意是用的 sk 实例化
            Encryptor encryptor = new Encryptor(context, keygen.getSecretKey());
            Decryptor decryptor = new Decryptor(context, keygen.getSecretKey());

            Ciphertext encrypted = new Ciphertext();
            Plaintext plain = new Plaintext();
            String hexPoly;

            hexPoly = "1x^28 + 1x^25 + 1x^23 + 1x^21 + 1x^20 + 1x^19 + 1x^16 + 1x^15 + 1x^13 + 1x^12 + 1x^7 + 1x^5 + 1";
            plain.fromHexPoly(hexPoly);
            // 对称加密
            encryptor.encryptSymmetric(plain, encrypted);
            decryptor.decrypt(encrypted, plain);
            Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
            Assert.assertEquals(hexPoly, plain.toString());
        }


    }

    @Test
    public void myTest() {
        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPlainModulus(plainModulus);
        parms.setPolyModulusDegree(256);
        parms.setCoeffModulus(CoeffModulus.create(256, new int[]{40, 40, 40, 40}));
        Context context = new Context(parms, true, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keygen = new KeyGenerator(context);

        PublicKey pk = new PublicKey();
        keygen.createPublicKey(pk);
        // 注意是用的 sk 实例化
        Encryptor encryptor = new Encryptor(context, keygen.getSecretKey());
        Decryptor decryptor = new Decryptor(context, keygen.getSecretKey());
        Evaluator evaluator = new Evaluator(context);
        Ciphertext encrypted = new Ciphertext();
        Plaintext plain = new Plaintext();
        String hexPoly;

        hexPoly = "1x^28 + 1x^25 + 1x^23 + 1x^21 + 1x^20 + 1x^19 + 1x^16 + 1x^15 + 1x^13 + 1x^12 + 1x^7 + 1x^5 + 1";
        plain.fromHexPoly(hexPoly);
        // 对称加密
        encryptor.encryptSymmetric(plain, encrypted);
        evaluator.negateInplace(encrypted);
        evaluator.negateInplace(encrypted);
        decryptor.decrypt(encrypted, plain);
        Assert.assertEquals(encrypted.getParmsId(), context.getFirstParmsId());
        Assert.assertEquals(hexPoly, plain.toString());
    }

}
