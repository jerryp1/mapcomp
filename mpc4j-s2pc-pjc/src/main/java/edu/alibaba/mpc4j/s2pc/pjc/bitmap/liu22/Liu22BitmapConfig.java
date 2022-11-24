package edu.alibaba.mpc4j.s2pc.pjc.bitmap.liu22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapConfig;

import static edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapFactory.BitmapType;

/**
 * Liu22 bitmap协议配置项。
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/24
 */
public class Liu22BitmapConfig implements BitmapConfig {
    /**
     * BC协议
     */
    private final BcConfig bcConfig;

    private Liu22BitmapConfig(Builder builder) {
        bcConfig = builder.bcConfig;
    }

    @Override
    public BitmapType getPtoType() {
        return BitmapType.LIU22;
    }

    @Override
    public int maxBaseNum() {
        return bcConfig.maxBaseNum();
    }

    @Override
    public void setEnvType(EnvType envType) {
        this.bcConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return bcConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (bcConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = bcConfig.getSecurityModel();
        }
        return securityModel;
    }

    public BcConfig getBcConfig() {
        return bcConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Liu22BitmapConfig> {
        /**
         * BTG协议配置项
         */
        private BcConfig bcConfig;

        public Builder() {
            bcConfig = BcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setBcConfig(BcConfig bcConfig) {
            this.bcConfig = bcConfig;
            return this;
        }

        @Override
        public Liu22BitmapConfig build() {
            return new Liu22BitmapConfig(this);
        }
    }
}
