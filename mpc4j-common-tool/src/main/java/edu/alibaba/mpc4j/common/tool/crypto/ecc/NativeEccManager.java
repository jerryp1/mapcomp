package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;

/**
 * Openssl的Ecc实现管理器。
 *
 * @author Weiran Liu
 * @date 2022/8/24
 */
public class NativeEccManager {
    /**
     * 当前椭圆曲线类型
     */
    static EccFactory.EccType currentEccType;

    /**
     * 私有构造函数
     */
    private NativeEccManager() {
        // empty
    }
}
