package edu.alibaba.mpc4j.common.circuit.prefixsum;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Prefix-sum operation interface
 *
 * @author Li Peng
 * @date 2023/10/27
 */
public interface PrefixSumOp {
    /**
     * Updates the tuples in current level of tree.
     *
     * @param inputIndexes  the indexes of input tuples.
     * @param outputIndexes the indexes of output tuples.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void updateCurrentLevel(int[] inputIndexes, int[] outputIndexes) throws MpcAbortException;
}
