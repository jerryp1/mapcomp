package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.NcLotFactory.NcLotType;

/**
 * NC-LOT协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/0816
 */
public interface NcLotConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     * @return 协议类型。
     */
    NcLotType getPtoType();

    /**
     * 一次可生成的最大OT数量。
     * @return 最大OT数量。
     */
    int maxAllowNum();

//    /**
//     * 判断是否支持该比特长度输入。
//     *
//     * @param bitLength 输入的比特长度。
//     * @return 是否支持输入的比特长度。
//     */
//    boolean validL(int bitLength);
}
