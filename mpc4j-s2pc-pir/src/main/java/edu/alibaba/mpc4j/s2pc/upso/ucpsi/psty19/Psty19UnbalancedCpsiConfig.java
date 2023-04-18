package edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UnbalancedCpsiConfig;

import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.UnbalancedCpsiFactory.*;

/**
 * PSTY19 unbalanced circuit PSI config.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class Psty19UnbalancedCpsiConfig implements UnbalancedCpsiConfig {
    /**
     * Batch OPPRF config
     */
    private final BopprfConfig bopprfConfig;
    /**
     * Boolean Circuit config
     */
    private final BcConfig bcConfig;

    private Psty19UnbalancedCpsiConfig(Builder builder) {
        assert builder.bopprfConfig.getEnvType().equals(builder.bcConfig.getEnvType());
        bopprfConfig = builder.bopprfConfig;
        bcConfig = builder.bcConfig;
    }

    @Override
    public UnbalancedCpsiType getPtoType() {
        return UnbalancedCpsiType.PSTY19;
    }

    @Override
    public void setEnvType(EnvType envType) {
        bopprfConfig.setEnvType(envType);
        bcConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return bopprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public BopprfConfig getBopprfConfig() {
        return bopprfConfig;
    }

    public BcConfig getBcConfig() {
        return bcConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Psty19UnbalancedCpsiConfig> {
        /**
         * Batch OPPRF config
         */
        private BopprfConfig bopprfConfig;
        /**
         * Boolean Circuit config
         */
        private BcConfig bcConfig;

        public Builder() {
            bopprfConfig = BopprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            bcConfig = BcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public Builder setBopprfConfig(BopprfConfig bopprfConfig) {
            this.bopprfConfig = bopprfConfig;
            return this;
        }

        public Builder setBcConfig(BcConfig bcConfig) {
            this.bcConfig = bcConfig;
            return this;
        }

        @Override
        public Psty19UnbalancedCpsiConfig build() {
            return new Psty19UnbalancedCpsiConfig(this);
        }
    }
}
