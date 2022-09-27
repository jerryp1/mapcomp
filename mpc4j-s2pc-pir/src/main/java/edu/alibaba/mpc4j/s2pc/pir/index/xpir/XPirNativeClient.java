package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import java.util.ArrayList;
import java.util.List;

/**
 * XPIR协议本地客户端。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class XPirNativeClient {

    /**
     * 单例模式
     */
    private XPirNativeClient() {
        // empty
    }

    /**
     * 客户端加密检索值。
     *
     * @param encryptionParams 加密方案参数。
     * @param publicKey        公钥。
     * @param secretKey        私钥。
     * @param message          明文检索值。
     * @return 密文检索值。
     */
    static native ArrayList<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey,
                                                  int[] message);

    /**
     * 客户端解密查询结果。
     *
     * @param encryptionParams  加密方案参数。
     * @param secretKey         私钥。
     * @param response          检索结果密文。
     * @param dimension         维度。
     * @return 查询结果。
     */
    static native long[] decodeReply(byte[] encryptionParams, byte[] secretKey, ArrayList<byte[]> response, int dimension);

    /**
     * 客户端生成公私钥对。
     *
     * @param encryptionParams 加密方案参数。
     * @return 公私钥对。
     */
    static native List<byte[]> keyGeneration(byte[] encryptionParams);
}
