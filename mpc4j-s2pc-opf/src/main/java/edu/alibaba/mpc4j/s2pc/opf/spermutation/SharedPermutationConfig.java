package edu.alibaba.mpc4j.s2pc.opf.spermutation;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory.SharedPermutationTypes;

/**
 * Shared permutation config.
 *
 * @author Li Peng
 * @date 2023/10/25
 */
public interface SharedPermutationConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    SharedPermutationTypes getPtoType();

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
