package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixsum;

import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixsum.PrefixSumFactory.PrefixSumTypes;

/**
 * Prefix Sum Config.
 *
 * @author Li Peng
 * @date 2023/10/30
 */
public interface PrefixSumConfig extends PrefixAggConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PrefixSumTypes getPtoType();
}
