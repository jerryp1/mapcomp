package edu.alibaba.mpc4j.common.circuit.prefixsum;

/**
 * Abstract prefix adder operation.
 *
 * @author Li Peng
 * @date 2023/10/27
 */
public abstract class AbstractPrefixSumTree implements PrefixSumTree {
    /**
     * Prefix sum operation.
     */
    protected final PrefixSumOp prefixSumOp;

    public AbstractPrefixSumTree(PrefixSumOp prefixSumOp) {
        this.prefixSumOp = prefixSumOp;
    }
}
