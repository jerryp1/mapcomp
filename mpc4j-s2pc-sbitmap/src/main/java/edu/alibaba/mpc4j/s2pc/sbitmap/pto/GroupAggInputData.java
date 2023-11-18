package edu.alibaba.mpc4j.s2pc.sbitmap.pto;

import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * @author Li Peng
 * @date 2023/11/17
 */
public class GroupAggInputData {
    String[] groups;
    long[] aggs;
    SquareZ2Vector e;
    public GroupAggInputData(String[] groups, long[] aggs, SquareZ2Vector e) {
        this.groups = groups;
        this.aggs = aggs;
        this.e=e;
    }

    public String[] getGroups() {
        return groups;
    }

    public long[] getAggs() {
        return aggs;
    }

    public SquareZ2Vector getE() {
        return e;
    }
}
