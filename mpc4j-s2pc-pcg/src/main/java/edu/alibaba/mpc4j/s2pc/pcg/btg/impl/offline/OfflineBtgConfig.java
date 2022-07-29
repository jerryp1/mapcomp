package edu.alibaba.mpc4j.s2pc.pcg.btg.impl.offline;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.RbtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.RbtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BtgFactory;

/**
 * 离线BTG协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/4/8
 */
public class OfflineBtgConfig implements BtgConfig {
    /**
     * RBTG协议配置项
     */
    private final RbtgConfig rbtgConfig;

    private OfflineBtgConfig(Builder builder) {
        rbtgConfig = builder.rbtgConfig;
    }

    public RbtgConfig getRbtgConfig() {
        return rbtgConfig;
    }

    @Override
    public BtgFactory.BtgType getPtoType() {
        return BtgFactory.BtgType.OFFLINE;
    }

    @Override
    public int maxBaseNum() {
        return rbtgConfig.maxAllowNum();
    }

    @Override
    public EnvType getEnvType() {
        return rbtgConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return rbtgConfig.getSecurityModel();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<OfflineBtgConfig> {
        /**
         * RBTG协议配置项
         */
        private RbtgConfig rbtgConfig;

        public Builder(SecurityModel securityModel) {
            rbtgConfig = RbtgFactory.createDefaultConfig(securityModel);
        }

        public Builder setRbtgConfig(RbtgConfig rbtgConfig) {
            this.rbtgConfig = rbtgConfig;
            return this;
        }

        @Override
        public OfflineBtgConfig build() {
            return new OfflineBtgConfig(this);
        }
    }
}