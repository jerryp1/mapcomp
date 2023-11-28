package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

public interface Z2MuxParty extends TwoPartyPto {
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
     * @param f the binary share choice bit f.
     * @param x the binary shares x.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector[] mux(SquareZ2Vector f, SquareZ2Vector[] x) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param f the binary share xiArray.
     * @param x the binary share yiArray.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector[][] mux(SquareZ2Vector[] f, SquareZ2Vector[][] x) throws MpcAbortException;
}
