package edu.alibaba.mpc4j.s2pc.opf.permutation;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.util.Vector;

/**
 * Permutation sender interface.

 */
public interface PermutationSender extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxL   max l.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param perm the share of permutation.
     * @param x    the input of sender.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZlVector permute(SquareZlVector perm, ZlVector x) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param perm the share of permutation.
     * @param x    the input of sender.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Vector<byte[]> permute(Vector<byte[]> perm, Vector<byte[]> x) throws MpcAbortException;
}
