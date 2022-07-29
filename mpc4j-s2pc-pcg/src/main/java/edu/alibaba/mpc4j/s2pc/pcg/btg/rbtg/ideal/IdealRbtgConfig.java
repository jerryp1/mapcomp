package edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.ideal;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.RbtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.RbtgFactory;

/**
 * 理想RBTG协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
public class IdealRbtgConfig implements RbtgConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;

    private IdealRbtgConfig(Builder builder) {
        envType = builder.envType;
    }

    @Override
    public RbtgFactory.RbtgType getPtoType() {
        return RbtgFactory.RbtgType.IDEAL;
    }

    @Override
    public int maxAllowNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.IDEAL;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<IdealRbtgConfig> {
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
        public IdealRbtgConfig build() {
            return new IdealRbtgConfig(this);
        }
    }
}
