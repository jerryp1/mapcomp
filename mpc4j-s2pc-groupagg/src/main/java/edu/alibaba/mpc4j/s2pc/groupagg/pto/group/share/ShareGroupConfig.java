package edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share.ShareGroupFactory.ShareGroupType;

/**
 * group aggregation config.
 *
 */
public interface ShareGroupConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ShareGroupType getPtoType();
}
