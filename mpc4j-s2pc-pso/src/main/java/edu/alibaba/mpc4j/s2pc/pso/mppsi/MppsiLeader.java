package edu.alibaba.mpc4j.s2pc.pso.mppsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPto;

import java.util.Set;

public interface MppsiLeader<T> extends MultiPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxLeaderElementSize max leader element size.
     * @param maxClientElementSizes max client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxLeaderElementSize, int[] maxClientElementSizes) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param leaderElementSet  leader element set.
     * @param clientElementSizes client element size.
     * @return 协议输出结果。
     * @throws MpcAbortException the protocol failure aborts.
     */
    Set<T> psi(Set<T> leaderElementSet, int[] clientElementSizes) throws MpcAbortException;
}
