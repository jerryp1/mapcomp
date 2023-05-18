package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.naive;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;

/**
 * naive private equality test config.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class NaivePeqtConfig implements PeqtConfig {
    /**
     * Boolean circuit config
     */
    private final Z2cConfig z2cConfig;

    private NaivePeqtConfig(Builder builder) {
        z2cConfig = builder.z2cConfig;
    }

    public Z2cConfig getBcConfig() {
        return z2cConfig;
    }

    @Override
    public PeqtFactory.PeqtType getPtoType() {
        return PeqtFactory.PeqtType.NAIVE;
    }

    @Override
    public void setEnvType(EnvType envType) {
        z2cConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return z2cConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (z2cConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = z2cConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaivePeqtConfig> {
        /**
         * Boolean circuit config
         */
        private Z2cConfig z2cConfig;

        public Builder(boolean silent) {
            z2cConfig = Z2cFactory.createDefaultConfig(silent);
        }

        public Builder setBcConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        @Override
        public NaivePeqtConfig build() {
            return new NaivePeqtConfig(this);
        }
    }
}
