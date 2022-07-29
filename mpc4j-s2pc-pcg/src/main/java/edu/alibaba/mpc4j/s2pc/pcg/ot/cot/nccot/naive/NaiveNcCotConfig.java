package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.naive;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nccot.NcCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.rcot.RcotFactory;

/**
 * 朴素NCCOT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/7/14
 */
public class NaiveNcCotConfig implements NcCotConfig {
    /**
     * RCOT协议配置项
     */
    private final RcotConfig rcotConfig;

    private NaiveNcCotConfig(Builder builder) {
        rcotConfig = builder.rcotConfig;
    }

    public RcotConfig getRcotConfig() {
        return rcotConfig;
    }

    @Override
    public NcCotFactory.NcCotType getPtoType() {
        return NcCotFactory.NcCotType.NAIVE;
    }

    @Override
    public int maxAllowNum() {
        // 底层协议理论上可以支持任意长度，但我们仍然做出一些限制，防止内存不足
        return 1 << 24;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaiveNcCotConfig> {
        /**
         * RCOT协议配置项
         */
        private RcotConfig rcotConfig;

        public Builder(SecurityModel securityModel) {
            rcotConfig = RcotFactory.createDefaultConfig(securityModel);
        }

        public Builder setRcotConfig(RcotConfig rcotConfig) {
            this.rcotConfig = rcotConfig;
            return this;
        }

        @Override
        public NaiveNcCotConfig build() {
            return new NaiveNcCotConfig(this);
        }
    }
}
