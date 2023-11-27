package edu.alibaba.mpc4j.s2pc.pcg.b2a;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTupleFactory.B2aTupleType;

/**
 * 布尔三元组生成协议配置项。
 *
 * @author Li Peng
 * @date 2023/11/21
 */
public interface B2aTupleConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    B2aTupleType getPtoType();

    /**
     * get zl
     */
    Zl getZl();
}
