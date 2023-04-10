package edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

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
     * @param maxBatchSize max batch size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxBatchSize) throws MpcAbortException;

}
