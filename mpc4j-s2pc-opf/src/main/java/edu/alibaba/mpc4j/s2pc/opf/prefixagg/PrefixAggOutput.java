package edu.alibaba.mpc4j.s2pc.opf.prefixagg;

import com.google.common.base.Preconditions;
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
     * number
     */
    private final int num;

    public PrefixAggOutput(Vector<byte[]> groupings, SquareZlVector aggs) {
        Preconditions.checkArgument(groupings.size() == aggs.getNum(), "size of input not match");
        this.groupings = groupings;
        this.aggs = aggs;
        this.num = aggs.getNum();
    }

    public Vector<byte[]> getGroupings() {
        return groupings;
    }

    public SquareZlVector getAggs() {
        return aggs;
    }


    public void setAggs(SquareZlVector aggs) {
        Preconditions.checkArgument(aggs.getNum()== num, "size of input not correct");
        this.aggs = aggs;
    }

    public int getNum() {
        return num;
    }
}
