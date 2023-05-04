package edu.alibaba.mpc4j.s2pc.pso.aidpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;

import java.util.Set;

/**
 * aid PSI client.
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
public interface AidPsiClient<T> extends ThreePartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxClientElementSize max client element size.
     * @param maxServerElementSize max server element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException;

    /**
     * Executes the protocol
     *
     * @param clientElementSet  client element set.
     * @param serverElementSize server element size.
     * @return intersection.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException;
}
