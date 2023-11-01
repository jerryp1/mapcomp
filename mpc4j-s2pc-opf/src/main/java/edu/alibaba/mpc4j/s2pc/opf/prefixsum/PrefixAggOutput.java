package edu.alibaba.mpc4j.s2pc.opf.prefixsum;

import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.util.Vector;

/**
 * Prefix agg output
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public class PrefixAggOutput {
    /**
     * secret shares of grouping fields.
     */
    private Vector<byte[]> groupings;
    /**
     * secret shares of aggregation fields.
     */
    private SquareZlVector aggs;

    public PrefixAggOutput(Vector<byte[]> groupings, SquareZlVector aggs) {
        this.groupings = groupings;
        this.aggs = aggs;
    }

    public Vector<byte[]> getGroupings() {
        return groupings;
    }

    public SquareZlVector getAggs() {
        return aggs;
    }
}
