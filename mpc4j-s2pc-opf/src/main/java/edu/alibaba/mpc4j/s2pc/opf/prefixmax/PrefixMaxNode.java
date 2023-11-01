package edu.alibaba.mpc4j.s2pc.opf.prefixmax;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixNode;

import java.math.BigInteger;

/**
 * Prefix max node.
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public class PrefixMaxNode implements PrefixNode {
    /**
     * Secret shares of grouping fields.
     */
    private final byte[] groupShare;
    /**
     * Secret shares of sum fields.
     */
    private final BigInteger sumShare;

    public PrefixMaxNode(byte[] groupShare, BigInteger sumShare) {
        this.groupShare = groupShare;
        this.sumShare = sumShare;
    }

    public byte[] getGroupShare() {
        return groupShare;
    }

    public BigInteger getMaxShare() {
        return sumShare;
    }
}
