package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.shuffle;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirFactory.SingleKeywordCpPirType;

/**
 * Shuffle keyword client-specific preprocessing PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/20
 */
public class ShuffleSingleKeywordCpPirConfig extends AbstractMultiPartyPtoConfig implements SingleKeywordCpPirConfig {
    /**
     * sq-OPRF config
     */
    private final SqOprfConfig sqOprfConfig;

    public ShuffleSingleKeywordCpPirConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.sqOprfConfig);
        sqOprfConfig = builder.sqOprfConfig;
    }

    @Override
    public SingleKeywordCpPirType getProType() {
        return SingleKeywordCpPirType.LLP23_SHUFFLE;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<ShuffleSingleKeywordCpPirConfig> {
        /**
         * sq-OPRF config
         */
        private SqOprfConfig sqOprfConfig;

        public Builder() {
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setSqOprfConfig(SqOprfConfig sqOprfConfig) {
            this.sqOprfConfig = sqOprfConfig;
            return this;
        }

        @Override
        public ShuffleSingleKeywordCpPirConfig build() {
            return new ShuffleSingleKeywordCpPirConfig(this);
        }
    }
}
