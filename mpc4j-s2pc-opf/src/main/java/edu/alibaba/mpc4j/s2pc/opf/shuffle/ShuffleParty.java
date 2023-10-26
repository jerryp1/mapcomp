package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.List;
import java.util.Vector;

/**
 * Shuffle party interface.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
public interface ShuffleParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxL   max length.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param x          the input of party.
     * @param randomPerm the random permutation.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    List<Vector<byte[]>> shuffle(List<Vector<byte[]>> x, int[] randomPerm) throws MpcAbortException;
}
