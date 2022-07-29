package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.vole.kos16;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.vole.Z2VoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.vole.Z2VoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;

/**
 * KOS16-Z2-半诚实安全VOLE协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
public class Kos16ShZ2VoleConfig implements Z2VoleConfig {
    /**
     * 基础OT协议
     */
    private final BaseOtConfig baseOtConfig;

    private Kos16ShZ2VoleConfig(Builder builder) {
        baseOtConfig = builder.baseOtConfig;
    }

    public BaseOtConfig getBaseOtConfig() {
        return baseOtConfig;
    }

    @Override
    public Z2VoleFactory.Z2VoleType getPtoType() {
        return Z2VoleFactory.Z2VoleType.KOS16_SEMI_HONEST;
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    public EnvType getEnvType() {
        return baseOtConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (baseOtConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = baseOtConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kos16ShZ2VoleConfig> {
        /**
         * 基础OT协议配置项
         */
        private BaseOtConfig baseOtConfig;

        public Builder() {
            baseOtConfig = BaseOtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setBaseOtConfig(BaseOtConfig baseOtConfig) {
            this.baseOtConfig = baseOtConfig;
            return this;
        }

        @Override
        public Kos16ShZ2VoleConfig build() {
            return new Kos16ShZ2VoleConfig(this);
        }
    }
}
