package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Map;

/**
 * Single Keyword Client-specific Preprocessing PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public interface SingleKeywordCpPirServer<T> extends TwoPartyPto {
    /**
     * Server initializes the protocol.
     *
     * @param keyValueMap    key value map.
     * @param valueBitLength value bit length.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Map<T, byte[]> keyValueMap, int valueBitLength) throws MpcAbortException;

    /**
     * Server executes the protocol.
     *
     * @return server information.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SingleKeywordCpPirServerOutput pir() throws MpcAbortException;
}
