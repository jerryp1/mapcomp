package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * plain private equality test config.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public interface PlainPeqtConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PlainPeqtFactory.PlainPeqtType getPtoType();
}
