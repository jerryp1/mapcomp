package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggFactory.GroupAggTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;

/**
 * Group aggregation config.
 *
 * @author Li Peng
 * @date 2023/11/3
 */
public interface GroupAggConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    GroupAggTypes getPtoType();

    /**
     * Whether the shuffle is reversed (un-shuffle).
     *
     * @return whether the shuffle is reversed (un-shuffle)
     */
    boolean isReverse();

    /**
     * get zl.
     */
    Zl getZl();

    /**
     * get type of aggregation.
     *
     * @return type of aggregation.
     */
    PrefixAggTypes getAggType();
}
