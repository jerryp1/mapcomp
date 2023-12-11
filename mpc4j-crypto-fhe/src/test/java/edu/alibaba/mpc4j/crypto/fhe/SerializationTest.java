package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.keys.GaloisKeys;
import edu.alibaba.mpc4j.crypto.fhe.keys.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.utils.DynArray;
import edu.alibaba.mpc4j.crypto.fhe.utils.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/10/13
 */
public class SerializationTest {

    private static final int MAX_LOOP = 1000;


    @Test
    public void parmsIdTypeerializationTest() {

        Random random = new Random();
        for (int i = 0; i < MAX_LOOP; i++) {
            long[] value = IntStream.range(0, 4).mapToLong(n -> random.nextLong()).toArray();

            ParmsIdType id = new ParmsIdType(value);
            byte[] idBytes = SerializationUtils.serializeObject(id);
            ParmsIdType id2 = SerializationUtils.deserializeObject(idBytes);
            Assert.assertEquals(id, id2);
        }
    }


    @Test
    public void dynArrayTypeerializationTest() {

        Random random = new Random();
        int n = 1024;
        for (int i = 0; i < MAX_LOOP; i++) {
            long[] value = IntStream.range(0, n).mapToLong(e -> random.nextLong()).toArray();

            DynArray dynArray = new DynArray(value);
            byte[] idBytes = SerializationUtils.serializeObject(dynArray);
            DynArray dynArray2 = SerializationUtils.deserializeObject(idBytes);

            Assert.assertArrayEquals(
                    dynArray.data(),
                    dynArray2.data()
            );
            Assert.assertEquals(
                    dynArray.size(),
                    dynArray2.size()
            );
            Assert.assertEquals(
                    dynArray.capacity(),
                    dynArray2.capacity()
            );

        }
    }


    @Test
    public void ciphertextSerializationTest() {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPlainModulus(plainModulus);
        parms.setPolyModulusDegree(1024);
        parms.setCoeffModulus(CoeffModulus.bfvDefault(1024));
        Context context = new Context(parms, true, CoeffModulus.SecurityLevelType.NONE);

        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();
        keyGenerator.createPublicKey(pk);

        Encryptor encryptor = new Encryptor(context, pk, keyGenerator.getSecretKey());


        Ciphertext ct = new Ciphertext();
        Plaintext pt = new Plaintext("6");

        for (int i = 0; i < MAX_LOOP; i++) {
            encryptor.encrypt(pt, ct);
            byte[] objectBytes = SerializationUtils.serializeObject(ct);
            Ciphertext ct2 = SerializationUtils.deserializeObject(objectBytes);
            Assert.assertEquals(ct, ct2);
        }
    }


    @Test
    public void pkSerializationTest() {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(1 << 6);
        parms.setPlainModulus(plainModulus);
        parms.setPolyModulusDegree(1024);
        parms.setCoeffModulus(CoeffModulus.bfvDefault(1024));
        Context context = new Context(parms, true, CoeffModulus.SecurityLevelType.NONE);

        KeyGenerator keyGenerator = new KeyGenerator(context);
        PublicKey pk = new PublicKey();


        for (int i = 0; i < MAX_LOOP; i++) {
            keyGenerator.createPublicKey(pk);
            byte[] objectBytes = SerializationUtils.serializeObject(pk);
            PublicKey pk2 = SerializationUtils.deserializeObject(objectBytes);
            Assert.assertEquals(pk, pk2);
        }
    }

    @Test
    public void galoisKeysSerializationTest() {

        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        Modulus plainModulus = new Modulus(257);
        parms.setPolyModulusDegree(8);
        parms.setPlainModulus(plainModulus);
        parms.setCoeffModulus(CoeffModulus.create(8, new int[]{40, 40}));
        Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
        KeyGenerator keyGenerator = new KeyGenerator(context);
        GaloisKeys galoisKeys = new GaloisKeys();


        for (int i = 0; i < MAX_LOOP; i++) {
            keyGenerator.createGaloisKeys(galoisKeys);
            byte[] objectBytes = SerializationUtils.serializeObject(galoisKeys);
            GaloisKeys galoisKeys2 = SerializationUtils.deserializeObject(objectBytes);
            Assert.assertEquals(galoisKeys, galoisKeys2);
        }
    }



}
