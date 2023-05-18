package edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory.OprpType;

/**
 * LowMc-OPRP协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class LowMcOprpConfig implements OprpConfig {
    /**
     * BC协议
     */
    private final Z2cConfig z2cConfig;

    private LowMcOprpConfig(Builder builder) {
        z2cConfig = builder.z2cConfig;
    }

    public Z2cConfig getBcConfig() {
        return z2cConfig;
    }

    @Override
    public OprpType getPtoType() {
        return OprpType.LOW_MC;
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
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (z2cConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = z2cConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<LowMcOprpConfig> {
        /**
         * BC协议配置项
         */
        private Z2cConfig z2cConfig;

        public Builder() {
            z2cConfig = Z2cFactory.createDefaultConfig(true);
        }

        public Builder setZ2cConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        @Override
        public LowMcOprpConfig build() {
            return new LowMcOprpConfig(this);
        }
    }
}
