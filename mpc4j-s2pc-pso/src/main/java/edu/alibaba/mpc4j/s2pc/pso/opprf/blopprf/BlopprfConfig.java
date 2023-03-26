package edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Batched l-bit-input OPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public interface BlopprfConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return tye type.
     */
    BlopprfFactory.BlopprfType getPtoType();
}
