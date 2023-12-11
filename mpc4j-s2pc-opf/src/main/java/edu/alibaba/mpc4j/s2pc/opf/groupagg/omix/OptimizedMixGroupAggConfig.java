package edu.alibaba.mpc4j.s2pc.opf.groupagg.omix;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
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
 * Optimized mix group aggregation config.
 *
 * @author Li Peng
 * @date 2023/11/25
 */
public class OptimizedMixGroupAggConfig extends AbstractMultiPartyPtoConfig implements GroupAggConfig {
    /**
     * Osn config.
     */
    private final OsnConfig osnConfig;
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
    /**
     * One side group config.
     */
    private final OneSideGroupConfig oneSideGroupConfig;
    /**
     * Z2 mux config
     */
    private final Z2MuxConfig z2MuxConfig;
    /**
     * Zl
     */
    private final Zl zl;
    /**
     * Silent
     */
    private final boolean silent;

    private OptimizedMixGroupAggConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.osnConfig,
            builder.plainPayloadMuxConfig, builder.prefixAggConfig, builder.z2cConfig,
            builder.zlcConfig, builder.oneSideGroupConfig, builder.z2MuxConfig);
        this.osnConfig = builder.osnConfig;
        this.z2cConfig = builder.z2cConfig;
        this.plainPayloadMuxConfig = builder.plainPayloadMuxConfig;
        this.prefixAggConfig = builder.prefixAggConfig;
        this.zlcConfig = builder.zlcConfig;
        this.z2MuxConfig = builder.z2MuxConfig;
        oneSideGroupConfig = builder.oneSideGroupConfig;
        this.zl = builder.zl;
        this.silent = builder.silent;
    }

    @Override
    public GroupAggTypes getPtoType() {
        return GroupAggTypes.O_MIX;
    }

    @Override
    public boolean isReverse() {
        return false;
    }

    public PlainPayloadMuxConfig getPlainPayloadMuxConfig() {
        return plainPayloadMuxConfig;
    }


    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public PrefixAggConfig getPrefixAggConfig() {
        return prefixAggConfig;
    }

    public OneSideGroupConfig getOneSideGroupConfig() {
        return oneSideGroupConfig;
    }

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
    }

    public Z2MuxConfig getZ2MuxConfig() {
        return z2MuxConfig;
    }

    public boolean isSilent() {
        return silent;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<OptimizedMixGroupAggConfig> {
        /**
         * Osn config.
         */
        private final OsnConfig osnConfig;
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
        /**
         * One side group config.
         */
        private final OneSideGroupConfig oneSideGroupConfig;
        /**
         * Z2 mux config
         */
        private final Z2MuxConfig z2MuxConfig;
        /**
         * Zl
         */
        private final Zl zl;
        /**
         * Silent
         */
        private final boolean silent;

        public Builder(Zl zl, boolean silent, PrefixAggTypes type) {
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            plainPayloadMuxConfig = PlainPlayloadMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            prefixAggConfig = PrefixAggFactory.createDefaultPrefixAggConfig(SecurityModel.SEMI_HONEST, zl, silent, type, true);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            oneSideGroupConfig = OneSideGroupFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            z2MuxConfig = Z2MuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            this.zl = zl;
            this.silent = silent;
        }

        @Override
        public OptimizedMixGroupAggConfig build() {
            return new OptimizedMixGroupAggConfig(this);
        }
    }
}
