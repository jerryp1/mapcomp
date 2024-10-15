package edu.alibaba.mpc4j.s2pc.groupagg.pto.group.oneside;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.oneside.OneSideGroupFactory.OneSideGroupType;

/**
 * group aggregation config.
 *
 */
public interface OneSideGroupConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    OneSideGroupType getPtoType();
}
