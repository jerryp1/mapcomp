package edu.alibaba.mpc4j.s2pc.aby.base.bc.bea91;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.base.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BtgFactory;
import edu.alibaba.mpc4j.s2pc.aby.base.bc.BcFactory;

/**
 * Beaver91-BC协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class Bea91BcConfig implements BcConfig {
    /**
     * BTG协议
     */
    private final BtgConfig btgConfig;

    private Bea91BcConfig(Builder builder) {
        btgConfig = builder.btgConfig;
    }

    public BtgConfig getBtgConfig() {
        return btgConfig;
    }

    @Override
    public BcFactory.BcType getPtoType() {
        return BcFactory.BcType.BEA91;
    }

    @Override
    public int maxBaseNum() {
        return btgConfig.maxBaseNum();
    }

    @Override
    public EnvType getEnvType() {
        return btgConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (btgConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = btgConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea91BcConfig> {
        /**
         * BTG协议配置项
         */
        private BtgConfig btgConfig;

        public Builder() {
            btgConfig = BtgFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setBtgConfig(BtgConfig btgConfig) {
            this.btgConfig = btgConfig;
            return this;
        }

        @Override
        public Bea91BcConfig build() {
            return new Bea91BcConfig(this);
        }
    }
}
