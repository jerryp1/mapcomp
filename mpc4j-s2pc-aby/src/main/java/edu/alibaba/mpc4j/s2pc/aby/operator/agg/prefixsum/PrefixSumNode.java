package edu.alibaba.mpc4j.s2pc.aby.operator.agg.prefixsum;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixNode;

import java.math.BigInteger;

/**
 * Prefix sum node.
 *
 * @author Li Peng
 * @date 2023/10/30
 */
public class PrefixSumNode implements PrefixNode {
    /**
     * Secret shares of grouping fields.
     */
    private final byte[] groupShare;
    /**
     * Secret shares of sum fields.
     */
    private final BigInteger sumShare;
    /**
     * Secret share of indicator of group.
     */
    private final boolean groupIndicator;

    public PrefixSumNode(byte[] groupShare, BigInteger sumShare, boolean groupIndicator) {
        this.groupShare = groupShare;
        this.sumShare = sumShare;
        this.groupIndicator = groupIndicator;
    }

    public byte[] getGroupShare() {
        return groupShare;
    }

    public BigInteger getSumShare() {
        return sumShare;
    }

    public boolean getGroupIndicator() {
        return groupIndicator;
    }
}
