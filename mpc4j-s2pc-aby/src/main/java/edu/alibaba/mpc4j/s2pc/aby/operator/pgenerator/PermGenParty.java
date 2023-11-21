package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * permutation generator Party.
 *
 * @author Feng Han
 * @date 2023/11/03
 */
public interface PermGenParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxNum max num.
     * @param maxBitNum max input bit number
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxNum, int maxBitNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param xiArrays the arrays of share xi.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZlVector sort(SquareZ2Vector[] xiArrays) throws MpcAbortException;
}
