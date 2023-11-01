package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggOutput;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggParty;

import java.util.Vector;

/**
 * Prefix aggregation party thread.
 *
 * @author Li Peng
 * @date 2023/10/30
 */
class PrefixAggPartyThread extends Thread {
    /**
     * the sender
     */
    private final PrefixAggParty prefixAggParty;
    /**
     * x
     */
    private final Vector<byte[]> groups;
    /**
     * num
     */
    private final SquareZlVector aggs;
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

    PrefixAggPartyThread(PrefixAggParty prefixAggParty, Vector<byte[]> groups, SquareZlVector aggs) {
        this.prefixAggParty = prefixAggParty;
        this.groups = groups;
        this.aggs = aggs;
        this.l = aggs.getZl().getL();
        this.num = groups.size();
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
