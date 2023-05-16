package edu.alibaba.mpc4j.s2pc.aby.millionaire;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;

/**
 * Millionaire Protocol Party.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public interface MillionaireParty extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxL   max input bit length.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param l      input bit length.
     * @param inputs the party's inputs.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector lt(int l, byte[][] inputs) throws MpcAbortException;
}
