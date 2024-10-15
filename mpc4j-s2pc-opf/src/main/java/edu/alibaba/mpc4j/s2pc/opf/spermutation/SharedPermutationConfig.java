package edu.alibaba.mpc4j.s2pc.opf.spermutation;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory.SharedPermutationTypes;

/**
 * Shared permutation config.
 *
 */
public interface SharedPermutationConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    SharedPermutationTypes getPtoType();

    /**
     * Get the indicator of whether the permutation is reversed.
     *
     * @return the indicator of whether the permutation is reversed.
     */
    boolean isReverse();
}
