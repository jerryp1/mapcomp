package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory.Z2MuxType;

/**
 * Z2 mux config.
 *
 * @author Feng Han
 * @date 2023/11/28
 */
public interface Z2MuxConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    Z2MuxType getPtoType();
}
