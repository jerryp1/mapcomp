package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.checkerframework.checker.units.qual.A;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/7/12
 */
public class NttContextTest {


    @Test
    public void testNtt() {
        NttContext ntt = new NttContext(4, 73);
        long[] fwd = ntt.ntt(new long[]{0, 1, 4, 5}, ntt.rootOfUnityArray);
        assert Arrays.equals(fwd, new long[]{10, 34, 71, 31});
    }


    @Test
    public void testInvNtt() {
        NttContext ntt = new NttContext(4, 73);

        long[] coeffs = new long[]{10, 34, 71, 31};
        for (int i = 0; i < coeffs.length; i++) {
            coeffs[i] = ntt.ringLweZp64.mul(coeffs[i], -18);
        }
        long[] fwd = ntt.ntt(coeffs, ntt.rootOfUnityInvArray);
        assert Arrays.equals(fwd, new long[]{0, 1, 4, 5});
    }


    @Test
    public void testNttPolyMul() {
        NttContext ntt = new NttContext(4, 17);

        long[] a = new long[]{0, 0, 2, 0};
        long[] b = new long[]{0, 0, 2, 0};

        a = ntt.nttForward(a);
        b = ntt.nttForward(b);

        long[] ab = new long[a.length];
        for (int i = 0; i < a.length; i++) {
            ab[i] = ntt.ringLweZp64.mul(a[i], b[i]);
        }
        ab = ntt.nttInverse(ab);

        assert Arrays.equals(ab, new long[]{13, 0, 0, 0});
    }



}
