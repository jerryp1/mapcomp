package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.naive;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.PlainPeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.PlainPeqtFactory;

/**
 * naive plain private equality test config.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class NaivePlainPeqtConfig implements PlainPeqtConfig {
    /**
     * Boolean circuit config
     */
    private final BcConfig bcConfig;

    private NaivePlainPeqtConfig(Builder builder) {
        bcConfig = builder.bcConfig;
    }

    public BcConfig getBcConfig() {
        return bcConfig;
    }

    @Override
    public PlainPeqtFactory.PlainPeqtType getPtoType() {
        return PlainPeqtFactory.PlainPeqtType.NAIVE;
    }

    @Override
    public void setEnvType(EnvType envType) {
        bcConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return bcConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (bcConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = bcConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaivePlainPeqtConfig> {
        /**
         * Boolean circuit config
         */
        private BcConfig bcConfig;

        public Builder(SecurityModel securityModel) {
            bcConfig = BcFactory.createDefaultConfig(securityModel);
        }

        public Builder setBcConfig(BcConfig bcConfig) {
            this.bcConfig = bcConfig;
            return this;
        }

        @Override
        public NaivePlainPeqtConfig build() {
            return new NaivePlainPeqtConfig(this);
        }
    }
}
