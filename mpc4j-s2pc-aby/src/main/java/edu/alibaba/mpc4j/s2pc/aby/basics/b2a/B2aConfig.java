package edu.alibaba.mpc4j.s2pc.aby.basics.b2a;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory.B2aTypes;

/**
 * B2a Config.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
public interface B2aConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    B2aTypes getPtoType();

    /**
     * Get the zl.
     *
     * @return the zl.
     */
    Zl getZl();
}
