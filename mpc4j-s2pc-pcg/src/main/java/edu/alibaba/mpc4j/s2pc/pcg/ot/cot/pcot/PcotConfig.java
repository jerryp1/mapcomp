package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.PcotFactory.PcotType;

/**
 * 预计算关联不经意传输（Precompute Correlated Oblivious Transfer，PCOT）协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public interface PcotConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    PcotType getPtoType();
}
