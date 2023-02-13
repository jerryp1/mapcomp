package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * DPPRF config interface.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public interface DpprfConfig extends MultiPartyPtoConfig {
    /**
     * Get the protocol type.
     *
     * @return the protocol type.
     */
    DpprfFactory.DpprfType getPtoType();
}
