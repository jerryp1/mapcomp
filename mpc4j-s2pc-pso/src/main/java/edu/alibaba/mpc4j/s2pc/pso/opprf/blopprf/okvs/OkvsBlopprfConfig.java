package edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf.okvs;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf.BlopprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.opprf.blopprf.BlopprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;

/**
 * OKVS Batched l-bit-input OPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
public class OkvsBlopprfConfig implements BlopprfConfig {
    /**
     * oprf config
     */
    private final OprfConfig oprfConfig;
    /**
     * the OKVS type
     */
    private final OkvsFactory.OkvsType okvsType;

    private OkvsBlopprfConfig(Builder builder) {
        oprfConfig = builder.oprfConfig;
        okvsType = builder.okvsType;
    }

    @Override
    public BlopprfFactory.BlopprfType getPtoType() {
        return BlopprfFactory.BlopprfType.OKVS;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<OkvsBlopprfConfig> {
        /**
         * oprf config
         */
        private OprfConfig oprfConfig;
        /**
         * the OKVS type
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
        public OkvsBlopprfConfig build() {
            return new OkvsBlopprfConfig(this);
        }
    }
}
