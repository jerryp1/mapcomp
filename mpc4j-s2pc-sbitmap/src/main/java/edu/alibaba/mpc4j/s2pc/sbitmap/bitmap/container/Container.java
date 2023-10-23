package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Container.
 *
 * @author Li Peng
 * @date 2023/8/20
 */
public interface Container {
    /**
     * Transfer container to SquareZ2Vector.
     *
     * @return result.
     */
    SquareZ2Vector toSecureVector();

    /**
     * Get BitVector of container.
     *
     * @return result.
     */
    BitVector getBitVector();

    /**
     * Copy the container.
     *
     * @return the copied container.
     */
    Container copy();

    /**
     * Return whether the container is in plain state.
     *
     * @return whether the container is in plain state.
     */
    boolean isPlain();
}
