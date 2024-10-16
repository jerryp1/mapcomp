package edu.alibaba.mpc4j.s2pc.opf.permutation;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory.PermutationTypes;

/**
 * Permutation config.
 *
 */
public interface PermutationConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PermutationTypes getPtoType();

    /**
     * Get the zl of plaintext.
     *
     * @return the zl.
     */
    Zl getZl();

    /**
     * Get the indicator of whether the permutation is reversed.
     *
     * @return the indicator of whether the permutation is reversed.
     */
    boolean isReverse();
}
