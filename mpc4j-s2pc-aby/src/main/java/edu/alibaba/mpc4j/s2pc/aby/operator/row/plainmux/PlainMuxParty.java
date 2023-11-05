package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainmux;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Zl plain mux party.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public interface PlainMuxParty extends TwoPartyPto {
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
    SquareZlVector mux(SquareZ2Vector xi, long[] yi) throws MpcAbortException;
}
