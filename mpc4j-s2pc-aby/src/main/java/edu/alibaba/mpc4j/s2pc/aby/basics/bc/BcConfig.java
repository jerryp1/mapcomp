package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Boolean circuit config.
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
public interface BcConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    BcFactory.BcType getPtoType();
}
