package edu.alibaba.mpc4j.s2pc.pcg.btg;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BtgFactory.BtgType;

/**
 * BTG协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/07
 */
public interface BtgConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    BtgType getPtoType();

    /**
     * 返回底层协议最大数量。
     *
     * @return 底层协议最大数量。
     */
    int maxBaseNum();
}
