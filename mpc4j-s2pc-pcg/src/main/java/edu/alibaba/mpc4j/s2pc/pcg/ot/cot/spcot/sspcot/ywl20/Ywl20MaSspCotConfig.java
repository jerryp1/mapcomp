package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.spcot.sspcot.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.spcot.sspcot.SspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.spcot.sspcot.SspCotFactory.SspCotType;

/**
 * YWL20-SSP-COT恶意安全协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/6/7
 */
public class Ywl20MaSspCotConfig implements SspCotConfig {
    /**
     * RCOT协议配置项
     */
    private final RcotConfig rcotConfig;

    private Ywl20MaSspCotConfig(Builder builder) {
        rcotConfig = builder.rcotConfig;
    }

    public RcotConfig getRcotConfig() {
        return rcotConfig;
    }

    @Override
    public SspCotType getPtoType() {
        return SspCotType.YWL20_MALICIOUS;
    }

    @Override
    public EnvType getEnvType() {
        return rcotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (rcotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = rcotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20MaSspCotConfig> {
        /**
         * RCOT协议配置项
         */
        private RcotConfig rcotConfig;

        public Builder() {
            rcotConfig = RcotFactory.createDefaultConfig(SecurityModel.MALICIOUS);
        }

        public Builder setRcotConfig(RcotConfig rcotConfig) {
            this.rcotConfig = rcotConfig;
            return this;
        }

        @Override
        public Ywl20MaSspCotConfig build() {
            return new Ywl20MaSspCotConfig(this);
        }
    }
}