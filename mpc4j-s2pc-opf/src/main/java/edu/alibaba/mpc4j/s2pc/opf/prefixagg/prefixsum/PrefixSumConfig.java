package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum.PrefixSumFactory.PrefixSumTypes;

/**
 * Prefix Sum Config.
 *
 * @author Li Peng
 * @date 2023/10/30
 */
public interface PrefixSumConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PrefixSumTypes getPtoType();

    /**
     * Zl
     */
    Zl getZl();

    /**
     * whether need shuffle result before output.
     */
    boolean needShuffle();
}
