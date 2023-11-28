package edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory.PlainMuxType;

/**
 * Plain mux config.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public interface PlainPayloadMuxConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PlainMuxType getPtoType();
}
