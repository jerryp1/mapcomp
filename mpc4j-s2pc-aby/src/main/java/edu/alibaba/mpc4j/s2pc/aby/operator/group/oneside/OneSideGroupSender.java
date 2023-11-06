package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory.AggTypes;

public interface OneSideGroupSender extends TwoPartyPto {
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
     * Executes the protocol.
     *
     * @param xiArrays the arrays of share xi.
     * @param validFlags whether the corresponding row is valid
     * @param aggTypes max or min
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector[] groupAgg(SquareZ2Vector[] xiArrays, SquareZ2Vector validFlags, AggTypes aggTypes) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param xiArrays the arrays of share xi.
     * @param validFlags whether the corresponding row is valid
     * @param aggTypes max or min
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector[][] groupAgg(SquareZ2Vector[][] xiArrays, SquareZ2Vector[] validFlags, AggTypes[] aggTypes) throws MpcAbortException;
}
