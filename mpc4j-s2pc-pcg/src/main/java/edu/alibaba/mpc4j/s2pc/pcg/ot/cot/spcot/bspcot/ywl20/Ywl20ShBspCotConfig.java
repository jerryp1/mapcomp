package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.spcot.bspcot.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.spcot.bspcot.BspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.spcot.bspcot.BspCotFactory;

/**
 * YWL20-BSP-COT半诚实安全协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
public class Ywl20ShBspCotConfig implements BspCotConfig {
    /**
     * RCOT协议配置项
     */
    private final RcotConfig rcotConfig;

    private Ywl20ShBspCotConfig(Builder builder) {
        rcotConfig = builder.rcotConfig;
    }

    public RcotConfig getRcotConfig() {
        return rcotConfig;
    }

    @Override
    public BspCotFactory.BspCotType getPtoType() {
        return BspCotFactory.BspCotType.YWL20_SEMI_HONEST;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20ShBspCotConfig> {
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
        public Ywl20ShBspCotConfig build() {
            return new Ywl20ShBspCotConfig(this);
        }
    }
}
