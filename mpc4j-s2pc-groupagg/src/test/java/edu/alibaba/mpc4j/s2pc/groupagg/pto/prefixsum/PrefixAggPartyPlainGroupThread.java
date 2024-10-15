package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixsum;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggParty;

/**
 * Prefix aggregation party thread.
 *
 */
class PrefixAggPartyPlainGroupThread extends Thread {
    /**
     * the sender
     */
    private final PrefixAggParty prefixAggParty;
    /**
     * x
     */
    private final String[] groups;
    /**
     * num
     */
    private final SquareZ2Vector[] aggs;
    /**
     * l
     */
    private final int l;
    /**
     * num
     */
    private final int num;
    /**
     * z
     */
    private PrefixAggOutput shareZ;

    PrefixAggPartyPlainGroupThread(PrefixAggParty prefixAggParty, String[] groups, SquareZ2Vector[] aggs) {
        this.prefixAggParty = prefixAggParty;
        this.groups = groups;
        this.aggs = aggs;
        this.l = aggs.length;
        this.num = aggs[0].getNum();
    }

    PrefixAggOutput getShareZ() {
        return shareZ;
    }

    @Override
    public void run() {
        try {
            prefixAggParty.init(l, num);
            shareZ = prefixAggParty.agg(groups, aggs);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
