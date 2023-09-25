package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.shuffle;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirFactory;

/**
 * Shuffle client-specific preprocessing PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/20
 */
public class ShuffleSingleKeywordCpPirConfig extends AbstractMultiPartyPtoConfig implements SingleKeywordCpPirConfig {

    public ShuffleSingleKeywordCpPirConfig() {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleKeywordCpPirFactory.SingleKeywordCpPirType getProType() {
        return SingleKeywordCpPirFactory.SingleKeywordCpPirType.LLP23_STREAM_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<ShuffleSingleKeywordCpPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public ShuffleSingleKeywordCpPirConfig build() {
            return new ShuffleSingleKeywordCpPirConfig();
        }
    }
}
