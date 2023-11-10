package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.List;

/**
 * payload-circuit PSI server, where server encodes payload into circuit
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public interface PlpsiServer<T, X> extends TwoPartyPto {
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
     * @param serverElementList server element list.
     * @param clientElementSize client element size.
     * @return the server output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PlpsiShareOutput psi(List<T> serverElementList, int clientElementSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param serverPayloadList server payload
     * @param payloadBitLs      valid bit length
     * @param isBinaryShare     whether binary sharing or not
     * @throws MpcAbortException the protocol failure aborts.
     */
    void intersectPayload(List<X> serverPayloadList, int payloadBitLs, boolean isBinaryShare) throws MpcAbortException;

    /**
     * Executes the circuit psi with payload.
     *
     * @param serverElementList server element list.
     * @param clientElementSize client element size.
     * @param serverPayloadLists server payloads
     * @param payloadBitLs valid bit lengths
     * @param isBinaryShare whether binary sharing or not
     * @return the server output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PlpsiShareOutput psiWithPayload(List<T> serverElementList, int clientElementSize,
                                    List<List<X>> serverPayloadLists, int[] payloadBitLs, boolean[] isBinaryShare) throws MpcAbortException;
}
