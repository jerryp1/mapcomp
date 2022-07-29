package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * ZP_VOLE协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/06/08
 */
public interface ZpVoleConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    ZpVoleFactory.ZpVoleType getPtoType();
}
