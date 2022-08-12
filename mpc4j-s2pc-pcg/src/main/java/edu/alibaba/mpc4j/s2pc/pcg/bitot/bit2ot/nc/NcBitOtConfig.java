package edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.nc;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * NC-BitOT协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/08/11
 */
public interface NcBitOtConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    NcBitOtFactory.NcBitOtType getPtoType();

    /**
     * 返回底层协议最大数量。
     *
     * @return 底层协议最大数量。
     */
    int maxAllowNum();

}
