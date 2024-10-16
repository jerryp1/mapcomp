package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bitmap;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.PlainAndConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.PlainAndFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggFactory.GroupAggTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;

/**
 * Bitmap group aggregation config.
 *
 */
public class BitmapGroupAggConfig extends AbstractMultiPartyPtoConfig implements GroupAggConfig {
    /**
     * Zl mux config.
     */
    private final ZlMuxConfig zlMuxConfig;
    /**
     * Prefix aggregate config.
     */
    private final PrefixAggConfig prefixAggConfig;
    /**
     * Zl circuit config.
     */
    private final ZlcConfig zlcConfig;
    /**
     * Z2 circuit party.
     */
    private final Z2cConfig z2cConfig;
    /**
     * Plain and config.
     */
    private final PlainAndConfig plainAndConfig;
    /**
     * Zl max config.
     */
    private final ZlMaxConfig zlMaxConfig;
    private PlainPayloadMuxConfig plainPayloadMuxConfig;
    /**
     * Zl
     */
    private final Zl zl;
    private final Z2MuxConfig z2MuxConfig;

    private BitmapGroupAggConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.zlMuxConfig,
            builder.prefixAggConfig, builder.zlcConfig,
            builder.plainAndConfig, builder.zlMaxConfig,
            builder.z2cConfig, builder.plainPayloadMuxConfig);
        this.zlMuxConfig = builder.zlMuxConfig;
        this.prefixAggConfig = builder.prefixAggConfig;
        this.zlcConfig = builder.zlcConfig;
        this.plainAndConfig = builder.plainAndConfig;
        this.zlMaxConfig = builder.zlMaxConfig;
        this.z2cConfig = builder.z2cConfig;
        this.plainPayloadMuxConfig = builder.plainPayloadMuxConfig;
        this.zl = builder.zl;
        this.z2MuxConfig = builder.z2MuxConfig;
    }

    @Override
    public GroupAggTypes getPtoType() {
        return GroupAggTypes.BITMAP;
    }

    @Override
    public boolean isReverse() {
        return false;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public PrefixAggTypes getAggType() {
        return prefixAggConfig.getPrefixType();
    }

    public ZlMuxConfig getZlMuxConfig() {
        return zlMuxConfig;
    }

    public PrefixAggConfig getPrefixAggConfig() {
        return prefixAggConfig;
    }

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
    }

    public PlainAndConfig getPlainAndConfig() {
        return plainAndConfig;
    }

    public ZlMaxConfig getZlMaxConfig() {
        return zlMaxConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public PlainPayloadMuxConfig getPlainPayloadMuxConfig() {
        return plainPayloadMuxConfig;
    }

    public Z2MuxConfig getZ2MuxConfig() {
        return z2MuxConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BitmapGroupAggConfig> {
        /**
         * Zl mux config.
         */
        private final ZlMuxConfig zlMuxConfig;
        /**
         * Prefix aggregate config.
         */
        private final PrefixAggConfig prefixAggConfig;
        /**
         * Z2 circuit party.
         */
        private final Z2cConfig z2cConfig;
        /**
         * Zl circuit config.
         */
        private final ZlcConfig zlcConfig;
        /**
         * Plain and config.
         */
        private final PlainAndConfig plainAndConfig;
        /**
         * Zl max config.
         */
        private final ZlMaxConfig zlMaxConfig;
        private PlainPayloadMuxConfig plainPayloadMuxConfig;
        /**
         * Zl
         */
        private final Zl zl;
        private final Z2MuxConfig z2MuxConfig;

        public Builder(Zl zl, boolean silent, PrefixAggTypes type) {
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            prefixAggConfig = PrefixAggFactory.createDefaultPrefixAggConfig(SecurityModel.SEMI_HONEST, zl, silent, type, true);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            plainAndConfig = PlainAndFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            zlMaxConfig = ZlMaxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent, zl);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            plainPayloadMuxConfig = PlainPlayloadMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            z2MuxConfig = Z2MuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            this.zl = zl;
        }

        @Override
        public BitmapGroupAggConfig build() {
            return new BitmapGroupAggConfig(this);
        }
    }
}
