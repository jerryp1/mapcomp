package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

/**
 * Keyword PIR client interface.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public interface KwPirClient<T> extends TwoPartyPto {

    /**
     * client initializes protocol.
     *
     * @param kwPirParams     keyword PIR params.
     * @param labelByteLength label byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(KwPirParams kwPirParams, int labelByteLength) throws MpcAbortException;

    /**
     * client initializes protocol.
     *
     * @param maxRetrievalSize max retrieval size.
     * @param labelByteLength  label byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxRetrievalSize, int labelByteLength) throws MpcAbortException;

    /**
     * client executes protocol.
     *
     * @param retrievalSet retrieval set.
     * @return keyword label map.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Map<T, ByteBuffer> pir(Set<T> retrievalSet) throws MpcAbortException;
}
