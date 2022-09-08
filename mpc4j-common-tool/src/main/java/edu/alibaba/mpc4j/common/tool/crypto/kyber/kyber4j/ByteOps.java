package edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j;


import java.util.Arrays;

/**
 * Utility class for byte operations
 *
 * @author Steven K Fisher <swiftcryptollc@gmail.com>
 */
public final class ByteOps {

    /**
     * Returns a 32-bit unsigned integer as a long from byte x
     *
     * @param x 输入为byte数组，读取前四个byte转为long
     * @return 输出为一个long
     */
    public static long convertByteTo32BitUnsignedInt(byte[] x) {
        long r = (x[0] & 0xFF);
        r = r | ((long) (x[1] & 0xFF) << 8);
        r = r | ((long) (x[2] & 0xFF) << 16);
        r = r | ((long) (x[3] & 0xFF) << 24);
        return r;
    }

    /**
     * Returns a 24-bit unsigned integer as a long from byte x
     *
     * @param x 输入为byte数组，读取前三个转为long
     * @return 输出为一个long
     */
    public static long convertByteTo24BitUnsignedInt(byte[] x) {
        long r = (x[0] & 0xFF);
        r = r | ((long) (x[1] & 0xFF) << 8);
        r = r | ((long) (x[2] & 0xFF) << 16);
        return r;
    }

    /**
     * Generate a polynomial with coefficients distributed according to a
     * centered binomial distribution with parameter eta, given an array of
     * uniformly random bytes.
     *
     * @param buf     生成的种子
     * @param paramsK 选择的安全系数
     * @return 符合二项分布的噪声系数
     */
    public static short[] generateCbdPoly(byte[] buf, int paramsK) {
        long t, d; // both unsigned
        int a, b;
        short[] r = new short[KyberParams.PARAMS_N];
        switch (paramsK) {
            case 2:
                for (int i = 0; i < KyberParams.PARAMS_N / KyberParams.MATH_FOUR; i++) {
                    t = ByteOps.convertByteTo24BitUnsignedInt(Arrays.copyOfRange(buf, (3 * i), buf.length));
                    d = t & 0x00249249;
                    d = d + ((t >> 1) & 0x00249249);
                    d = d + ((t >> 2) & 0x00249249);
                    for (int j = 0; j < KyberParams.MATH_FOUR; j++) {
                        a = (short) ((d >> (6 * j)) & 0x7);
                        b = (short) ((d >> (6 * j + KyberParams.ETA_512)) & 0x7);
                        r[4 * i + j] = (short) (a - b);
                    }
                }
                break;
            default:
                for (int i = 0; i < KyberParams.PARAMS_N / KyberParams.MATH_EIGHT; i++) {
                    t = ByteOps.convertByteTo32BitUnsignedInt(Arrays.copyOfRange(buf, (4 * i), buf.length));
                    d = t & 0x55555555;
                    d = d + ((t >> 1) & 0x55555555);
                    for (int j = 0; j < KyberParams.MATH_EIGHT; j++) {
                        a = (short) ((d >> (4 * j)) & 0x3);
                        b = (short) ((d >> (4 * j + KyberParams.ETA_768_1024)) & 0x3);
                        r[8 * i + j] = (short) (a - b);
                    }
                }
        }
        return r;
    }

    /**
     * Computes a Montgomery reduction given a 32-Bit Integer
     *
     * @param a 对输入的long进行规约
     * @return 输出为short
     */
    public static short montgomeryReduce(long a) {
        short u = (short) (a * KyberParams.PARAMS_QINV);
        int t = (u * KyberParams.PARAMS_Q);
        t = (int) (a - t);
        t >>= 16;
        return (short) t;
    }

    /**
     * Computes a Barrett reduction given a 16-Bit Integer
     *
     * @param a 输入为short值，同样也是为了规约
     * @return 返回值为规约后的值
     */
    public static short barrettReduce(short a) {
        short t;
        long shift = (((long) 1) << 26);
        short v = (short) ((shift + (KyberParams.PARAMS_Q / 2)) / KyberParams.PARAMS_Q);
        t = (short) ((v * a) >> 26);
        t = (short) (t * KyberParams.PARAMS_Q);
        return (short) (a - t);
    }

    /**
     * Conditionally subtract Q (from KyberParams) from a
     * 如果是大于等于Q（3329），那么是减Q，如果是小于Q那么则是不变（包括负数）
     *
     * @param a 输入的值
     * @return 模Q后的数
     */
    public static short conditionalSubQ(short a) {
        a = (short) (a - KyberParams.PARAMS_Q);
        a = (short) (a + (((int) a >> 15) & KyberParams.PARAMS_Q));
        return a;
    }
}
