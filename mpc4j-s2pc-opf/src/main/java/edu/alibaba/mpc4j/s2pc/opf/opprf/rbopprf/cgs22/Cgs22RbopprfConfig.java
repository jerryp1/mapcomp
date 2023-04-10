package edu.alibaba.mpc4j.s2pc.opf.opprf.rbopprf.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rbopprf.RbopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rbopprf.RbopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;

/**
 * CGS22 Related-Batch OPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class Cgs22RbopprfConfig implements RbopprfConfig {
    /**
     * d = 3
     */
    private static final int D = 3;
    /**
     * OPRF config
     */
    private final OprfConfig oprfConfig;

    private Cgs22RbopprfConfig(Builder builder) {
        oprfConfig = builder.oprfConfig;
    }

    @Override
    public RbopprfFactory.RbopprfType getPtoType() {
        return RbopprfFactory.RbopprfType.CGS22;
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

    @Override
    public int getD() {
        return D;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22RbopprfConfig> {
        /**
         * OPRF config
         */
        private OprfConfig oprfConfig;

        public Builder() {
            oprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setOprfConfig(OprfConfig oprfConfig) {
            this.oprfConfig = oprfConfig;
            return this;
        }

        @Override
        public Cgs22RbopprfConfig build() {
            return new Cgs22RbopprfConfig(this);
        }
    }
}
