package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole.kos16;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole.ZpVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole.ZpVoleFactory;

/**
 * KOS16-ZP_VOLE协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/06/08
 */
public class Kos16ShZpVoleConfig implements ZpVoleConfig {
    /**
     * 基础OT协议
     */
    private final BaseOtConfig baseOtConfig;

    private Kos16ShZpVoleConfig(Builder builder) {
        baseOtConfig = builder.baseOtConfig;
    }

    public BaseOtConfig getBaseOtConfig() {
        return baseOtConfig;
    }

    @Override
    public ZpVoleFactory.ZpVoleType getPtoType() {
        return ZpVoleFactory.ZpVoleType.KOS16_SEMI_HONEST;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kos16ShZpVoleConfig> {
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
        public Kos16ShZpVoleConfig build() {
            return new Kos16ShZpVoleConfig(this);
        }
    }

}
