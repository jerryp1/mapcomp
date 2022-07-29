package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * 根关联不经意传输（Root Correlated Oblivious Transfer，RCOT）协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
public interface RcotConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    RcotFactory.RcotType getPtoType();
}
