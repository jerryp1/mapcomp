package edu.alibaba.mpc4j.common.tool.crypto.kyber;

import java.util.Arrays;

/**
 * Polynomial and Polynomial Vector Utility class
 *
 * @author Steven K Fisher <swiftcryptollc@gmail.com>
 */
public final class Poly {
    /**
     * Apply the conditional subtraction of Q (KyberParams) to each coefficient of a polynomial
     *
     * @param r r是多项式的系数构成的数组，每一个系数都计算conditionalSubQ
     * @return 返回值是多项式系数
     */
    public static short[] polyConditionalSubQ(short[] r) {
        for (int i = 0; i < KyberParams.PARAMS_N; i++) {
            r[i] = ByteOps.conditionalSubQ(r[i]);
        }
        return r;
    }

    /**
     * Applies the conditional subtraction of Q (KyberParams) to each coefficient of each element
     * of a vector of polynomials.
     * @param r 输入为一组多项式向量的系数
     * @return 返回值为所有多项式向量系数大于等于Q的值经过减Q的处理
     */
    public static short[][] polyVectorCSubQ(short[][] r) {
        for (int i = 0; i < r.length; i++) {
            r[i] = Poly.polyConditionalSubQ(r[i]);
        }
        return r;
    }

    /**
     * Add two polynomials
     *
     * @param polyA 多项式A的系数组
     * @param polyB 多项式B的系数组
     * @return 返回值是多项式A的系数加多项式B的系数
     */
    public static short[] polyAdd(short[] polyA, short[] polyB) {
        for (int i = 0; i < KyberParams.PARAMS_N; i++) {
            polyA[i] = (short) (polyA[i] + polyB[i]);
        }
        return polyA;
    }

    /**
     * Add two polynomial vectors
     *
     * @param polyA 一组多项式向量A
     * @param polyB 一组多项式向量B
     * @return 多项式向量系数相加的结果
     */
    public static short[][] polyVectorAdd(short[][] polyA, short[][] polyB) {
        for (int i = 0; i < polyA.length; i++) {
            polyA[i] = Poly.polyAdd(polyA[i], polyB[i]);
        }
        return polyA;
    }

    /**
     * Subtract two polynomials
     *
     * @param polyA 多项式A的系数组
     * @param polyB 多项式B的系数组
     * @return 返回值是多项式A的系数减多项式B的系数
     */
    public static short[] polySub(short[] polyA, short[] polyB) {
        for (int i = 0; i < KyberParams.PARAMS_N; i++) {
            polyA[i] = (short) (polyA[i] - polyB[i]);
        }
        return polyA;
    }

    /**
     * Performs an in-place conversion of all coefficients of a polynomial from
     * the normal domain to the Montgomery domain
     *
     * @param polyR 将多项式中的所有系数都传递至Montgomery域。
     * @return 返回值同样是多项式系数
     */
    public static short[] polyToMont(short[] polyR) {
        for (int i = 0; i < KyberParams.PARAMS_N; i++) {
            polyR[i] = ByteOps.montgomeryReduce((long) (polyR[i] * 1353));
        }
        return polyR;
    }

    /**
     * Apply Barrett reduction to all coefficients of this polynomial
     *
     * @param r 对于所有的系数都执行Barrett reduction
     * @return 返回值同样是多项式系数
     */
    public static short[] polyReduce(short[] r) {
        for (int i = 0; i < KyberParams.PARAMS_N; i++) {
            r[i] = ByteOps.barrettReduce(r[i]);
        }
        return r;
    }

    /**
     * Applies Barrett reduction to each coefficient of each element of a vector
     * of polynomials.
     *
     * @param r  对于所有的系数都执行Barrett reduction
     * @return 返回值是多项式向量
     */
    public static short[][] polyVectorReduce(short[][] r) {
        for (int i = 0; i < r.length; i++) {
            r[i] = Poly.polyReduce(r[i]);
        }
        return r;
    }

    /**
     * Serialize a polynomial in to an array of bytes
     * 将一个多项式转换为byte数组————每两个short对应3个byte
     * @param a 输入为short数组
     * @return 返回值是长度为384的byte数组
     */
    public static byte[] polyToBytes(short[] a) {
        int t0, t1;
        byte[] r = new byte[KyberParams.POLY_BYTES];
        a = Poly.polyConditionalSubQ(a);
        for (int i = 0; i < KyberParams.PARAMS_N / 2; i++) {
            t0 = ((int) (a[2 * i] & 0xFFFF));
            t1 = ((int) (a[2 * i + 1]) & 0xFFFF);
            r[3 * i] = (byte) (t0);
            r[3 * i + 1] = (byte) ((int) (t0 >> 8) | (int) (t1 << 4));
            r[3 * i + 2] = (byte) (t1 >> 4);
        }
        return r;
    }

    /**
     * Serialize a polynomial vector to a byte array
     * 将多项式向量转换为byte数组
     * @param polyA 多项式向量
     * @return 返回值是长度为 K * 384
     */
    public static byte[] polyVectorToBytes(short[][] polyA) {
        int paramsK = polyA.length;
        byte[] r = new byte[paramsK * KyberParams.POLY_BYTES];
        for (int i = 0; i < paramsK; i++) {
            byte[] byteA = polyToBytes(polyA[i]);
            System.arraycopy(byteA, 0, r, i * KyberParams.POLY_BYTES, byteA.length);
        }
        return r;
    }

    /**
     * De-serialize a byte array into a polynomial
     * 将一个byte数组转换为多项式（short形式表现）————每3个byte对应两个short
     * @param a 输入为byte数组
     * @return 输出为short，长度可以修改为256原来为384
     */
    public static short[] polyFromBytes(byte[] a) {
        short[] r = new short[KyberParams.PARAMS_N];
        for (int i = 0; i < KyberParams.PARAMS_N / 2; i++) {
            r[2 * i] = (short) ((((a[3 * i] & 0xFF)) | ((a[3 * i + 1] & 0xFF) << 8)) & 0xFFF);
            r[2 * i + 1] = (short) ((((a[3 * i + 1] & 0xFF) >> 4) | ((a[3 * i + 2] & 0xFF) << 4)) & 0xFFF);
        }
        return r;
    }

    /**
     * Deserialize a byte array into a polynomial vector
     * 将byte数组转为多项式向量
     * @param polyA 输入的byte数组
     * @return 多项式向量
     */
    public static short[][] polyVectorFromBytes(byte[] polyA) {
        int paramsK = polyA.length/KyberParams.POLY_BYTES;
        short[][] r = new short[paramsK][KyberParams.POLY_BYTES];
        for (int i = 0; i < paramsK; i++) {
            int start = (i * KyberParams.POLY_BYTES);
            int end = (i + 1) * KyberParams.POLY_BYTES;
            r[i] = Poly.polyFromBytes(Arrays.copyOfRange(polyA, start, end));
        }
        return r;
    }

    /**
     * Convert a 32-byte message to a polynomial
     * 将256/8 = 32 的byte数组转换为多项式（256）short，对于每一个bit，如果为0，则输出为0，如果为1，则输出为1665。
     * @param msg
     * @return
     */
    public static short[] polyFromData(byte[] msg) {
        short[] r = new short[KyberParams.PARAMS_N];
        short mask;
        for (int i = 0; i < KyberParams.PARAMS_N / 8; i++) {
            for (int j = 0; j < 8; j++) {
                //这里乘-1，是为了将1转换为-1，以在做and运算是变成-1的补码，即16个1，再去计算and
                mask = (short) (-1 * (short) (((msg[i] & 0xFF) >> j) & 1));
                r[8 * i + j] = (short) (mask & (short) ((KyberParams.PARAMS_Q + 1) / 2));
            }
        }
        return r;
    }

    /**
     * Convert a polynomial to a 32-byte message
     * 将多项式（256个short）转换为32个byte数组
     * @param a 多项式系数
     * @return byte数组，msg
     */
    public static byte[] polyToMsg(short[] a) {
        byte[] msg = new byte[KyberParams.SYM_BYTES];
        int t;
        a = polyConditionalSubQ(a);
        for (int i = 0; i < KyberParams.PARAMS_N / 8; i++) {
            msg[i] = 0;
            for (int j = 0; j < 8; j++) {
                // 计算 msg【i】 = (a * 2 + Q/2 ) / Q & 1
                t = (int) ((((((int) (a[8 * i + j])) << 1) + (KyberParams.PARAMS_Q / 2)) / KyberParams.PARAMS_Q) & 1);
                msg[i] = (byte) (msg[i] | (t << j));
            }
        }
        return msg;
    }

    /**
     * Create a new Polynomial Vector
     * 生成一个新的多项式
     * @param paramsK 输入为安全系数，决定了代码中的多项式系数是如何组织的
     * @return 返回值是一个新的多项式向量
     */
    public static short[][] generateNewPolyVector(int paramsK) {
        short[][] pv = new short[paramsK][KyberParams.POLY_BYTES];
        return pv;
    }
    /**
     * Computes an in-place negacyclic number-theoretic transform (NTT) of a polynomial
     * 将多项式系数转换至NTT域。
     * Input is assumed normal order
     *
     * Output is assumed bit-revered order
     *
     * @param r 多项式系数
     * @return
     */
    public static short[] polyNTT(short[] r) {
        return Ntt.ntt(r);
    }

    /**
     * Applies forward number-theoretic transforms (NTT) to all elements of a
     * vector of polynomial
     *
     * @param r 多项式向量
     * @return NTT域上多项式向量
     */
    public static short[][] polyVectorNTT(short[][] r) {
        for (int i = 0; i < r.length; i++) {
            r[i] = Poly.polyNTT(r[i]);
        }
        return r;
    }

    /**
     * Computes an in-place inverse of a negacyclic number-theoretic transform (NTT) of a polynomial
     * 将NTT域上的系数转换至多项式系数
     * Input is assumed bit-revered order
     *
     * Output is assumed normal order
     *
     * @param r NTT数域上的系数
     * @return 多项式系数
     */
    public static short[] polyInvNTTMont(short[] r) {
        return Ntt.invNtt(r);
    }

    /**
     * Applies the inverse number-theoretic transform (NTT) to all elements of a
     * vector of polynomials and multiplies by Montgomery factor 2^16
     *
     * @param r NTT域多项式向量
     * @return 多项式向量
     */
    public static short[][] polyVectorInvNTTMont(short[][] r) {
        for (int i = 0; i < r.length; i++) {
            r[i] = Poly.polyInvNTTMont(r[i]);
        }
        return r;
    }

    /**
     * Multiply two polynomials in the number-theoretic transform (NTT) domain
     * NTT数域上的乘法
     * @param polyA 乘法因子A
     * @param polyB 乘法因子B
     * @return 乘积在NTT域上的系数
     */
    public static short[] polyBaseMulMont(short[] polyA, short[] polyB) {
        for (int i = 0; i < KyberParams.PARAMS_N / 4; i++) {
            short[] rx = Ntt.baseMultiplier(
                    polyA[4 * i], polyA[4 * i + 1],
                    polyB[4 * i], polyB[4 * i + 1],
                    (short) Ntt.nttZetas[64 + i]
            );
            short[] ry = Ntt.baseMultiplier(
                    polyA[4 * i + 2], polyA[4 * i + 3],
                    polyB[4 * i + 2], polyB[4 * i + 3],
                    (short) (-1 * Ntt.nttZetas[64 + i])
            );
            polyA[4 * i] = rx[0];
            polyA[4 * i + 1] = rx[1];
            polyA[4 * i + 2] = ry[0];
            polyA[4 * i + 3] = ry[1];
        }
        return polyA;
    }

    /**
     * Pointwise-multiplies elements of the given polynomial-vectors ,
     * accumulates the results , and then multiplies by 2^-16
     *
     * @param polyA 多项式向量乘法因子A
     * @param polyB 多项式向量乘法因子B
     * @return
     */
    public static short[] polyVectorPointWiseAccMont(short[][] polyA, short[][] polyB) {
        short[] r = Poly.polyBaseMulMont(polyA[0], polyB[0]);
        for (int i = 1; i < polyA.length; i++) {
            short[] t = Poly.polyBaseMulMont(polyA[i], polyB[i]);
            r = Poly.polyAdd(r, t);
        }
        return polyReduce(r);
    }

    /**
     * Performs lossy compression and serialization of a polynomial
     * 压缩一个多项式（只有密文）
     * @param polyA 压缩的多项式系数
     * @param paramsK 安全系数,此处安全系数不能省略，因为不知道256个多项式系数中有多少位是有用的。
     * @return 压缩后的多项式系数
     */
    public static byte[] compressPoly(short[] polyA, int paramsK) {
        byte[] t = new byte[8];
        polyA = Poly.polyConditionalSubQ(polyA);
        int rr = 0;
        byte[] r;
        switch (paramsK) {
            case 2:
            case 3:
                r = new byte[KyberParams.POLY_COMPRESSED_BYTES_768];
                for (int i = 0; i < KyberParams.PARAMS_N / 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        t[j] = (byte) (((((polyA[8 * i + j]) << 4) + (KyberParams.PARAMS_Q / 2)) / (KyberParams.PARAMS_Q)) & 15);
                    }
                    r[rr] = (byte) (t[0] | (t[1] << 4));
                    r[rr + 1] = (byte) (t[2] | (t[3] << 4));
                    r[rr + 2] = (byte) (t[4] | (t[5] << 4));
                    r[rr + 3] = (byte) (t[6] | (t[7] << 4));
                    rr = rr + 4;
                }
                break;
            default:
                r = new byte[KyberParams.POLY_COMPRESSED_BYTES_1024];
                for (int i = 0; i < KyberParams.PARAMS_N / 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        t[j] = (byte) (((((polyA[8 * i + j]) << 5) + (KyberParams.PARAMS_Q / 2)) / (KyberParams.PARAMS_Q)) & 31);
                    }
                    r[rr] = (byte) ((t[0]) | (t[1] << 5));
                    r[rr + 1] = (byte) ((t[1] >> 3) | (t[2] << 2) | (t[3] << 7));
                    r[rr + 2] = (byte) ((t[3] >> 1) | (t[4] << 4));
                    r[rr + 3] = (byte) ((t[4] >> 4) | (t[5] << 1) | (t[6] << 6));
                    r[rr + 4] = (byte) ((t[6] >> 2) | (t[7] << 3));
                    rr = rr + 5;
                }
        }

        return r;
    }
    /**
     * Perform a lossly compression and serialization of a vector of polynomials
     * 将多项式向量（只有密文）压缩
     * @param a
     * @param paramsK
     * @return
     */
    public static byte[] compressPolyVector(short[][] a, int paramsK) {
        Poly.polyVectorCSubQ(a);
        int rr = 0;
        byte[] r;
        long[] t;
        switch (paramsK) {
            case 2:
                r = new byte[KyberParams.POLY_VECTOR_COMPRESSED_BYTES_512];
                break;
            case 3:
                r = new byte[KyberParams.POLY_VECTOR_COMPRESSED_BYTES_768];
                break;
            default:
                r = new byte[KyberParams.POLY_VECTOR_COMPRESSED_BYTES_1024];
        }

        switch (paramsK) {
            case 2:
            case 3:
                t = new long[4];
                for (int i = 0; i < paramsK; i++) {
                    for (int j = 0; j < KyberParams.PARAMS_N / 4; j++) {
                        for (int k = 0; k < 4; k++) {
                            t[k] = ((long) (((long) ((long) (a[i][4 * j + k]) << 10) + (long) (KyberParams.PARAMS_Q / 2)) / (long) (KyberParams.PARAMS_Q)) & 0x3ff);
                        }
                        r[rr] = (byte) (t[0]);
                        r[rr + 1] = (byte) ((t[0] >> 8) | (t[1] << 2));
                        r[rr + 2] = (byte) ((t[1] >> 6) | (t[2] << 4));
                        r[rr + 3] = (byte) ((t[2] >> 4) | (t[3] << 6));
                        r[rr + 4] = (byte) ((t[3] >> 2));
                        rr = rr + 5;
                    }
                }
                break;
            default:
                t = new long[8];
                for (int i = 0; i < paramsK; i++) {
                    for (int j = 0; j < KyberParams.PARAMS_N / 8; j++) {
                        for (int k = 0; k < 8; k++) {
                            t[k] = ((long) (((long) ((long) (a[i][8 * j + k]) << 11) + (long) (KyberParams.PARAMS_Q / 2)) / (long) (KyberParams.PARAMS_Q)) & 0x7ff);
                        }
                        r[rr] = (byte) ((t[0]));
                        r[rr + 1] = (byte) ((t[0] >> 8) | (t[1] << 3));
                        r[rr + 2] = (byte) ((t[1] >> 5) | (t[2] << 6));
                        r[rr + 3] = (byte) ((t[2] >> 2));
                        r[rr + 4] = (byte) ((t[2] >> 10) | (t[3] << 1));
                        r[rr + 5] = (byte) ((t[3] >> 7) | (t[4] << 4));
                        r[rr + 6] = (byte) ((t[4] >> 4) | (t[5] << 7));
                        r[rr + 7] = (byte) ((t[5] >> 1));
                        r[rr + 8] = (byte) ((t[5] >> 9) | (t[6] << 2));
                        r[rr + 9] = (byte) ((t[6] >> 6) | (t[7] << 5));
                        r[rr + 10] = (byte) ((t[7] >> 3));
                        rr = rr + 11;
                    }
                }
        }
        return r;
    }
    /**
     * De-serialize and decompress a polynomial
     * 解密压缩后的多项式
     * Compression is lossy so the resulting polynomial will not match the
     * original polynomial
     * 压缩是有损的，因此会与原多项式不同
     * @param a 压缩后的byte
     * @param paramsK 多项式系数
     * @return
     */
    public static short[] decompressPoly(byte[] a, int paramsK) {
        short[] r = new short[KyberParams.PARAMS_N];
        switch (paramsK) {
            case 2:
            case 3:
                for (int i = 0; i < KyberParams.PARAMS_N / 2; i++) {
                    r[2 * i] = (short) ((((int) (a[i] & 15) * KyberParams.PARAMS_Q) + 8) >> 4);
                    r[2 * i + 1] = (short) (((((int) (a[i] & 0xFF) >> 4) * KyberParams.PARAMS_Q) + 8) >> 4);
                }
                break;
            default:
                int aa = 0;
                long[] t = new long[8];
                for (int i = 0; i < KyberParams.PARAMS_N / 8; i++) {
                    t[0] = (long) ((int) (a[aa] & 0xFF));
                    t[1] = (long) ((byte) (((int) (a[aa] & 0xFF) >> 5)) | (byte) ((int) (a[aa + 1] & 0xFF) << 3)) & 0xFF;
                    t[2] = (long) ((int) (a[aa + 1] & 0xFF) >> 2) & 0xFF;
                    t[3] = (long) ((byte) (((int) (a[aa + 1] & 0xFF) >> 7)) | (byte) ((int) (a[aa + 2] & 0xFF) << 1)) & 0xFF;
                    t[4] = (long) ((byte) (((int) (a[aa + 2] & 0xFF) >> 4)) | (byte) ((int) (a[aa + 3] & 0xFF) << 4)) & 0xFF;
                    t[5] = (long) ((int) (a[aa + 3] & 0xFF) >> 1) & 0xFF;
                    t[6] = (long) ((byte) (((int) (a[aa + 3] & 0xFF) >> 6)) | (byte) ((int) (a[aa + 4] & 0xFF) << 2)) & 0xFF;
                    t[7] = ((long) ((int) (a[aa + 4] & 0xFF) >> 3)) & 0xFF;
                    aa = aa + 5;
                    for (int j = 0; j < 8; j++) {
                        r[8 * i + j] = (short) ((((long) (t[j] & 31) * (KyberParams.PARAMS_Q)) + 16) >> 5);
                    }
                }
        }
        return r;
    }

    /**
     * De-serialize and decompress a vector of polynomials
     * 解密压缩后的多项式向量
     * Since the compress is lossy, the results will not be exactly the same as
     * the original vector of polynomials
     * 压缩是有损的，因此会与原多项式向量不同
     * @param a 压缩后的向量
     * @param paramsK 安全参数K
     * @return 多项式向量
     */
    public static short[][] decompressPolyVector(byte[] a, int paramsK) {
        short[][] r = new short[paramsK][KyberParams.POLY_BYTES];
        int aa = 0;
        int[] t;
        switch (paramsK) {
            case 2:
            case 3:
                t = new int[4]; // has to be unsigned..
                for (int i = 0; i < paramsK; i++) {
                    for (int j = 0; j < (KyberParams.PARAMS_N / 4); j++) {
                        t[0] = ((a[aa] & 0xFF) | ((a[aa + 1] & 0xFF) << 8));
                        t[1] = ((a[aa + 1] & 0xFF) >> 2) | ((a[aa + 2] & 0xFF) << 6);
                        t[2] = ((a[aa + 2] & 0xFF) >> 4) | ((a[aa + 3] & 0xFF) << 4);
                        t[3] = ((a[aa + 3] & 0xFF) >> 6) | ((a[aa + 4] & 0xFF) << 2);
                        aa = aa + 5;
                        for (int k = 0; k < 4; k++) {
                            r[i][4 * j + k] = (short) (((long) (t[k] & 0x3FF) * (long) (KyberParams.PARAMS_Q) + 512) >> 10);
                        }
                    }
                }
                break;
            default:
                t = new int[8]; // has to be unsigned..
                for (int i = 0; i < paramsK; i++) {
                    for (int j = 0; j < (KyberParams.PARAMS_N / 8); j++) {
                        t[0] = ((a[aa] & 0xff) | ((a[aa + 1] & 0xff) << 8));
                        t[1] = (((a[aa + 1] & 0xff) >> 3) | ((a[aa + 2] & 0xff) << 5));
                        t[2] = (((a[aa + 2] & 0xff) >> 6) | ((a[aa + 3] & 0xff) << 2) | ((a[aa + 4] & 0xff) << 10));
                        t[3] = (((a[aa + 4] & 0xff) >> 1) | ((a[aa + 5] & 0xff) << 7));
                        t[4] = (((a[aa + 5] & 0xff) >> 4) | ((a[aa + 6] & 0xff) << 4));
                        t[5] = (((a[aa + 6] & 0xff) >> 7) | ((a[aa + 7] & 0xff) << 1) | ((a[aa + 8] & 0xff) << 9));
                        t[6] = (((a[aa + 8] & 0xff) >> 2) | ((a[aa + 9] & 0xff) << 6));
                        t[7] = (((a[aa + 9] & 0xff) >> 5) | ((a[aa + 10] & 0xff) << 3));
                        aa = aa + 11;
                        for (int k = 0; k < 8; k++) {
                            r[i][8 * j + k] = (short) (((long) (t[k] & 0x7FF) * (long) (KyberParams.PARAMS_Q) + 1024) >> 11);
                        }
                    }
                }
        }
        return r;
    }

    /**
     * Generate a deterministic noise polynomial from a seed and nonce
     * The polynomial output will be close to a centered binomial distribution
     * 通过seed和nonce生成符合二项分布的噪声多项式
     * @param seed 随机数种子
     * @param nonce 噪声
     * @param paramsK 安全参数
     * @return 生成的噪声
     */
    public static short[] getNoisePoly(byte[] seed, byte nonce, int paramsK) {
        int l;
        byte[] p;
        switch (paramsK) {
            case 2:
                l = KyberParams.ETA_512 * KyberParams.PARAMS_N / 4;
                break;
            default:
                l = KyberParams.ETA_768_1024 * KyberParams.PARAMS_N / 4;
        }

        p = Indcpa.generatePrfByteArray(l, seed, nonce);
        return ByteOps.generateCbdPoly(p, paramsK);
    }

}
