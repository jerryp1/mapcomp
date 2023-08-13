package edu.alibaba.mpc4j.crypto.fhe.bfv;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.crypto.fhe.utils.Polynomial;
import edu.alibaba.mpc4j.crypto.fhe.utils.RandomSample;
import edu.alibaba.mpc4j.crypto.fhe.utils.RingLweZp64;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/7/14
 */
public class BatchEncoderTest {

    private static int MAX_RANDOM_NUM = 1000;

    private BatchEncoder encoder;

    public BatchEncoderTest() {

        long degree = 4;
        long plainModulus = 17;
        long cipherModulus = 0x3fffffff000001L;
        BfvParameters parameters = new BfvParameters(degree, plainModulus, cipherModulus);

        encoder = new BatchEncoder(parameters);
    }

    @Test
    public void testEncodeDecode() {

        long[] vec = new long[]{8, 1, 2, 4};
        runEncodeDecode(vec);
        long[] zero = new long[(int) encoder.polyModulusDegree];
        runEncodeDecode(zero);

        for (int i = 0; i < MAX_RANDOM_NUM; i++) {
            long[] values = RandomSample.samplePositiveUniform(0, encoder.plainModulus, (int) encoder.polyModulusDegree);
            runEncodeDecode(values);
        }
    }

    @Test
    public void testEncodeMul() {

        for (int i = 0; i < MAX_RANDOM_NUM; i++) {
            long[] a = RandomSample.sampleUniform(0, encoder.plainModulus, (int) encoder.polyModulusDegree);
            long[] b = RandomSample.sampleUniform(0, encoder.plainModulus, (int) encoder.polyModulusDegree);
            runEncodeMul(a, b);
        }
    }

    // 编码后的乘法，说具有同态性质的，注意这里的乘法是 point-wise mul
    public void runEncodeMul(long[] a, long[] b) {
        assert a.length == b.length;
        long[] prod = new long[a.length];

        RingLweZp64 ringLweZp64 = new RingLweZp64(EnvType.STANDARD_JDK, encoder.plainModulus);
        // 这里一定要用 Zp 下的乘法实现，因为在目前的 Poly 的乘法结果是处理在了 [0, Q-1]
        for (int i = 0; i < a.length; i++) {
            prod[i] = ringLweZp64.mul(a[i], b[i]);
        }

        Plaintext ap = encoder.encode(a);
        Plaintext bp = encoder.encode(b);
        Plaintext apMulBpNTT = new Plaintext(ap.poly.mul(bp.poly, encoder.nttContext));
        Plaintext apMulBpNaive = new Plaintext(ap.poly.mul(bp.poly, encoder.plainModulus));

        long[] decodeProd = encoder.decode(apMulBpNTT);
        long[] decodeProd2 = encoder.decode(apMulBpNaive);

        assert Arrays.equals(prod, decodeProd);
        assert Arrays.equals(prod, decodeProd2);
    }


    public void runEncodeDecode(long[] values) {
        Plaintext plain = encoder.encode(values);
        long[] decode = encoder.decode(plain);
        assert Arrays.equals(decode, values);
    }

}
