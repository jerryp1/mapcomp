package edu.alibaba.mpc4j.s2pc.opf.prefixsum;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.util.Vector;

/**
 * Zl Max party thread.
 *
 * @author Li Peng
 * @date 2023/10/30
 */
class PrefixSumPartyThread extends Thread {
    /**
     * the sender
     */
    private final PrefixSumParty prefixSumParty;
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

    PrefixSumPartyThread(PrefixSumParty prefixSumParty, Vector<byte[]> groups, SquareZlVector aggs) {
        this.prefixSumParty = prefixSumParty;
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
            prefixSumParty.init(l, num);
            shareZ = prefixSumParty.sum(groups, aggs);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
