package edu.alibaba.mpc4j.s2pc.aby.hamming;

/**
 * 汉明距离协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/11/22
 */
public class HammingFactory {
    /**
     * 私有构造函数
     */
    private HammingFactory() {
        // empty
    }

    /**
     * 协议类型
     */
    public enum HammingType {
        /**
         * 半诚实安全BCP13协议
         */
        BCP13_SEMI_HOSNT,
        /**
         * 恶意安全BCP13协议
         */
        BCP13_MALICIOUS,
    }
}
