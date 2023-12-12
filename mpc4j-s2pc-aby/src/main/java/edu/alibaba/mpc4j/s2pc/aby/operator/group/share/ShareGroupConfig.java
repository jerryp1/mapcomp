package edu.alibaba.mpc4j.s2pc.aby.operator.group.share;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.share.ShareGroupFactory.ShareGroupType;

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
