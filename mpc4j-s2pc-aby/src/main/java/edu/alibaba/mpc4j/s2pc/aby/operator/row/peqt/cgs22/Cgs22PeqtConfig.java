package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;

/**
 * CGS22 private equality test config.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class Cgs22PeqtConfig implements PeqtConfig {
    /**
     * Boolean circuit config
     */
    private final Z2cConfig z2cConfig;
    /**
     * LNOT config
     */
    private final LnotConfig lnotConfig;

    private Cgs22PeqtConfig(Builder builder) {
        assert builder.z2cConfig.getEnvType().equals(builder.lnotConfig.getEnvType());
        z2cConfig = builder.z2cConfig;
        lnotConfig = builder.lnotConfig;
    }

    public Z2cConfig getBcConfig() {
        return z2cConfig;
    }

    public LnotConfig getLnotConfig() {
        return lnotConfig;
    }

    @Override
    public PeqtFactory.PeqtType getPtoType() {
        return PeqtFactory.PeqtType.CGS22;
    }

    @Override
    public void setEnvType(EnvType envType) {
        z2cConfig.setEnvType(envType);
        lnotConfig.setEnvType(envType);
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22PeqtConfig> {
        /**
         * Boolean circuit config
         */
        private Z2cConfig z2cConfig;
        /**
         * LNOT config
         */
        private LnotConfig lnotConfig;

        public Builder(boolean silent) {
            z2cConfig = Z2cFactory.createDefaultConfig(silent);
            if (silent) {
                lnotConfig = LnotFactory.createCacheConfig(SecurityModel.SEMI_HONEST);
            } else {
                lnotConfig = LnotFactory.createDirectConfig(SecurityModel.SEMI_HONEST);
            }
        }

        public Builder setBcConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        public Builder setLnotConfig(LnotConfig lnotConfig) {
            this.lnotConfig = lnotConfig;
            return this;
        }

        @Override
        public Cgs22PeqtConfig build() {
            return new Cgs22PeqtConfig(this);
        }
    }
}
