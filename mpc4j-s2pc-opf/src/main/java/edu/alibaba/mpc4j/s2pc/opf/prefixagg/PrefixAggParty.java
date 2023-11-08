package edu.alibaba.mpc4j.s2pc.opf.prefixagg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.util.Vector;

/**
 * Prefix sum Interface.
 *
 * @author Li Peng
 * @date 2023/10/30
 */
public interface PrefixAggParty extends TwoPartyPto {
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
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PrefixAggOutput agg(Vector<byte[]> groupField, SquareZlVector sumField) throws MpcAbortException;

    /**
     * Executes the protocol. Assume groupField is hold by receiver.
     *
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PrefixAggOutput agg(String[] groupField, SquareZlVector aggField) throws MpcAbortException;
}
