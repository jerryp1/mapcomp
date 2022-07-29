package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.kk13;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.LotFactory;

/**
 * KK13-LHOT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/23
 */
public class Kk13OriLhotConfig implements LhotConfig {
    /**
     * RCOT协议配置项
     */
    private final RcotConfig rcotConfig;

    private Kk13OriLhotConfig(Builder builder) {
        rcotConfig = builder.rcotConfig;
    }

    public RcotConfig getRcotConfig() {
        return rcotConfig;
    }

    @Override
    public LotFactory.LotType getPtoType() {
        return LotFactory.LotType.KK13_ORI;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kk13OriLhotConfig> {
        /**
         * RCOT协议配置项
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
        public Kk13OriLhotConfig build() {
            return new Kk13OriLhotConfig(this);
        }
    }
}
