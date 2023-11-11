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
     * Secret shares of group indicator (group_i == group_{i+1}), such as 11101110.
     */
    private final boolean groupIndicator;
    /**
     * The flag of intersection.
     */
    private boolean intersFlag;

    public PrefixAggNode(BigInteger aggShare, boolean groupIndicator) {
        this.aggShare = aggShare;
        this.groupIndicator = groupIndicator;
        intersFlag = false;
    }

    public BigInteger getAggShare() {
        return aggShare;
    }

    public boolean isGroupIndicator() {
        return groupIndicator;
    }
}
