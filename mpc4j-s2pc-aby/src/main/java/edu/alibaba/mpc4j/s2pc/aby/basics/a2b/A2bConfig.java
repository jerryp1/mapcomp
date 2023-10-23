package edu.alibaba.mpc4j.s2pc.aby.basics.a2b;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory.A2bTypes;

/**
 * A2b Config.
 *
 * @author Li Peng
 * @date 2023/10/20
 */
public interface A2bConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    A2bTypes getPtoType();

    /**
     * Get the zl.
     *
     * @return the zl.
     */
    Zl getZl();
}
