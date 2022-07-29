package edu.alibaba.mpc4j.s2pc.pso.oprf.kkrt16;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;

/**
 * KKRT16-ORI-OPRF协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/05
 */
public class Kkrt16OriOprfConfig implements OprfConfig {
    /**
     * RCOT协议配置项
     */
    private final RcotConfig rcotConfig;

    private Kkrt16OriOprfConfig(Builder builder) {
        rcotConfig = builder.rcotConfig;
    }

    public RcotConfig getRcotConfig() {
        return rcotConfig;
    }

    @Override
    public OprfFactory.OprfType getPtoType() {
        return OprfFactory.OprfType.KKRT16_ORI;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kkrt16OriOprfConfig> {
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
        public Kkrt16OriOprfConfig build() {
            return new Kkrt16OriOprfConfig(this);
        }
    }
}
