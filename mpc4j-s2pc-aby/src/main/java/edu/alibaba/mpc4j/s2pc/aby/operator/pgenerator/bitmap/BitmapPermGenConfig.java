package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.bitmap;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aConfig;
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
     * Zl mux config.
     */
    private final ZlMuxConfig zlMuxConfig;

    private BitmapPermGenConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bit2aConfig, builder.zlcConfig, builder.zlMuxConfig);
        bit2aConfig = builder.bit2aConfig;
        zlcConfig = builder.zlcConfig;
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
         * Zl mux config.
         */
        private ZlMuxConfig zlMuxConfig;

        public Builder(Bit2aConfig bit2aConfig) {
            this.bit2aConfig = bit2aConfig;
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, bit2aConfig.getZl());
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public Builder setZlcConfig(ZlcConfig zlcConfig) {
            this.zlcConfig = zlcConfig;
            return this;
        }

        public Builder setSilent(boolean silent){
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            return this;
        }

        @Override
        public BitmapPermGenConfig build() {
            return new BitmapPermGenConfig(this);
        }
    }
}
