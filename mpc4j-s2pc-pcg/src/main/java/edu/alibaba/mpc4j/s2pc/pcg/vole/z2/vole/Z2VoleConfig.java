package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.vole;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * Z2-VOLE协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
public interface Z2VoleConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    Z2VoleFactory.Z2VoleType getPtoType();

    /**
     * 返回是否为根协议。根协议是指自身即可实现协议功能，不需要调用其他协议。
     *
     * @return 是否为根协议。
     */
    boolean isRoot();
}
