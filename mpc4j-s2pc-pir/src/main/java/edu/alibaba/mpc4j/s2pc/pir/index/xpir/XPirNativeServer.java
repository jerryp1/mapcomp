package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import java.util.ArrayList;

/**
 * XPIR协议本地服务端。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class XPirNativeServer {

    /**
     * 单例模式
     */
    private XPirNativeServer() {
        // empty
    }

    /**
     * NTT转换。
     *
     * @param encryptionParams 加密方案参数。
     * @param plaintext        系数表示的多项式。
     * @return 点值表示的多项式。
     */
    static native ArrayList<byte[]> transformToNttForm(byte[] encryptionParams, ArrayList<long[]> plaintext);

    /**
     * 服务端密态下计算检索结果。
     *
     * @param encryptionParams 加密方案参数。
     * @param queryList        检索值密文。
     * @param plaintextList    数据库明文。
     * @param nvec             各维度长度。
     * @return 检索结果密文。
     */
    static native ArrayList<byte[]> generateReply(byte[] encryptionParams, ArrayList<byte[]> queryList,
                                                  ArrayList<byte[]> plaintextList, int[] nvec);
}
