package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.util.Vector;

/**
 * Prefix agg output
 *
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
     * secret shares of aggregation fields.
     */
    private SquareZ2Vector[] aggsBinary;
    /**
     * secret shares of group indicator
     */
    private final SquareZ2Vector indicator;
    /**
     * number
     */
    private final int num;

    public PrefixAggOutput(Vector<byte[]> groupings, SquareZlVector aggs, SquareZ2Vector indicator) {
        Preconditions.checkArgument(groupings.size() == aggs.getNum(), "size of input not match");
        this.groupings = groupings;
        this.aggs = aggs;
        this.indicator = indicator;
        this.num = aggs.getNum();
    }

    public PrefixAggOutput(Vector<byte[]> groupings, SquareZ2Vector[] aggs, SquareZ2Vector indicator) {
        Preconditions.checkArgument(groupings.size() == aggs[0].getNum(), "size of input not match");
        this.groupings = groupings;
        this.aggsBinary = aggs;
        this.indicator = indicator;
        this.num = aggs[0].getNum();
    }

    public Vector<byte[]> getGroupings() {
        return groupings;
    }

    public SquareZlVector getAggs() {
        return aggs;
    }

    public SquareZ2Vector[] getAggsBinary() {
        return aggsBinary;
    }

    public SquareZ2Vector getIndicator() {
        return indicator;
    }

    public void setAggs(SquareZlVector aggs) {
        Preconditions.checkArgument(aggs.getNum() == num, "size of input not correct");
        this.aggs = aggs;
    }

    public int getNum() {
        return num;
    }
}
