package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.spvole.sspvole.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.spvole.sspvole.Z2SspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.spvole.sspvole.Z2SspVoleFactory;

/**
 * WYKW21-Z2-SSP-VOLE半诚实安全协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/6/13
 */
public class Wykw21ShZ2SspVoleConfig implements Z2SspVoleConfig {
    /**
     * RCOT协议配置项
     */
    private final RcotConfig rcotConfig;

    private Wykw21ShZ2SspVoleConfig(Builder builder) {
        rcotConfig = builder.rcotConfig;
    }

    public RcotConfig getRcotConfig() {
        return rcotConfig;
    }

    @Override
    public Z2SspVoleFactory.Z2SspVoleType getPtoType() {
        return Z2SspVoleFactory.Z2SspVoleType.WYKW21_SEMI_HONEST;
    }

    @Override
    public EnvType getEnvType() {
        return rcotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (rcotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = rcotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Wykw21ShZ2SspVoleConfig> {
        /**
         * 根COT协议配置项
         */
        private RcotConfig rcotConfig;

        public Builder() {
            rcotConfig = RcotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setRcotConfig(RcotConfig rcotConfig) {
            this.rcotConfig = rcotConfig;
            return this;
        }

        @Override
        public Wykw21ShZ2SspVoleConfig build() {
            return new Wykw21ShZ2SspVoleConfig(this);
        }
    }
}
