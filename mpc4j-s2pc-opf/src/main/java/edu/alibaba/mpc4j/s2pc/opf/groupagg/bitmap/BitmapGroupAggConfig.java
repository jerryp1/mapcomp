package edu.alibaba.mpc4j.s2pc.opf.groupagg.bitmap;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.PlainAndConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.PlainAndFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggFactory.GroupAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;

/**
 * Bitmap group aggregation config.
 *
 * @author Li Peng
 * @date 2023/11/8
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

        public Builder(Zl zl, boolean silent, PrefixAggTypes type) {
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            prefixAggConfig = PrefixAggFactory.createDefaultPrefixAggConfig(SecurityModel.SEMI_HONEST, zl, silent, type, true);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            plainAndConfig = PlainAndFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            zlMaxConfig = ZlMaxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent, zl);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            plainPayloadMuxConfig = PlainPlayloadMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST,zl,silent);
            this.zl = zl;
        }

        @Override
        public BitmapGroupAggConfig build() {
            return new BitmapGroupAggConfig(this);
        }
    }
}
