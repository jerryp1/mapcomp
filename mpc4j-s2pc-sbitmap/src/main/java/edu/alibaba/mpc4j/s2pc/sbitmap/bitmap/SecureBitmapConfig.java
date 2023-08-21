package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapSecurityMode;

/**
 * Secure Bitmap Config.
 * @author Li Peng
 * @date 2023/8/16
 */
public class SecureBitmapConfig implements MultiPartyPtoConfig {
    /**
     * Z2 circuit config.
     */
    private Z2cConfig z2cConfig;
    /**
     * secure mode.
     */
    private SbitmapSecurityMode securityMode;
    /**
     * window size.
     */
    private int w;
    /**
     * window num.
     */
    private int windowNum;
    /**
     * total bit num.
     */
    private int totalBitNum;
    /**
     * env type.
     */
    private EnvType envType;

    private SecureBitmapConfig(SecureBitmapConfig.Builder builder) {
        this.z2cConfig = builder.z2cConfig;
        this.w = builder.w;
        this.windowNum = builder.windowNum;
        this.securityMode = builder.securityMode;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public SbitmapSecurityMode getSecurityMode() {
        return securityMode;
    }

    public int getW() {
        return w;
    }

    public int getWindowNum() {
        return windowNum;
    }

    public int getTotalBitNum() {
        return totalBitNum;
    }

    @Override
    public void setEnvType(EnvType envType) {
        this.envType = envType;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SecureBitmapConfig> {
        /**
         * Z2 circuit config.
         */
        private Z2cConfig z2cConfig;
        /**
         * Secure mode.
         */
        private SbitmapSecurityMode securityMode;
        /**
         * window size
         */
        private int w;
        /**
         * total bit num.
         */
        private int windowNum;
        /**
         * total bit num.
         */
        private int totalBitNum;

        public Builder(SbitmapSecurityMode securityMode, int w, int windowNum, int totalbitNum) {
            this.securityMode = securityMode;
            this.w = w;
            this.windowNum = windowNum;
            this.totalBitNum = totalbitNum;
            this.z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public SecureBitmapConfig.Builder setZ2cConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        @Override
        public SecureBitmapConfig build() {
            return new SecureBitmapConfig(this);
        }
    }
}
