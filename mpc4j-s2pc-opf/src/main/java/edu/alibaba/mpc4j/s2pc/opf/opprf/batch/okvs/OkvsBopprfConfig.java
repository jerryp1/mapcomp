package edu.alibaba.mpc4j.s2pc.opf.opprf.batch.okvs;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;

/**
 * OKVS Batch OPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class OkvsBopprfConfig implements BopprfConfig {
    /**
     * OPRF config
     */
    private final OprfConfig oprfConfig;
    /**
     * OKVS type
     */
    private final OkvsFactory.OkvsType okvsType;

    private OkvsBopprfConfig(Builder builder) {
        oprfConfig = builder.oprfConfig;
        okvsType = builder.okvsType;
    }

    @Override
    public BopprfFactory.BopprfType getPtoType() {
        return BopprfFactory.BopprfType.OKVS;
    }

    @Override
    public void setEnvType(EnvType envType) {
        oprfConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return oprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public OprfConfig getOprfConfig() {
        return oprfConfig;
    }

    public OkvsFactory.OkvsType getOkvsType() {
        return okvsType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<OkvsBopprfConfig> {
        /**
         * OPRF config
         */
        private OprfConfig oprfConfig;
        /**
         * OKVS type
         */
        private OkvsFactory.OkvsType okvsType;

        public Builder() {
            oprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            okvsType = OkvsFactory.OkvsType.MEGA_BIN;
        }

        public Builder setOprfConfig(OprfConfig oprfConfig) {
            this.oprfConfig = oprfConfig;
            return this;
        }

        public Builder setOkvsType(OkvsFactory.OkvsType okvsType) {
            this.okvsType = okvsType;
            return this;
        }

        @Override
        public OkvsBopprfConfig build() {
            return new OkvsBopprfConfig(this);
        }
    }
}
