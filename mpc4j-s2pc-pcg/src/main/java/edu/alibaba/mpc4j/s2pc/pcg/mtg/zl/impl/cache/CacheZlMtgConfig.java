package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.impl.cache;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgFactory;

/**
 * cache Zl multiplication triple generator config.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public class CacheZlMtgConfig implements ZlMtgConfig {
    /**
     * Zl core multiplication triple generator config
     */
    private final ZlCoreMtgConfig zlCoreMtgConfig;

    private CacheZlMtgConfig(Builder builder) {
        zlCoreMtgConfig = builder.zlCoreMtgConfig;
    }

    public ZlCoreMtgConfig getZlCoreMtgConfig() {
        return zlCoreMtgConfig;
    }

    @Override
    public ZlMtgFactory.ZlMtgType getPtoType() {
        return ZlMtgFactory.ZlMtgType.CACHE;
    }

    @Override
    public Zl getZl() {
        return zlCoreMtgConfig.getZl();
    }

    @Override
    public void setEnvType(EnvType envType) {
        zlCoreMtgConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return zlCoreMtgConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return zlCoreMtgConfig.getSecurityModel();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CacheZlMtgConfig> {
        /**
         * Zl core multiplication triple generator config
         */
        private ZlCoreMtgConfig zlCoreMtgConfig;

        public Builder(SecurityModel securityModel, Zl zl) {
            zlCoreMtgConfig = ZlCoreMtgFactory.createDefaultConfig(securityModel, zl);
        }

        public Builder setZlCoreMtgConfig(ZlCoreMtgConfig zlCoreMtgConfig) {
            this.zlCoreMtgConfig = zlCoreMtgConfig;
            return this;
        }

        @Override
        public CacheZlMtgConfig build() {
            return new CacheZlMtgConfig(this);
        }
    }
}
