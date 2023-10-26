package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory.ShuffleTypes;

/**
 * Shuffle config.
 *
 * @author Li Peng
 * @date 2023/10/11
 */
public interface ShuffleConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    ShuffleTypes getPtoType();

    /**
     * Get the zl of plaintext.
     *
     * @return the zl.
     */
    Zl getZl();

    /**
     * Whether the shuffle is reversed (un-shuffle).
     *
     * @return whether the shuffle is reversed (un-shuffle)
     */
    boolean isReverse();
}
