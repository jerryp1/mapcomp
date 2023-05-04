package edu.alibaba.mpc4j.s2pc.pso.aidpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;

import java.util.Set;

/**
 * aided PSI server.
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
public interface AidPsiServer<T> extends ThreePartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxServerElementSize max server element size.
     * @param maxClientElementSize max client element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param serverElementSet  server element set.
     * @param clientElementSize client element size.
     * @return intersection.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Set<T> psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException;
}
