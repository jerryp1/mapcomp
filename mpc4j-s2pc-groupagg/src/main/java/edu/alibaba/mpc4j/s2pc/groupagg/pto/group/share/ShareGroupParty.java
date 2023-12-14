package edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.GroupTypes.AggTypes;

/**
 * abstract group aggregation party for shared values and flag
 *
 * @author Feng Han
 * @date 2023/11/28
 */
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
     * get the valid flag for result, 1 for valid tuples and 0 for dummy tuples
     *
     * @param groupFlag if the i-th row is the first one in its group, groupFlag[i] = true, otherwise, groupFlag[i] = false
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector getFlag(SquareZ2Vector groupFlag) throws MpcAbortException;

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
    default SquareZ2Vector[] groupAgg(SquareZ2Vector[] xiArrays, SquareZ2Vector validFlags, AggTypes aggTypes, SquareZ2Vector groupFlag) throws MpcAbortException {
        return groupAgg(new SquareZ2Vector[][]{xiArrays}, validFlags == null ? null : new SquareZ2Vector[]{validFlags}, new AggTypes[]{aggTypes}, groupFlag)[0];
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
