package edu.alibaba.mpc4j.s2pc.pso.mppsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPto;

import java.util.Set;

public interface MppsiClient<T> extends MultiPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxClientElementSizes max client element size.
     * @param maxLeaderElementSize max leader element size.
     * @param selfIndex the index of current client in all clients.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int[] maxClientElementSizes, int maxLeaderElementSize, int selfIndex) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param clientElementSet  client element set.
     * @param leaderElementSize leader element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void psi(Set<T> clientElementSet, int leaderElementSize, int[] allClientElementSizes) throws MpcAbortException;
}
