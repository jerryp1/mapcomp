package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.PrefixMaxFactory.PrefixMaxTypes;

/**
 * Prefix max config.
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public interface PrefixMaxConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PrefixMaxTypes getPtoType();

    /**
     * Zl
     */
    Zl getZl();

    /**
     * whether need shuffle result before output.
     */
    boolean needShuffle();
}
