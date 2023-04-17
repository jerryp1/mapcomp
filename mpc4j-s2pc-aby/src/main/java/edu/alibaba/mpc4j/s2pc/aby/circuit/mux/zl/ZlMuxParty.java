package edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl.SquareShareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

/**
 * Zl mux party.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public interface ZlMuxParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param xi the binary share xi.
     * @param yi the arithmetic share yi.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareShareZlVector mux(SquareShareZ2Vector xi, SquareShareZlVector yi) throws MpcAbortException;
}
