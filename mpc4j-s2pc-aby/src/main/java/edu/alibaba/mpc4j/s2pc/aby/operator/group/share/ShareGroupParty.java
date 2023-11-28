package edu.alibaba.mpc4j.s2pc.aby.operator.group.share;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.GroupFactory.AggTypes;

public interface ShareGroupParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param attrNum   attribute number.
     * @param maxNum    max num.
     * @param maxBitNum max input bit number
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int attrNum, int maxNum, int maxBitNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param xiArrays   the arrays of share xi.
     * @param validFlags whether the corresponding row is valid
     * @param aggTypes   max or min
     * @param groupFlag  if the i-th row is the first one in its group, groupFlag[i] = true, otherwise, groupFlag[i] = false
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default SquareZ2Vector[] groupAgg(SquareZ2Vector[] xiArrays, SquareZ2Vector validFlags, AggTypes aggTypes, SquareZ2Vector groupFlag) throws MpcAbortException{
        return groupAgg(new SquareZ2Vector[][]{xiArrays}, new SquareZ2Vector[]{validFlags}, new AggTypes[]{aggTypes}, groupFlag)[0];
    }

    /**
     * Executes the protocol.
     *
     * @param xiArrays   the arrays of share xi.
     * @param validFlags whether the corresponding row is valid
     * @param aggTypes   max or min
     * @param groupFlag  if the i-th row is the first one in its group, groupFlag[i] = true, otherwise, groupFlag[i] = false
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector[][] groupAgg(SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags, AggTypes[] aggTypes, SquareZ2Vector groupFlag) throws MpcAbortException;
}
