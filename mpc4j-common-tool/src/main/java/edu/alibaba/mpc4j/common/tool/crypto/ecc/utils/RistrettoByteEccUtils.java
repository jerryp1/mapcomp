package edu.alibaba.mpc4j.common.tool.crypto.ecc.utils;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe.CafeRistrettoCompressedPoint;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe.CafeScalar;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.math.BigInteger;
import java.util.Locale;

/**
 * Ristretto椭圆曲线工具类。
 *
 * @author Weiran Liu
 * @date 2022/11/11
 */
public class RistrettoByteEccUtils {
    /**
     * 点的字节长度
     */
    public static final int POINT_BYTES = CafeRistrettoCompressedPoint.BYTE_SIZE;
    /**
     * 幂指数的字节长度
     */
    public static final int SCALAR_BYTES = CafeScalar.BYTE_SIZE;
    /**
     * l = 2^{252} + 27742317777372353535851937790883648493
     */
    public static final BigInteger N = BigInteger.ONE.shiftLeft(252)
        .add(new BigInteger("27742317777372353535851937790883648493"));

    /**
     * 无穷远点：X = 0，小端表示
     */
    public static final byte[] POINT_INFINITY = new byte[]{
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    };

    /**
     * 将大整数表示的幂指数转换为字节数组。
     *
     * @param k 幂指数。
     * @return 转换结果。
     */
    public static byte[] toByteK(BigInteger k) {
        assert BigIntegerUtils.greaterOrEqual(k, BigInteger.ZERO) && BigIntegerUtils.less(k, N) :
            "k must be in range [0, " + N.toString().toUpperCase(Locale.ROOT) + "): " + k;
        byte[] byteK = BigIntegerUtils.nonNegBigIntegerToByteArray(k, SCALAR_BYTES);
        BytesUtils.innerReverseByteArray(byteK);
        return byteK;
    }
}
