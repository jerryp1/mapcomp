package edu.alibaba.mpc4j.s2pc.pso.cpsi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Circuit PSI config.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public interface CpsiConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return tye type.
     */
    CpsiFactory.CpsiType getPtoType();
}
