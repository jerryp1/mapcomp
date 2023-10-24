package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.List;

/**
 * payload-circuit PSI client, where server encodes payload into circuit
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public interface PlpsiClient<T> extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxClientElementSize max client element size.
     * @param maxServerElementSize max server element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param clientElementList client element list.
     * @param serverElementSize server element size.
     * @return the client output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PlpsiClientOutput<T> psi(List<T> clientElementList, int serverElementSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param payloadBitL the bit length of server payload
     * @param isBinaryShare whether binary sharing or not
     * @return the shared output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Payload intersectPayload(int payloadBitL, boolean isBinaryShare) throws MpcAbortException;
}
