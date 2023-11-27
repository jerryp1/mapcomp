package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.bitmap;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenFactory.FieldTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenFactory.PermGenTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;

public class BitmapPermGenConfig extends AbstractMultiPartyPtoConfig implements PermGenConfig {
    /**
     * bit2a config.
     */
    private final Bit2aConfig bit2aConfig;
    /**
     * Zl circuit config.
     */
    private final ZlcConfig zlcConfig;
    /**
     * Zl circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * Zl mux config.
     */
    private final ZlMuxConfig zlMuxConfig;

    private BitmapPermGenConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bit2aConfig, builder.zlcConfig, builder.zlMuxConfig);
        bit2aConfig = builder.bit2aConfig;
        zlcConfig = builder.zlcConfig;
        z2cConfig = builder.z2cConfig;
        zlMuxConfig = builder.zlMuxConfig;
    }

    @Override
    public PermGenTypes getPtoType() {
        return PermGenTypes.AHI22_BITMAP;
    }

    @Override
    public Zl getZl() {
        return zlcConfig.getZl();
    }

    @Override
    public FieldTypes getFieldType() {
        return FieldTypes.SMALL_FIELD;
    }

    public Bit2aConfig getBit2aConfig() {
        return bit2aConfig;
    }

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public ZlMuxConfig getZlMuxConfig() {
        return zlMuxConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BitmapPermGenConfig> {
        /**
         * bit2a config.
         */
        private final Bit2aConfig bit2aConfig;
        /**
         * Zl circuit config.
         */
        private ZlcConfig zlcConfig;
        /**
         * Zl circuit config.
         */
        private Z2cConfig z2cConfig;
        /**
         * Zl mux config.
         */
        private ZlMuxConfig zlMuxConfig;

        public Builder(Zl zl) {
            this.bit2aConfig = Bit2aFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public Builder setZlcConfig(ZlcConfig zlcConfig) {
            this.zlcConfig = zlcConfig;
            return this;
        }

        public Builder setSilent(boolean silent){
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            return this;
        }

        @Override
        public BitmapPermGenConfig build() {
            return new BitmapPermGenConfig(this);
        }
    }
}
