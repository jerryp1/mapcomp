package edu.alibaba.mpc4j.s2pc.opf.prefixagg;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixNode;

import java.math.BigInteger;

/**
 * Prefix aggregation node in a prefix tree.
 *
 * @author Li Peng
 * @date 2023/10/30
 */
public class PrefixAggNode implements PrefixNode {
    /**
     * Secret shares of aggregation fields.
     */
    private final BigInteger aggShare;
    /**
     * Secret shares of group indicator
     */
    private final boolean groupIndicator;

    public PrefixAggNode(BigInteger aggShare, boolean groupIndicator) {
        this.aggShare = aggShare;
        this.groupIndicator = groupIndicator;
    }

    public BigInteger getAggShare() {
        return aggShare;
    }

    public boolean isGroupIndicator() {
        return groupIndicator;
    }
}