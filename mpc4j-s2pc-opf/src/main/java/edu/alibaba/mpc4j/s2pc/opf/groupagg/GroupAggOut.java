package edu.alibaba.mpc4j.s2pc.opf.groupagg;

import java.math.BigInteger;

/**
 * @author Li Peng
 * @date 2023/11/3
 */
public class GroupAggOut {
    /**
     * group field.
     */
    private String[] groupField;
    /**
     * aggregation result.
     */
    private BigInteger[] aggregationResult;

    public GroupAggOut(String[] groupField, BigInteger[] aggregationResult) {
        this.groupField = groupField;
        this.aggregationResult = aggregationResult;
    }

    public String[] getGroupField() {
        return groupField;
    }

    public BigInteger[] getAggregationResult() {
        return aggregationResult;
    }
}
