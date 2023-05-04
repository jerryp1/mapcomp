package edu.alibaba.mpc4j.s2pc.pso.aidpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;

/**
 * aid PSI aider.
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
public interface AidPsiAider extends ThreePartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void psi() throws MpcAbortException;
}
