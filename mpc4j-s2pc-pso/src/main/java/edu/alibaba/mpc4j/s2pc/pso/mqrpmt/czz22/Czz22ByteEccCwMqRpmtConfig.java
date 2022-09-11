package edu.alibaba.mpc4j.s2pc.pso.mqrpmt.czz22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pso.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pso.mqrpmt.MqRpmtFactory;

/**
 * ZZL22-字节椭圆曲线mqRPMT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public class Czz22ByteEccCwMqRpmtConfig implements MqRpmtConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * 是否使用过滤器
     */
    private final boolean useFilter;

    private Czz22ByteEccCwMqRpmtConfig(Builder builder) {
        envType = builder.envType;
        useFilter = builder.useFilter;
    }

    @Override
    public MqRpmtFactory.MqRpmtType getPtoType() {
        return MqRpmtFactory.MqRpmtType.CZZ22_BYTE_ECC_CW;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public boolean getUseFilter() {
        return useFilter;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Czz22ByteEccCwMqRpmtConfig> {
        /**
         * 环境类型
         */
        private EnvType envType;
        /**
         * 是否使用过滤器
         */
        private boolean useFilter;

        public Builder() {
            super();
            this.envType = EnvType.STANDARD;
            useFilter = true;
        }

        public Builder setEnvType(EnvType envType) {
            this.envType = envType;
            return this;
        }

        public Builder setUseFilter(boolean useFilter) {
            this.useFilter = useFilter;
            return this;
        }

        @Override
        public Czz22ByteEccCwMqRpmtConfig build() {
            return new Czz22ByteEccCwMqRpmtConfig(this);
        }
    }
}
