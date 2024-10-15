package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixxor;

import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixxor.PrefixXorFactory.PrefixXorTypes;

/**
 * Prefix xor Config.
 *
 */
public interface PrefixXorConfig extends PrefixAggConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PrefixXorTypes getPtoType();
}
