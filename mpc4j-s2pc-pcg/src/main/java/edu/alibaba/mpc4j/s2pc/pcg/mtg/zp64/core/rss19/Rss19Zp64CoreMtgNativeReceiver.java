package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19;

/**
 * RSS19-核zp64三元组生成协议本地接收方。
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public class Rss19Zp64CoreMtgNativeReceiver {

    /**
     * 接收方密文计算函数。
     *
     * @param encryptionParams 加密方案参数。
     * @param cipher1          密文1。
     * @param cipher2          密文2。
     * @param plain1           明文1。
     * @param plain2           明文2。
     * @param randomMask       随机掩码。
     * @return 密文计算结果。
     */
    static native byte[] computeResponse(byte[] encryptionParams, byte[] cipher1, byte[] cipher2, long[] plain1,
                                         long[] plain2, long[] randomMask);
}
