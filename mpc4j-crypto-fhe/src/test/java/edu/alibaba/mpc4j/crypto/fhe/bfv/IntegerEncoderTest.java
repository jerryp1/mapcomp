package edu.alibaba.mpc4j.crypto.fhe.bfv;

import org.junit.Test;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/7/14
 */
public class IntegerEncoderTest {


    private IntegerEncoder encoder;

    public IntegerEncoderTest() {

        long degree = 2048;
        long plainModulus = 256;
        long cipherModulus = 0x3fffffff000001L;

        encoder = new IntegerEncoder(degree);
    }

    @Test
    public void testSpecificEncodeDecode() {
        Plaintext plaintext = encoder.encode(21);
        long[] truth = new long[(int) encoder.polyModulusDegree];
        truth[0] = 1;
        truth[1] = 0;
        truth[2] = 1;
        truth[3] = 0;
        truth[4] = 1;
        assert Arrays.equals(plaintext.poly.coeffs, truth);

        Plaintext plain2 = new Plaintext(encoder.polyModulusDegree, truth);
        long value = encoder.decode(plain2);
        assert value == 21;
    }

    @Test
    public void testEncodeDecode() {
        runEncodeDecode(21321);
        runEncodeDecode(839);
        runEncodeDecode(9000);
    }

    @Test
    public void testEncodeNegative() {
        runEncodeDecode(-6);
    }


    public void runEncodeDecode(long value) {
        Plaintext plain = encoder.encode(value);
        long decode = encoder.decode(plain);
        assert decode == value;
    }

}
