package edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j;

/**
 * Helper class for various static byte sizes
 *
 * @author Steven K Fisher <swiftcryptollc@gmail.com>
 */
public final class KyberParams {

    public final static int MATH_TWO = 2;

    public final static int MATH_THREE = 3;

    public final static int MATH_FOUR = 4;
    public final static int MATH_EIGHT = 8;
    /**
     * 多项式长度（short）256
     */
    public final static int PARAMS_N = 256;

    /**
     * 多项式计算乘法时使用的参数
     */
    public final static int PARAMS_NTT_NUM = 128;

    public final static int PARAMS_Q = 3329;

    public final static int PARAMS_QINV = 62209;

    /**
     * 多项式长度（Byte）384 = 128 * 3
     */
    public final static int POLY_BYTES = 384;

    /**
     * 决定噪声中hash值的长度(K=2)。
     */
    public final static int ETA_512 = 3;

    /**
     * 决定噪声中hash值的长度(K=3 或 4)。
     */
    public final static int ETA_768_1024 = 2;

    /**
     * 噪声/随机数种子的长度、进行加密的消息的长度。
     */
    public final static int SYM_BYTES = 32;

    /**
     * K = 2或3时，压缩后的多项式长度的时候。
     */
    public final static int POLY_COMPRESSED_BYTES_768 = 128;

    /**
     * K = 4时，压缩后的多项式长度的时候。
     */
    public final static int POLY_COMPRESSED_BYTES_1024 = 160;

    /**
     * K = 2时，多项式向量压缩后的长度
     */
    public final static int POLY_VECTOR_COMPRESSED_BYTES_512 = 2 * 320;

    /**
     * K = 3时，多项式向量压缩后的长度
     */
    public final static int POLY_VECTOR_COMPRESSED_BYTES_768 = 3 * 320;

    /**
     * K = 4时，多项式向量压缩后的长度
     */
    public final static int POLY_VECTOR_COMPRESSED_BYTES_1024 = 4 * 352;

    /**
     * 适用于K=2时，公钥的长度 = 2 * 384。
     */
    public final static int POLY_VECTOR_BYTES_512 = 2 * POLY_BYTES;

    /**
     * 适用于K=2时，打包后的公钥长度 = 2 * 384 + 32。加了一个seed。
     */
    public final static int INDCPA_PK_BYTES_512 = POLY_VECTOR_BYTES_512 + SYM_BYTES;

    /**
     * 适用于K=3时，公钥的长度 = 3 * 384。
     */
    public final static int POLY_VECTOR_BYTES_768 = 3 * POLY_BYTES;

    /**
     * 适用于K=3时，打包后的公钥长度 = 3 * 384 + 32。加了一个seed。
     */
    public final static int INDCPA_PK_BYTES_768 = POLY_VECTOR_BYTES_768 + SYM_BYTES;

    /**
     * 适用于K=4时，公钥的长度 = 4 * 384。
     */
    public final static int POLY_VECTOR_BYTES_1024 = 4 * POLY_BYTES;

    /**
     * 适用于K=4时，打包后的公钥长度 = 4 * 384 + 32。加了一个seed。
     */
    public final static int INDCPA_PK_BYTES_1024 = POLY_VECTOR_BYTES_1024 + SYM_BYTES;
}
