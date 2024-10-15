package edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.GroupTypes.AggTypes;

/**
 * Group aggregation Party Thread.
 *
 */
public class ShareGroupPartyThread extends Thread {
    /**
     * the party
     */
    public ShareGroupParty party;
    /**
     * aggregation attributes
     */
    public SquareZ2Vector[][] xiArrays;
    /**
     * valid flags
     */
    public SquareZ2Vector[] validFlags;
    /**
     * max or min
     */
    public AggTypes[] aggTypes;
    /**
     * group flag
     */
    public SquareZ2Vector groupFlag;
    /**
     * result
     */
    public SquareZ2Vector[][] res;
    /**
     * the flag indicating whether the current tuple is a valid group result
     */
    public SquareZ2Vector resFlag;

    ShareGroupPartyThread(ShareGroupParty party, SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags, AggTypes[] aggTypes, SquareZ2Vector groupFlag) {
        this.party = party;
        this.xiArrays = xiArrays;
        this.validFlags = validFlags;
        this.aggTypes = aggTypes;
        this.groupFlag = groupFlag;
    }

    public SquareZ2Vector[][] getGroupRes() {
        return res;
    }

    public SquareZ2Vector getResFlag() {
        return resFlag;
    }

    @Override
    public void run() {
        try {
            party.init(xiArrays.length, xiArrays[0][0].getNum(), xiArrays[0].length);
            res = party.groupAgg(xiArrays, validFlags, aggTypes, groupFlag);
            resFlag = party.getFlag(groupFlag);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
