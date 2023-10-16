package edu.alibaba.mpc4j.s2pc.aby.operator.psorter;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.PermutableSorterFactory.PermutableSorterTypes;

/**
 * @author Li Peng
 * @date 2023/10/11
 */
public interface PermutableSorterConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PermutableSorterTypes getPtoType();

    /**
     * Get the zl.
     *
     * @return the zl.
     */
    Zl getZl();
}
