package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory.AggTypes;

public interface OneSideGroupParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxL   max l.
     * @param maxNum max num.
     * @param maxBitNum max input bit number
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL, int maxNum, int maxBitNum) throws MpcAbortException;

    /**
     * get the flags representing which rows store the group aggregation results
     *
     * @param groupFlag if the i-th row is the last one in its group, groupFlag[i] = true, otherwise, groupFlag[i] = false
     * @return the party's output.
     */
    default boolean[] getResPosFlag(boolean[] groupFlag){
        boolean[] res = new boolean[groupFlag.length];
        int index = groupFlag.length - 1;
        while (index >= 0) {
            int curr = index - 1;
            while (curr >= 0 && (!groupFlag[curr])) {
                curr--;
            }if(curr == -1){
                res[0] = true;
            }else{
                res[OneSideGroupUtils.rightMostChildOfLeftSubTreeOfCommonAncestor(curr + 1, index)] = true;
            }
            index = curr;
        }
        return res;
    }

    /**
     * Executes the protocol.
     *
     * @param xiArrays the arrays of share xi.
     * @param validFlags whether the corresponding row is valid
     * @param aggTypes max or min
     * @param groupFlag if the i-th row is the last one in its group, groupFlag[i] = true, otherwise, groupFlag[i] = false
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector[] groupAgg(SquareZ2Vector[] xiArrays, SquareZ2Vector validFlags, AggTypes aggTypes, BitVector groupFlag) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param xiArrays the arrays of share xi.
     * @param validFlags whether the corresponding row is valid
     * @param aggTypes max or min
     * @param groupFlag if the i-th row is the last one in its group, groupFlag[i] = true, otherwise, groupFlag[i] = false
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector[][] groupAgg(SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags, AggTypes[] aggTypes, boolean[] groupFlag) throws MpcAbortException;
}