package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;

/**
 * direct Boolean triple generation config.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class DirectZ2MtgConfig implements Z2MtgConfig {
    /**
     * core Boolean triple generation config
     */
    private final Z2CoreMtgConfig z2CoreMtgConfig;

    private DirectZ2MtgConfig(Builder builder) {
        z2CoreMtgConfig = builder.z2CoreMtgConfig;
    }

    public Z2CoreMtgConfig getZ2CoreMtgConfig() {
        return z2CoreMtgConfig;
    }

    @Override
    public Z2MtgFactory.Z2MtgType getPtoType() {
        return Z2MtgFactory.Z2MtgType.DIRECT;
    }

    @Override
    public int maxBaseNum() {
        return z2CoreMtgConfig.maxNum();
    }

    @Override
    public void setEnvType(EnvType envType) {
        z2CoreMtgConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return z2CoreMtgConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return z2CoreMtgConfig.getSecurityModel();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectZ2MtgConfig> {
        /**
         * core Boolean triple generation config
         */
        private Z2CoreMtgConfig z2CoreMtgConfig;

        public Builder(SecurityModel securityModel) {
            z2CoreMtgConfig = Z2CoreMtgFactory.createDefaultConfig(securityModel, false);
        }

        public Builder setZ2CoreMtgConfig(Z2CoreMtgConfig z2CoreMtgConfig) {
            this.z2CoreMtgConfig = z2CoreMtgConfig;
            return this;
        }

        @Override
        public DirectZ2MtgConfig build() {
            return new DirectZ2MtgConfig(this);
        }
    }
}
