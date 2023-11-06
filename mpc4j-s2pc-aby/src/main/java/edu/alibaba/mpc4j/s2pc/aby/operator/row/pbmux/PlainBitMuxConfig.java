package edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory.PlainBitMuxType;

/**
 * Plain bit mux config.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public interface PlainBitMuxConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PlainBitMuxType getPtoType();

    /**
     * Gets the zl.
     *
     * @return zl.
     */
    Zl getZl();
}
