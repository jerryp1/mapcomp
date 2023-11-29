package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.smallfield.ahi22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenFactory.FieldTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenFactory.PermGenTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;

/**
 * Ahi22 Permutable Sorter Config.
 *
 * @author Li Peng
 * @date 2023/10/11
 */
public class Ahi22SmallFieldPermGenConfig extends AbstractMultiPartyPtoConfig implements PermGenConfig {
    /**
     * bit2a config.
     */
    private final Bit2aConfig bit2aConfig;
    /**
     * Zl circuit config.
     */
    private final ZlcConfig zlcConfig;
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * Zl mux config.
     */
    private final ZlMuxConfig zlMuxConfig;

    private Ahi22SmallFieldPermGenConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bit2aConfig, builder.zlcConfig, builder.zlMuxConfig);
        bit2aConfig = builder.bit2aConfig;
        zlcConfig = builder.zlcConfig;
        z2cConfig = builder.z2cConfig;
        zlMuxConfig = builder.zlMuxConfig;
    }

    @Override
    public PermGenTypes getPtoType() {
        return PermGenTypes.AHI22_SMALL_FIELD;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ahi22SmallFieldPermGenConfig> {
        /**
         * bit2a config.
         */
        private final Bit2aConfig bit2aConfig;
        /**
         * Zl circuit config.
         */
        private ZlcConfig zlcConfig;
        /**
         * Z2 circuit config.
         */
        private Z2cConfig z2cConfig;
        /**
         * Zl mux config.
         */
        private ZlMuxConfig zlMuxConfig;

        public Builder(Bit2aConfig bit2aConfig, boolean silent) {
            this.bit2aConfig = bit2aConfig;
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, bit2aConfig.getZl());
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        public Builder setZlcConfig(ZlcConfig zlcConfig) {
            this.zlcConfig = zlcConfig;
            return this;
        }

        public Builder setZ2cConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        public Builder setSilent(boolean silent){
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            return this;
        }

        @Override
        public Ahi22SmallFieldPermGenConfig build() {
            return new Ahi22SmallFieldPermGenConfig(this);
        }
    }
}
