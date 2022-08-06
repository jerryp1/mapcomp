package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.ideal;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.RootZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.RootZ2MtgFactory;

/**
 * 理想根布尔三元组生成协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
public class IdealRootZ2MtgConfig implements RootZ2MtgConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;

    private IdealRootZ2MtgConfig(Builder builder) {
        envType = builder.envType;
    }

    @Override
    public RootZ2MtgFactory.RbtgType getPtoType() {
        return RootZ2MtgFactory.RbtgType.IDEAL;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<IdealRootZ2MtgConfig> {
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
        public IdealRootZ2MtgConfig build() {
            return new IdealRootZ2MtgConfig(this);
        }
    }
}
