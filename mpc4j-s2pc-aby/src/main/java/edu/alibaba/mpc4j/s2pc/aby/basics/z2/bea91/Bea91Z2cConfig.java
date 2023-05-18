package edu.alibaba.mpc4j.s2pc.aby.basics.z2.bea91;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;

/**
 * Bea91 Z2 circuit config.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class Bea91Z2cConfig implements Z2cConfig {
    /**
     * Boolean triple generation config
     */
    private final Z2MtgConfig z2MtgConfig;

    private Bea91Z2cConfig(Builder builder) {
        z2MtgConfig = builder.z2MtgConfig;
    }

    public Z2MtgConfig getMtgConfig() {
        return z2MtgConfig;
    }

    @Override
    public Z2cFactory.BcType getPtoType() {
        return Z2cFactory.BcType.BEA91;
    }

    @Override
    public void setEnvType(EnvType envType) {
        z2MtgConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return z2MtgConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (z2MtgConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = z2MtgConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea91Z2cConfig> {
        /**
         * Boolean triple generation config
         */
        private Z2MtgConfig z2MtgConfig;

        public Builder() {
            z2MtgConfig = Z2MtgFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public Builder setZ2MtgConfig(Z2MtgConfig z2MtgConfig) {
            this.z2MtgConfig = z2MtgConfig;
            return this;
        }

        @Override
        public Bea91Z2cConfig build() {
            return new Bea91Z2cConfig(this);
        }
    }
}
