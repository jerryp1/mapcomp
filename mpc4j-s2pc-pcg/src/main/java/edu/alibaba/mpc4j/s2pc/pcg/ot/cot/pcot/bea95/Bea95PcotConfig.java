package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.bea95;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.PcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.PcotFactory;

/**
 * Bea95-PCOT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class Bea95PcotConfig implements PcotConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;

    private Bea95PcotConfig(Builder builder) {
        envType = builder.envType;
    }

    @Override
    public PcotFactory.PcotType getPtoType() {
        return PcotFactory.PcotType.Bea95;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.MALICIOUS;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea95PcotConfig> {
        /**
         * 环境类型
         */
        private EnvType envType;

        public Builder() {
            super();
            this.envType = EnvType.STANDARD;
        }

        public Builder setEnvType(EnvType envType) {
            this.envType = envType;
            return this;
        }

        @Override
        public Bea95PcotConfig build() {
            return new Bea95PcotConfig(this);
        }
    }
}
