package edu.alibaba.mpc4j.s2pc.pcg.btg.impl.cache;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.RbtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.rbtg.RbtgFactory;

/**
 * 缓存BTG协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/7/14
 */
public class CacheBtgConfig implements BtgConfig {
    /**
     * RBTG协议配置项
     */
    private final RbtgConfig rbtgConfig;

    private CacheBtgConfig(Builder builder) {
        rbtgConfig = builder.rbtgConfig;
    }

    public RbtgConfig getRbtgConfig() {
        return rbtgConfig;
    }

    @Override
    public BtgFactory.BtgType getPtoType() {
        return BtgFactory.BtgType.CACHE;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CacheBtgConfig> {
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
        public CacheBtgConfig build() {
            return new CacheBtgConfig(this);
        }
    }
}
