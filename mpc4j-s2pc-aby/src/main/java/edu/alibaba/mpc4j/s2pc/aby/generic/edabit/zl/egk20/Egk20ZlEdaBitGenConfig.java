package edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.egk20;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.ZlEdaBitGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.generic.edabit.zl.ZlEdaBitGenFactory;

/**
 * EGK+20 semi-honest Zl edaBit generation config.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public class Egk20ZlEdaBitGenConfig implements ZlEdaBitGenConfig {
    /**
     * Zl circuit config
     */
    private final ZlcConfig zlcConfig;
    /**
     * Z2 circuit config
     */
    private final Z2cConfig z2cConfig;

    private Egk20ZlEdaBitGenConfig(Builder builder) {
        Preconditions.checkArgument(builder.zlcConfig.getSecurityModel().equals(SecurityModel.SEMI_HONEST));
        Preconditions.checkArgument(builder.z2cConfig.getSecurityModel().equals(SecurityModel.SEMI_HONEST));
        zlcConfig = builder.zlcConfig;
        z2cConfig = builder.z2cConfig;
    }

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    @Override
    public ZlEdaBitGenFactory.ZlEdaBitGenType getPtoType() {
        return ZlEdaBitGenFactory.ZlEdaBitGenType.EGK20;
    }

    @Override
    public Zl getZl() {
        return zlcConfig.getZl();
    }

    @Override
    public void setEnvType(EnvType envType) {
        zlcConfig.setEnvType(envType);
        z2cConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return zlcConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Egk20ZlEdaBitGenConfig> {
        /**
         * Zl circuit config
         */
        private ZlcConfig zlcConfig;
        /**
         * Z2 circuit config
         */
        private Z2cConfig z2cConfig;

        public Builder(Zl zl, boolean silent) {
            zlcConfig = ZlcFactory.createDefaultConfig(zl);
            z2cConfig = Z2cFactory.createDefaultConfig(silent);
        }

        public Builder setZlcConfig(ZlcConfig zlcConfig) {
            this.zlcConfig = zlcConfig;
            return this;
        }

        public Builder setBcConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        @Override
        public Egk20ZlEdaBitGenConfig build() {
            return new Egk20ZlEdaBitGenConfig(this);
        }
    }
}
