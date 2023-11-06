package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapSecurityMode;

/**
 * Secure Bitmap Config.
 *
 * @author Li Peng
 * @date 2023/8/16
 */
public class SecureBitmapConfig implements MultiPartyPtoConfig {
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * secure mode.
     */
    private final SbitmapSecurityMode securityMode;
    /**
     * container size.
     */
    private final int containerSize;
    /**
     * container num.
     */
    private final int containerNum;
    /**
     * total bit num.
     */
    private final int totalBitNum;
    /**
     * env type.
     */
    private EnvType envType;
    /**
     * party id, 0 for sender, 1 for receiver.
     */
    private final int partyId;
    /**
     * privacy budget
     */
    private double epsilon;

    private SecureBitmapConfig(SecureBitmapConfig.Builder builder) {
        this.z2cConfig = builder.z2cConfig;
        this.containerSize = builder.containerSize;
        this.containerNum = builder.containerNum;
        this.securityMode = builder.securityMode;
        this.totalBitNum = builder.totalBitNum;
        this.partyId = builder.partyId;
        this.epsilon = builder.epsilon;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public SbitmapSecurityMode getSecurityMode() {
        return securityMode;
    }

    public int getContainerSize() {
        return containerSize;
    }

    public int getContainerNum() {
        return containerNum;
    }

    public int getTotalBitNum() {
        return totalBitNum;
    }

    public int getPartyId() {
        return partyId;
    }

    public double getEpsilon() {
        return epsilon;
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
         * container size
         */
        private final int containerSize;
        /**
         * container num.
         */
        private final int containerNum;
        /**
         * total bit num.
         */
        private final int totalBitNum;
        /**
         * party id, 0 for sender, 1 for receiver.
         */
        private final int partyId;
        /**
         * privacy budget
         */
        private double epsilon;

        public Builder(int containerSize, int containerNum, int totalBitNum, int partyId, double epsilon) {
            this.containerSize = containerSize;
            this.containerNum = containerNum;
            this.totalBitNum = totalBitNum;
            this.z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
            this.partyId = partyId;
            this.securityMode = SbitmapSecurityMode.FULL_SECURE;
            this.epsilon = epsilon;
        }

        public SecureBitmapConfig.Builder setZ2cConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        public void setSecurityMode(SbitmapSecurityMode securityMode) {
            this.securityMode = securityMode;
        }

        @Override
        public SecureBitmapConfig build() {
            return new SecureBitmapConfig(this);
        }
    }
}
