package edu.alibaba.mpc4j.s2pc.opf.prefixsum;

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

    public PrefixSumNode(byte[] groupShare, BigInteger sumShare) {
        this.groupShare = groupShare;
        this.sumShare = sumShare;
    }

    public byte[] getGroupShare() {
        return groupShare;
    }

    public BigInteger getSumShare() {
        return sumShare;
    }
}
