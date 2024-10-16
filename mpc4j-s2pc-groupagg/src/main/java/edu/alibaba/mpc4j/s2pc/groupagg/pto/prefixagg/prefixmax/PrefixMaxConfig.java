package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax;

import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax.PrefixMaxFactory.PrefixMaxTypes;

/**
 * Prefix max config.
 *
 */
public interface PrefixMaxConfig extends PrefixAggConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PrefixMaxTypes getPtoType();
}
