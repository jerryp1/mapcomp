package edu.alibaba.mpc4j.s2pc.pso.mppsi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

public interface MppsiConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    MppsiFactory.MppsiType getPtoType();
    /**
     * 返回协议的参与方人数。
     *
     * @return 参与方人数。
     */
    public int getPartyNum();
    /**
     * 返回攻击者最多可以共谋的参与方人数。
     *
     * @return 最多可以共谋的参与方人数。
     */
    public int getMaxColludedPartyNum();
}
