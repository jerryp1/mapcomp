package edu.alibaba.mpc4j.crypto.fhe;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.keys.GaloisKeys;
import edu.alibaba.mpc4j.crypto.fhe.keys.PublicKey;
import edu.alibaba.mpc4j.crypto.fhe.keys.RelinKeys;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.utils.ValueChecker;

import org.junit.Assert;
import org.junit.Test;

/**
 * Key Generator Test.
 *
 * @author Qixian Zhou
 * @date 2023/9/22
 */
public class KeyGeneratorTest {

    private static final int MAX_LOOP = 100;

    @Test
    public void bfvKeyGeneration2() {
        for (int i = 0; i < MAX_LOOP; i++) {
            bfvKeyGeneration();
        }
    }

    @Test
    public void bfvKeyGeneration() {
        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);

        {
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(65537);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60}));
            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            // throw exception
            Assert.assertThrows(IllegalArgumentException.class, keyGenerator::createRelinKeys);
            Assert.assertThrows(IllegalArgumentException.class, keyGenerator::createGaloisKeys);
        }

        {
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(65537);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60}));
            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            RelinKeys evk = new RelinKeys();
            keyGenerator.createRelinKeys(evk);
            Assert.assertEquals(evk.parmsId(), context.getKeyParmsId());
            Assert.assertEquals(1, evk.key(2).length);
            for (PublicKey[] a : evk.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValueChecker.isValidFor(evk, context));

            GaloisKeys galoisKeys = new GaloisKeys();
            keyGenerator.createGaloisKeys(galoisKeys);
            for (PublicKey[] a : galoisKeys.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValueChecker.isValidFor(galoisKeys, context));

            Assert.assertEquals(galoisKeys.parmsId(), context.getKeyParmsId());
            Assert.assertEquals(1, galoisKeys.key(3).length);
            Assert.assertEquals(10, galoisKeys.size());

            // new galoisKeys
            keyGenerator.createGaloisKeys(new int[]{1, 3, 5, 7}, galoisKeys);
            Assert.assertEquals(galoisKeys.parmsId(), context.getKeyParmsId());
            Assert.assertTrue(galoisKeys.hasKey(1));
            Assert.assertTrue(galoisKeys.hasKey(3));
            Assert.assertTrue(galoisKeys.hasKey(5));
            Assert.assertTrue(galoisKeys.hasKey(7));
            Assert.assertFalse(galoisKeys.hasKey(9));
            Assert.assertFalse(galoisKeys.hasKey(127));

            keyGenerator.createGaloisKeys(new int[]{1}, galoisKeys);
            Assert.assertEquals(galoisKeys.parmsId(), context.getKeyParmsId());
            Assert.assertTrue(galoisKeys.hasKey(1));
            Assert.assertFalse(galoisKeys.hasKey(3));
            Assert.assertFalse(galoisKeys.hasKey(127));
            Assert.assertEquals(1, galoisKeys.key(1).length);
            Assert.assertEquals(1, galoisKeys.size());

            keyGenerator.createGaloisKeys(new int[]{127}, galoisKeys);
            Assert.assertEquals(galoisKeys.parmsId(), context.getKeyParmsId());
            Assert.assertFalse(galoisKeys.hasKey(1));
            Assert.assertTrue(galoisKeys.hasKey(127));
            Assert.assertEquals(1, galoisKeys.key(127).length);
            Assert.assertEquals(1, galoisKeys.size());
        }
        {
            parms.setPolyModulusDegree(256);
            parms.setPlainModulus(65537);
            parms.setCoeffModulus(CoeffModulus.create(256, new int[]{60, 30, 30}));

            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);

            RelinKeys relinKeys = new RelinKeys();
            keyGenerator.createRelinKeys(relinKeys);

            Assert.assertEquals(relinKeys.parmsId(), context.getKeyParmsId());
            Assert.assertEquals(2, relinKeys.key(2).length);
            for (PublicKey[] a : relinKeys.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValueChecker.isValidFor(relinKeys, context));

            GaloisKeys galoisKeys = new GaloisKeys();
            keyGenerator.createGaloisKeys(galoisKeys);
            for (PublicKey[] a : galoisKeys.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValueChecker.isValidFor(galoisKeys, context));
            Assert.assertEquals(galoisKeys.parmsId(), context.getKeyParmsId());
            Assert.assertEquals(2, galoisKeys.key(3).length);
            Assert.assertEquals(14, galoisKeys.size());

            keyGenerator.createGaloisKeys(new int[]{1, 3, 5, 7}, galoisKeys);
            Assert.assertEquals(galoisKeys.parmsId(), context.getKeyParmsId());
            Assert.assertTrue(galoisKeys.hasKey(1));
            Assert.assertTrue(galoisKeys.hasKey(3));
            Assert.assertTrue(galoisKeys.hasKey(5));
            Assert.assertTrue(galoisKeys.hasKey(7));
            Assert.assertFalse(galoisKeys.hasKey(9));
            Assert.assertFalse(galoisKeys.hasKey(511));
            Assert.assertEquals(2, galoisKeys.key(1).length);
            Assert.assertEquals(2, galoisKeys.key(3).length);
            Assert.assertEquals(2, galoisKeys.key(5).length);
            Assert.assertEquals(2, galoisKeys.key(7).length);
            Assert.assertEquals(4, galoisKeys.size());

            keyGenerator.createGaloisKeys(new int[]{1}, galoisKeys);
            Assert.assertEquals(galoisKeys.parmsId(), context.getKeyParmsId());
            Assert.assertTrue(galoisKeys.hasKey(1));
            Assert.assertFalse(galoisKeys.hasKey(3));
            Assert.assertFalse(galoisKeys.hasKey(511));
            Assert.assertEquals(2, galoisKeys.key(1).length);
            Assert.assertEquals(1, galoisKeys.size());

            keyGenerator.createGaloisKeys(new int[]{511}, galoisKeys);
            Assert.assertEquals(galoisKeys.parmsId(), context.getKeyParmsId());
            Assert.assertFalse(galoisKeys.hasKey(1));
            Assert.assertTrue(galoisKeys.hasKey(511));
            Assert.assertEquals(2, galoisKeys.key(511).length);
            Assert.assertEquals(1, galoisKeys.size());
        }
    }

    @Test
    public void tempTest() {
        EncryptionParams parms = new EncryptionParams(SchemeType.BFV);
        {
            parms.setPolyModulusDegree(64);
            parms.setPlainModulus(65537);
            parms.setCoeffModulus(CoeffModulus.create(64, new int[]{60, 60}));
            Context context = new Context(parms, false, CoeffModulus.SecurityLevelType.NONE);
            KeyGenerator keyGenerator = new KeyGenerator(context);
            RelinKeys evk = new RelinKeys();
            keyGenerator.createRelinKeys(evk);
            Assert.assertEquals(evk.parmsId(), context.getKeyParmsId());
            Assert.assertEquals(1, evk.key(2).length);
            for (PublicKey[] a : evk.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValueChecker.isValidFor(evk, context));

            GaloisKeys galoisKeys = new GaloisKeys();
            keyGenerator.createGaloisKeys(galoisKeys);
            for (PublicKey[] a : galoisKeys.data()) {
                for (PublicKey b : a) {
                    Assert.assertFalse(b.data().isTransparent());
                }
            }
            Assert.assertTrue(ValueChecker.isValidFor(galoisKeys, context));
        }
    }
}
