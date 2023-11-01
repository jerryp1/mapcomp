package edu.alibaba.mpc4j.common.circuit.prefixsum;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Prefix-sum tree interface.
 *
 * @author Li Peng
 * @date 2023/10/27
 */
public interface PrefixSumTree {

    /**
     * Prefix computation using a prefix network.
     *
     * @param l the number of input nodes.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void addPrefix(int l) throws MpcAbortException;
}
