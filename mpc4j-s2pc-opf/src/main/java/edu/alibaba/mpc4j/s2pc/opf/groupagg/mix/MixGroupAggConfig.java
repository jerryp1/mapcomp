package edu.alibaba.mpc4j.s2pc.opf.groupagg.mix;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
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
 * Mix group aggregation config.
 *
 * @author Li Peng
 * @date 2023/11/3
 */
public class MixGroupAggConfig extends AbstractMultiPartyPtoConfig implements GroupAggConfig {
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
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * Zl circuit config.
     */
    private final ZlcConfig zlcConfig;
    /**
     * Prefix aggregate config.
     */
    private final PrefixAggConfig prefixAggConfig;

    private final Z2MuxConfig z2MuxConfig;
    /**
     * Zl
     */
    private final Zl zl;

    private MixGroupAggConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.osnConfig, builder.zlMuxConfig,
            builder.plainPayloadMuxConfig, builder.prefixAggConfig, builder.z2cConfig,
            builder.zlcConfig, builder.z2MuxConfig);
        this.osnConfig = builder.osnConfig;
        this.z2cConfig = builder.z2cConfig;
        this.zlMuxConfig = builder.zlMuxConfig;
        this.plainPayloadMuxConfig = builder.plainPayloadMuxConfig;
        this.prefixAggConfig = builder.prefixAggConfig;
        this.zlcConfig = builder.zlcConfig;
        this.z2MuxConfig = builder.z2MuxConfig;
        this.zl = builder.zl;
    }

    @Override
    public GroupAggTypes getPtoType() {
        return GroupAggTypes.MIX;
    }

    @Override
    public boolean isReverse() {
        return false;
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

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
    }

    public Z2MuxConfig getZ2MuxConfig() {
        return z2MuxConfig;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public PrefixAggTypes getAggType() {
        return prefixAggConfig.getPrefixType();
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<MixGroupAggConfig> {
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
         * Z2 circuit config.
         */
        private final Z2cConfig z2cConfig;
        /**
         * Zl circuit config.
         */
        private final ZlcConfig zlcConfig;
        /**
         * Prefix aggregate config.
         */
        private final PrefixAggConfig prefixAggConfig;
        private final Z2MuxConfig z2MuxConfig;
        /**
         * Zl
         */
        private final Zl zl;

        public Builder(Zl zl, boolean silent, PrefixAggTypes type) {
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            plainPayloadMuxConfig = PlainPlayloadMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            prefixAggConfig = PrefixAggFactory.createDefaultPrefixAggConfig(SecurityModel.SEMI_HONEST, zl, silent, type, true);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            z2MuxConfig = Z2MuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            this.zl = zl;
        }

        @Override
        public MixGroupAggConfig build() {
            return new MixGroupAggConfig(this);
        }
    }
}
