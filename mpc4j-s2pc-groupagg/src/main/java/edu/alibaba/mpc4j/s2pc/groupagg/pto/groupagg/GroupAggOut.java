package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg;

import java.math.BigInteger;

/**
 * Group aggregation output, including the plain group attribute and result aggregation attribute.
 *
 */
public class GroupAggOut {
    /**
     * group field.
     */
    private final String[] groupField;
    /**
     * aggregation result.
     */
    private final BigInteger[] aggregationResult;

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
