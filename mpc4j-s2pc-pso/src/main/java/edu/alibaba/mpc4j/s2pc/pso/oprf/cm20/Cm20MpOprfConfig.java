package edu.alibaba.mpc4j.s2pc.pso.oprf.cm20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;

/**
 * CM20-MPOPRF协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public class Cm20MpOprfConfig implements MpOprfConfig {
    /**
     * RCOT协议配置项
     */
    private final RcotConfig rcotConfig;

    private Cm20MpOprfConfig(Builder builder) {
        rcotConfig = builder.rcotConfig;
    }

    public RcotConfig getRcotConfig() {
        return rcotConfig;
    }

    @Override
    public OprfFactory.OprfType getPtoType() {
        return OprfFactory.OprfType.CM20;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cm20MpOprfConfig> {
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
        public Cm20MpOprfConfig build() {
            return new Cm20MpOprfConfig(this);
        }
    }
}
