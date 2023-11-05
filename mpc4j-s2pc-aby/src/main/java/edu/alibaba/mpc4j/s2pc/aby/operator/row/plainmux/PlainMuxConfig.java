package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainmux;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainmux.PlainMuxFactory.PlainMuxType;

/**
 * Plain mux config.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public interface PlainMuxConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PlainMuxType getPtoType();

    /**
     * Gets the zl.
     *
     * @return zl.
     */
    Zl getZl();
}
