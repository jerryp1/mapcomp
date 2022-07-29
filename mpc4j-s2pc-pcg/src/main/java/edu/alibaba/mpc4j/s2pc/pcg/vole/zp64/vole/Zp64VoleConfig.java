package edu.alibaba.mpc4j.s2pc.pcg.vole.zp64.vole;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * ZP64_VOLE协议配置项
 *
 * @author Hanwen Feng
 * @date 2022/06/15
 */
public interface Zp64VoleConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    Zp64VoleFactory.Zp64VoleType getPtoType();
}
