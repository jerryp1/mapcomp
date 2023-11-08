package edu.alibaba.mpc4j.s2pc.opf.prefixagg;

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
     * (secret shares) of grouping fields.
     */
    private final Vector<byte[]> groupings;
    /**
     * secret shares of aggregation fields.
     */
    private SquareZlVector aggs;
    /**
     * plain group.
     */
    private final String[] plainGroups;

    public PrefixAggOutput(Vector<byte[]> groupings, SquareZlVector aggs) {
        this.groupings = groupings;
        this.aggs = aggs;
        plainGroups = null;
    }

//    public PrefixAggOutput(String[] plainGroups, SquareZlVector aggs) {
//        this.plainGroups = plainGroups;
//        this.aggs = aggs;
////        groupings = null;
//    }

    public Vector<byte[]> getGroupings() {
        return groupings;
    }

    public SquareZlVector getAggs() {
        return aggs;
    }

    public String[] getPlainGroups() {
        return plainGroups;
    }

    public void setAggs(SquareZlVector aggs) {
        this.aggs = aggs;
    }
}
