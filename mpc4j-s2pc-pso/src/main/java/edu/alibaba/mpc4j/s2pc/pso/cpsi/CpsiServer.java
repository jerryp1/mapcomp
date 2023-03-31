package edu.alibaba.mpc4j.s2pc.pso.cpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

import java.util.Set;

/**
 * Circuit PSI server.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public interface CpsiServer<T> extends TwoPartyPto {
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
     * @return the server output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    CpsiServerOutput<T> psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException;
}
