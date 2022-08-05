package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.rvole;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * Z2-RVOLE协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
public interface Z2RvoleConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    Z2RvoleFactory.Z2RvoleType getPtoType();
}
