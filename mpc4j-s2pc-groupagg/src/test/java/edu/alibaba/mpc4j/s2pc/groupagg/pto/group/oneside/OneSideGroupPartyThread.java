package edu.alibaba.mpc4j.s2pc.groupagg.pto.group.oneside;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.GroupTypes.AggTypes;

/**
 * Group aggregation Party Thread.
 *
 */
public class OneSideGroupPartyThread extends Thread {
    /**
     * the party
     */
    public OneSideGroupParty party;
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
    public BitVector groupFlag;
    /**
     * result
     */
    public SquareZ2Vector[][] res;

    OneSideGroupPartyThread(OneSideGroupParty party, SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags, AggTypes[] aggTypes, BitVector groupFlag) {
        this.party = party;
        this.xiArrays = xiArrays;
        this.validFlags = validFlags;
        this.aggTypes = aggTypes;
        this.groupFlag = groupFlag;
    }

    public SquareZ2Vector[][] getGroupRes() {
        return res;
    }

    @Override
    public void run() {
        try {
            party.init(xiArrays.length, xiArrays[0][0].getNum(), xiArrays[0].length);
            res = party.groupAgg(xiArrays, validFlags, aggTypes, groupFlag);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
