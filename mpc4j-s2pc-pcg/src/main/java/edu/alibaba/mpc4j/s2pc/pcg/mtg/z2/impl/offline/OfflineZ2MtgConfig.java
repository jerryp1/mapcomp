package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.offline;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.RootZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.root.RootZ2MtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;

/**
 * 离线BTG协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/8
 */
public class OfflineZ2MtgConfig implements Z2MtgConfig {
    /**
     * RBTG协议配置项
     */
    private final RootZ2MtgConfig rootZ2MtgConfig;

    private OfflineZ2MtgConfig(Builder builder) {
        rootZ2MtgConfig = builder.rootZ2MtgConfig;
    }

    public RootZ2MtgConfig getRbtgConfig() {
        return rootZ2MtgConfig;
    }

    @Override
    public Z2MtgFactory.Z2MtgType getPtoType() {
        return Z2MtgFactory.Z2MtgType.OFFLINE;
    }

    @Override
    public int maxBaseNum() {
        return rootZ2MtgConfig.maxAllowNum();
    }

    @Override
    public EnvType getEnvType() {
        return rootZ2MtgConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return rootZ2MtgConfig.getSecurityModel();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<OfflineZ2MtgConfig> {
        /**
         * RBTG协议配置项
         */
        private RootZ2MtgConfig rootZ2MtgConfig;

        public Builder(SecurityModel securityModel) {
            rootZ2MtgConfig = RootZ2MtgFactory.createDefaultConfig(securityModel);
        }

        public Builder setRbtgConfig(RootZ2MtgConfig rootZ2MtgConfig) {
            this.rootZ2MtgConfig = rootZ2MtgConfig;
            return this;
        }

        @Override
        public OfflineZ2MtgConfig build() {
            return new OfflineZ2MtgConfig(this);
        }
    }
}