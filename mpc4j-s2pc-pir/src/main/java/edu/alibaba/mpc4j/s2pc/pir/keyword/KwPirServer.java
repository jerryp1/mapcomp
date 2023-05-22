package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Keyword PIR server interface.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public interface KwPirServer<T> extends TwoPartyPto {
    /**
     * server initializes protocol.
     *
     * @param kwPirParams     keyword PIR params.
     * @param keywordLabelMap keyword label map.
     * @param labelByteLength label byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(KwPirParams kwPirParams, Map<T, ByteBuffer> keywordLabelMap, int labelByteLength) throws MpcAbortException;

    /**
     * server initializes protocol.
     *
     * @param keywordLabelMap  keyword label map.
     * @param maxRetrievalSize max retrieval size.
     * @param labelByteLength  label byte length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Map<T, ByteBuffer> keywordLabelMap, int maxRetrievalSize, int labelByteLength) throws MpcAbortException;

    /**
     * server executes protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void pir() throws MpcAbortException;
}
