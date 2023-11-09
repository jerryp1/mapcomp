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
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.PlainAndConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.PlainAndFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggFactory.GroupAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
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
     * Osn config.
     */
    private final OsnConfig osnConfig;
    /**
     * Zl mux config.
     */
    private final ZlMuxConfig zlMuxConfig;
    /**
     * Plain payload mux config.
     */
    private final PlainPayloadMuxConfig plainPayloadMuxConfig;
    /**
     * Plain bit mux config.
     */
    private final PlainBitMuxConfig plainBitMuxConfig;
    /**
     * Prefix aggregate config.
     */
    private final PrefixAggConfig prefixAggConfig;
    /**
     * Z2 circuit config.
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
    /**
     * Zl
     */
    private Zl zl;

    private BitmapGroupAggConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.osnConfig, builder.zlMuxConfig,
            builder.plainPayloadMuxConfig, builder.plainBitMuxConfig,
            builder.prefixAggConfig, builder.z2cConfig, builder.zlcConfig,
            builder.plainAndConfig, builder.zlMaxConfig);
        this.osnConfig = builder.osnConfig;
        this.zlMuxConfig = builder.zlMuxConfig;
        this.plainPayloadMuxConfig = builder.plainPayloadMuxConfig;
        this.plainBitMuxConfig = builder.plainBitMuxConfig;
        this.prefixAggConfig = builder.prefixAggConfig;
        this.z2cConfig = builder.z2cConfig;
        this.zlcConfig = builder.zlcConfig;
        this.plainAndConfig = builder.plainAndConfig;
        this.zlMaxConfig = builder.zlMaxConfig;
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

    public PlainBitMuxConfig getPlainBitMuxConfig() {
        return plainBitMuxConfig;
    }

    public PlainPayloadMuxConfig getPlainPayloadMuxConfig() {
        return plainPayloadMuxConfig;
    }

    public ZlMuxConfig getZlMuxConfig() {
        return zlMuxConfig;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public PrefixAggConfig getPrefixAggConfig() {
        return prefixAggConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
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


    public static class Builder implements org.apache.commons.lang3.builder.Builder<BitmapGroupAggConfig> {
        /**
         * Osn config.
         */
        private final OsnConfig osnConfig;
        /**
         * Zl mux config.
         */
        private final ZlMuxConfig zlMuxConfig;
        /**
         * Plain mux config.
         */
        private final PlainPayloadMuxConfig plainPayloadMuxConfig;
        /**
         * Plain bit mux config.
         */
        private final PlainBitMuxConfig plainBitMuxConfig;
        /**
         * Prefix aggregate config.
         */
        private final PrefixAggConfig prefixAggConfig;
        /**
         * Z2 circuit config.
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
        /**
         * Zl
         */
        private final Zl zl;

        public Builder(Zl zl, boolean silent, PrefixAggTypes type) {
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            plainPayloadMuxConfig = PlainPlayloadMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            plainBitMuxConfig = PlainBitMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            prefixAggConfig = PrefixAggFactory.createDefaultPrefixAggConfig(SecurityModel.SEMI_HONEST, zl, silent, type);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            plainAndConfig = PlainAndFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            zlMaxConfig = ZlMaxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent, zl);
            this.zl = zl;
        }

        @Override
        public BitmapGroupAggConfig build() {
            return new BitmapGroupAggConfig(this);
        }
    }
}
