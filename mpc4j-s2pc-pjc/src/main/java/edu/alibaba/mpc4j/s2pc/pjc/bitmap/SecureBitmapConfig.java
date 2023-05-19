package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.HammingConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.HammingFactory;
import edu.alibaba.mpc4j.s2pc.pjc.bitmap.BitmapPtoDesc.BitmapType;


/**
 * Liu22 bitmap协议配置项。
 *
 * @author Li Peng  
 * @date 2022/11/24
 */
public class SecureBitmapConfig implements BitmapConfig {
    /**
     * Z2 circuit config
     */
    private final Z2cConfig z2cConfig;
    /**
     * hamming config
     */
    private final HammingConfig hammingConfig;

    private SecureBitmapConfig(Builder builder) {
        z2cConfig = builder.z2cConfig;
        hammingConfig = builder.hammingConfig;
    }

    @Override
    public BitmapType getPtoType() {
        return BitmapType.BITMAP;
    }

    @Override
    public void setEnvType(EnvType envType) {
        this.z2cConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return z2cConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (z2cConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = z2cConfig.getSecurityModel();
        }
        return securityModel;
    }

    @Override
    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    @Override
    public HammingConfig getHammingConfig() {
        return hammingConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SecureBitmapConfig> {
        /**
         * BTG协议配置项
         */
        private Z2cConfig z2cConfig;
        /**
         * 汉明距离计算配置项
         */
        private HammingConfig hammingConfig;

        public Builder() {
            z2cConfig = Z2cFactory.createDefaultConfig(true);
            hammingConfig = HammingFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public Builder setBcConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
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
