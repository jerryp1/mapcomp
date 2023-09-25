package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.llp23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirFactory;

/**
 * LLP23 client-specific preprocessing PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/20
 */
public class Llp23SingleKeywordCpPirConfig extends AbstractMultiPartyPtoConfig implements SingleKeywordCpPirConfig {

    public Llp23SingleKeywordCpPirConfig() {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleKeywordCpPirFactory.SingleKeywordCpPirType getProType() {
        return SingleKeywordCpPirFactory.SingleKeywordCpPirType.LLP23_STREAM_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Llp23SingleKeywordCpPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Llp23SingleKeywordCpPirConfig build() {
            return new Llp23SingleKeywordCpPirConfig();
        }
    }
}
