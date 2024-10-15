package edu.alibaba.mpc4j.s2pc.opf.spermutation;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.Vector;

/**
 * Shared permutation party interface.
 *
 */
public interface SharedPermutationParty extends TwoPartyPto {
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
     * @param perms the shared permutation of party.
     * @param x     the shared inputs party.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Vector<byte[]> permute(Vector<byte[]> perms, Vector<byte[]> x) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param perms the shared permutation of party.
     * @param x     the shared inputs party.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector[][] permute(SquareZ2Vector[] perms, SquareZ2Vector[][] x) throws MpcAbortException;
}
