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
public interface PlpsiServer<T> extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxServerElementSize max server element size.
     * @param maxClientElementSize max client element size.
     * @param payloadBitL max bit length of server's payload.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxServerElementSize, int maxClientElementSize, int payloadBitL) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param serverElementList  server element list.
     * @param serverPayloadList server payload list
     * @param clientElementSize client element size.
     * @return the server output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PlpsiServerOutput psi(List<T> serverElementList, List<T> serverPayloadList, int clientElementSize) throws MpcAbortException;
}
