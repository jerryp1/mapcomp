package edu.alibaba.mpc4j.s2pc.opf.groupagg.mix;

import java.math.BigInteger;

/**
 * Prefix agg node.
 *
 * @author Li Peng
 * @date 2023/11/6
 */
public class MixPrefixAggNode {
    boolean indicator;
    BigInteger agg;

    public MixPrefixAggNode(BigInteger agg, boolean indicator) {
        this.agg = agg;
        this.indicator = indicator;
    }
}
