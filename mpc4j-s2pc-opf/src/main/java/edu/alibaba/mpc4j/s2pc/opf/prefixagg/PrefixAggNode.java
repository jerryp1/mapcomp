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
     * Secret shares of grouping fields.
     */
    private final byte[] groupShare;
    /**
     * Secret shares of aggregation fields.
     */
    private final BigInteger aggShare;

    public PrefixAggNode(byte[] groupShare, BigInteger aggShare) {
        this.groupShare = groupShare;
        this.aggShare = aggShare;
    }

    public byte[] getGroupShare() {
        return groupShare;
    }

    public BigInteger getAggShare() {
        return aggShare;
    }
}
