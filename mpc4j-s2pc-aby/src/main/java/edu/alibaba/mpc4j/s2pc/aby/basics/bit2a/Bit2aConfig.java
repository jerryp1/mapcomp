package edu.alibaba.mpc4j.s2pc.aby.basics.bit2a;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory.Bit2aTypes;

/**
 * Bit2a Config.
 *
 * @author Li Peng
 * @date 2023/10/11
 */
public interface Bit2aConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    Bit2aTypes getPtoType();

    /**
     * Get the zl.
     *
     * @return the zl.
     */
    Zl getZl();
}
