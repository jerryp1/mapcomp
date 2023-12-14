package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;

/**
 * Prefix agg Config.
 *
 * @author Li Peng
 * @date 2023/10/30
 */
public interface PrefixAggConfig extends MultiPartyPtoConfig {
    /**
     * Zl
     */
    Zl getZl();

    /**
     * whether need shuffle result before output.
     */
    boolean needShuffle();

    /**
     * Gets the prefix type.
     *
     * @return prefix type.
     */
    PrefixAggTypes getPrefixType();
}
