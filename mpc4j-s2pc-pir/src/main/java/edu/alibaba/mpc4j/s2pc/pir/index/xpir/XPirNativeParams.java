package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

/**
 * XPIR协议本地参数。
 *
 * @author Liqiang Peng
 * @date 2022/9/1
 */
public class XPirNativeParams {

    /**
     * 单例模式
     */
    private XPirNativeParams() {
        // empty
    }

    /**
     * 生成加密方案参数。
     *
     * @param modulusDegree 多项式阶。
     * @param plainModulus  明文模数。
     * @return 加密方案参数。
     */
    static native byte[] genEncryptionParameters(int modulusDegree, long plainModulus);
}
