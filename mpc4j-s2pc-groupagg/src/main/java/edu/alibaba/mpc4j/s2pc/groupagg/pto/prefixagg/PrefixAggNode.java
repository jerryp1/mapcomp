package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg;

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
     * length used specifically in bool share.
     */
    private final int l;

    public PrefixAggNode(BigInteger aggShare, boolean groupIndicator) {
        this.aggShare = aggShare;
        this.groupIndicator = groupIndicator;
        l = 0;
    }

    public PrefixAggNode(BigInteger aggShare, boolean groupIndicator, int l) {
        this.aggShare = aggShare;
        this.groupIndicator = groupIndicator;
        this.l = l;
    }

    public BigInteger getAggShare() {
        return aggShare;
    }

    public boolean isGroupIndicator() {
        return groupIndicator;
    }

    public int getL() {
        return l;
    }
}
