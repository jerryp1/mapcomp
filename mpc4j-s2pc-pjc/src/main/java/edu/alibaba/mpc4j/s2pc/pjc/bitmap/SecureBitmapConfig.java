package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingConfig;
import edu.alibaba.mpc4j.s2pc.aby.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapPtoDesc.BitmapType;


/**
 * Liu22 bitmap协议配置项。
 *
 * @author Li Peng  
 * @date 2022/11/24
 */
public class SecureBitmapConfig implements BitmapConfig {
    /**
     * BC协议
     */
    private final BcConfig bcConfig;
    /**
     * 汉明计算协议
     */
    private final HammingConfig hammingConfig;

    private SecureBitmapConfig(Builder builder) {
        bcConfig = builder.bcConfig;
        hammingConfig = builder.hammingConfig;
    }

    @Override
    public BitmapType getPtoType() {
        return BitmapType.BITMAP;
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

    @Override
    public BcConfig getBcConfig() {
        return bcConfig;
    }

    @Override
    public HammingConfig getHammingConfig() {
        return hammingConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SecureBitmapConfig> {
        /**
         * BTG协议配置项
         */
        private BcConfig bcConfig;
        /**
         * 汉明距离计算配置项
         */
        private HammingConfig hammingConfig;

        public Builder() {
            bcConfig = BcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            hammingConfig = HammingFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setBcConfig(BcConfig bcConfig) {
            this.bcConfig = bcConfig;
            return this;
        }

        public Builder setHammingConfig(HammingConfig hammingConfig) {
            this.hammingConfig = hammingConfig;
            return this;
        }

        @Override
        public SecureBitmapConfig build() {
            return new SecureBitmapConfig(this);
        }
    }
}
