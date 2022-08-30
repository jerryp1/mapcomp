package edu.alibaba.mpc4j.common.tool.crypto.kyber;

public final class KyberParams {

    public final static int paramsN = 256;
    public final static int paramsQ = 3329;
    public final static int paramsQinv = 62209;
    //384 = 128 * 3 ——是一个多项式中的系数的长度，一个多项式包含384个short，但是将多项式转为byte的时候，byte的长度也为？384？
    public final static int paramsPolyBytes = 384;
    public final static int paramsETAK512 = 3;
    public final static int paramsETAK768K1024 = 2;
    public final static int paramsSymBytes = 32;
    //压缩后的多项式长度（K=2或3）的时候。
    public final static int paramsPolyCompressedBytesK768 = 128;
    //压缩后的多项式长度（4）的时候。
    public final static int paramsPolyCompressedBytesK1024 = 160;
    //K = 2时，多项式向量压缩后的长度
    public final static int paramsPolyvecCompressedBytesK512 = 2 * 320;
    //K = 3时，多项式向量压缩后的长度
    public final static int paramsPolyvecCompressedBytesK768 = 3 * 320;
    //K = 4时，多项式向量压缩后的长度
    public final static int paramsPolyvecCompressedBytesK1024 = 4 * 352;
    //适用于K=2时，公钥的长度 = 2 * 384
    public final static int paramsPolyvecBytesK512 = 2 * paramsPolyBytes;
    //适用于K=2时，打包后的公钥长度 = 2 * 384 + 32。加了一个seed。
    public final static int  paramsIndcpaPublicKeyBytesK512 = paramsPolyvecBytesK512 + paramsSymBytes;
    //适用于K=3时，公钥的长度 = 3 * 384
    public final static int paramsPolyvecBytesK768 = 3 * paramsPolyBytes;
    //适用于K=3时，打包后的公钥长度 = 3 * 384 + 32。加了一个seed。
    public final static int paramsIndcpaPublicKeyBytesK768 = paramsPolyvecBytesK768 + paramsSymBytes;
    //适用于K=4时，公钥的长度 = 4 * 384
    public final static int paramsPolyvecBytesK1024 = 4 * paramsPolyBytes;
    //适用于K=4时，打包后的公钥长度 = 4 * 384 + 32。加了一个seed。
    public final static int paramsIndcpaPublicKeyBytesK1024 = paramsPolyvecBytesK1024 + paramsSymBytes;


}
