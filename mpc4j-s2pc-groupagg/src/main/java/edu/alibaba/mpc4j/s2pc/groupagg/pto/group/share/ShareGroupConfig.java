package edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share.ShareGroupFactory.ShareGroupType;

/**
 * group aggregation config.
 *
 * @author Feng Han
 * @date 2023/11/28
 */
public interface ShareGroupConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ShareGroupType getPtoType();
}
