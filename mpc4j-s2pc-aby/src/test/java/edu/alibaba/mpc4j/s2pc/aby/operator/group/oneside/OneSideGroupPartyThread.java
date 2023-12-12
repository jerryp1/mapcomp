package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.GroupTypes.AggTypes;

public class OneSideGroupPartyThread extends Thread {
    public OneSideGroupParty party;
    public SquareZ2Vector[][] xiArrays;
    public SquareZ2Vector[] validFlags;
    public AggTypes[] aggTypes;
    public BitVector groupFlag;
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
            party.init(xiArrays.length, xiArrays[0][0].getNum(), xiArrays.length);
            res = party.groupAgg(xiArrays, validFlags, aggTypes, groupFlag);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

}
