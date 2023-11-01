package edu.alibaba.mpc4j.common.circuit.prefixsum;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Arrays;

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
    default void updateCurrentLevel(int[] inputIndexes, int[] outputIndexes) throws MpcAbortException {
        PrefixSumNode[] currentNodes = getPrefixSumNodes();
        PrefixSumNode[] inputs1 = Arrays.stream(inputIndexes).mapToObj(i -> currentNodes[i]).toArray(PrefixSumNode[]::new);
        PrefixSumNode[] inputs2 = Arrays.stream(outputIndexes).mapToObj(i -> currentNodes[i]).toArray(PrefixSumNode[]::new);
        operateAndUpdate(inputs1, inputs2, outputIndexes);
    }

    /**
     * Get prefix sum nodes.
     *
     * @return prefix sum nodes.
     */
    PrefixSumNode[] getPrefixSumNodes();

    /**
     * Conduct prefix sum operations on inputs.
     *
     * @param x             input1.
     * @param y             input2.
     * @param outputIndexes indexes of outputs.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void operateAndUpdate(PrefixSumNode[] x, PrefixSumNode[] y, int[] outputIndexes) throws MpcAbortException;
}
