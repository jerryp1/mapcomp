package edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * GF2K-DPPRF协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public interface Gf2kDpprfConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    Gf2kDpprfFactory.Gf2kDpprfType getPtoType();
}
