package edu.alibaba.mpc4j.crypto.fhe.params;

import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGenerator;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGeneratorFactory;
import edu.alibaba.mpc4j.crypto.fhe.zq.Numth;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Qixian Zhou
 * @date 2023/8/30
 */
public class EncryptionParamsTest {


    private void encryptionParametersTest(SchemeType scheme) {

        EncryptionParams parms = new EncryptionParams(scheme);
        parms.setCoeffModulus(new long[]{2, 3});

        if (scheme == SchemeType.BFV || scheme == SchemeType.BGV) {
            parms.setPlainModulus(2);
        }
        parms.setPolyModulusDegree(2);
        parms.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());

        Assert.assertEquals(scheme, parms.getScheme());
        Assert.assertEquals(parms.getCoeffModulus()[0].getValue(), 2);
        Assert.assertEquals(parms.getCoeffModulus()[1].getValue(), 3);

        if (scheme == SchemeType.BFV || scheme == SchemeType.BGV) {
            Assert.assertEquals(parms.getPlainModulus().getValue(), 2);
        } else if (scheme == SchemeType.CKKS) {
            Assert.assertEquals(parms.getPlainModulus().getValue(), 0);
        }

        parms.setCoeffModulus(CoeffModulus.create(2, new int[]{30, 40, 50}));
        if (scheme == SchemeType.BFV || scheme == SchemeType.BGV) {
            parms.setPlainModulus(2);
        }
        parms.setPolyModulusDegree(128);

        Assert.assertTrue(Numth.isPrime(parms.getCoeffModulus()[0].getValue()));
        Assert.assertTrue(Numth.isPrime(parms.getCoeffModulus()[1].getValue()));
        Assert.assertTrue(Numth.isPrime(parms.getCoeffModulus()[2].getValue()));
        if (scheme == SchemeType.BFV || scheme == SchemeType.BGV) {
            Assert.assertEquals(parms.getPlainModulus().getValue(), 2);
        } else if (scheme == SchemeType.CKKS) {
            Assert.assertEquals(parms.getPlainModulus().getValue(), 0);
        }

        Assert.assertEquals(128, parms.getPolyModulusDegree());
    }

    @Test
    public void create() {

        encryptionParametersTest(SchemeType.BFV);

        Assert.assertThrows(IllegalArgumentException.class, () -> encryptionParametersTest(SchemeType.BGV));
        Assert.assertThrows(IllegalArgumentException.class, () -> encryptionParametersTest(SchemeType.CKKS));
        Assert.assertThrows(IllegalArgumentException.class, () -> encryptionParametersTest(SchemeType.NONE));
    }


    private void encryptionParametersCompare(SchemeType scheme) {

        EncryptionParams params1 = new EncryptionParams(scheme);
        params1.setCoeffModulus(CoeffModulus.create(64, new int[]{30}));
        if (scheme == SchemeType.BFV || scheme == SchemeType.BGV) {
            params1.setPlainModulus(1 << 6);
        }
        params1.setPolyModulusDegree(64);
        params1.setRandomGeneratorFactory(new UniformRandomGeneratorFactory());
        // deep copy
        EncryptionParams params2 = new EncryptionParams(params1);
        Assert.assertEquals(params1, params2);
        params2.setPlainModulus(1024);
        Assert.assertNotEquals(params1, params2);

        // shallow copy
        EncryptionParams params3 = params2;
        Assert.assertTrue(params2.equals(params3));
        params3.setCoeffModulus(CoeffModulus.create(64, new int[]{32}));
        Assert.assertTrue(params3.equals(params2));

        params3 = params2;
        Assert.assertEquals(params2, params3);
        params3.setCoeffModulus(CoeffModulus.create(64, new int[]{30, 30}));
        Assert.assertEquals(params3, params2);

    }

    @Test
    public void compare() {

        encryptionParametersCompare(SchemeType.BFV);

    }


}
