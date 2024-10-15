package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory.ShuffleTypes;

/**
 * Shuffle config.
 *
 */
public interface ShuffleConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ShuffleTypes getPtoType();

    /**
     * Whether the shuffle is reversed (un-shuffle).
     *
     * @return whether the shuffle is reversed (un-shuffle)
     */
    boolean isReverse();
}
