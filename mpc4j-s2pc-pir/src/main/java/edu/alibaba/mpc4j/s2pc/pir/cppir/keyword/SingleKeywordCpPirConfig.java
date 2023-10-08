package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Single Keyword Client-specific Preprocessing PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public interface SingleKeywordCpPirConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    SingleKeywordCpPirFactory.SingleKeywordCpPirType getProType();
}
