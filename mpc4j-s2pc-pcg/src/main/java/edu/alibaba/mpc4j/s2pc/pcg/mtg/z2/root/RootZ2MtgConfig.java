package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * 根布尔三元组生成协议信息。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public interface RootZ2MtgConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    RootZ2MtgFactory.RbtgType getPtoType();

    /**
     * 返回最大支持数量。
     *
     * @return 最大支持数量。
     */
    int maxAllowNum();
}
