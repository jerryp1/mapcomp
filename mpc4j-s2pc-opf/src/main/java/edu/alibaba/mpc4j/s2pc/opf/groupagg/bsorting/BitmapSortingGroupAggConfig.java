package edu.alibaba.mpc4j.s2pc.opf.groupagg.bsorting;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.tuple.TupleB2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.bitmap.BitmapPermGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggFactory.GroupAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;

/**
 * Bitmap assist sorting-based group aggregation config.
 *
 * @author Li Peng
 * @date 2023/11/20
 */
public class BitmapSortingGroupAggConfig extends AbstractMultiPartyPtoConfig implements GroupAggConfig {
    /**
     * Osn config.
     */
    private final OsnConfig osnConfig;
    /**
     * Zl mux config.
     */
    private final ZlMuxConfig zlMuxConfig;
    /**
     * Shared permutation config.
     */
    private final SharedPermutationConfig sharedPermutationConfig;
    /**
     * Prefix aggregation config.
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
     * B2a config.
     */
    private final B2aConfig b2aConfig;
    /**
     * Plain bit mux config.
     */
    private final PlainPayloadMuxConfig plainPayloadMuxConfig;
    /**
     * Reverse permutation config.
     */
    private final PermutationConfig reversePermutationConfig;
    /**
     * Permutation config.
     */
    private final PermutationConfig permutationConfig;
    /**
     * Permutation generation protocol config.
     */
    private final PermGenConfig permGenConfig;
    /**
     * A2b config.
     */
    private final A2bConfig a2bConfig;
    /**
     * Zl
     */
    private final Zl zl;

    private BitmapSortingGroupAggConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.osnConfig, builder.zlMuxConfig,
            builder.sharedPermutationConfig, builder.z2cConfig,
            builder.zlcConfig, builder.b2aConfig, builder.plainPayloadMuxConfig,
            builder.reversePermutationConfig, builder.permutationConfig,
            builder.permGenConfig, builder.a2bConfig);
        this.osnConfig = builder.osnConfig;
        this.zlMuxConfig = builder.zlMuxConfig;
        this.sharedPermutationConfig = builder.sharedPermutationConfig;
        this.prefixAggConfig = builder.prefixAggConfig;
        this.z2cConfig = builder.z2cConfig;
        this.zlcConfig = builder.zlcConfig;
        this.b2aConfig = builder.b2aConfig;
        this.plainPayloadMuxConfig = builder.plainPayloadMuxConfig;
        this.reversePermutationConfig = builder.reversePermutationConfig;
        this.permutationConfig = builder.permutationConfig;
        this.permGenConfig = builder.permGenConfig;
        this.a2bConfig = builder.a2bConfig;
        this.zl = builder.zl;
    }

    @Override
    public GroupAggTypes getPtoType() {
        return GroupAggTypes.B_SORTING;
    }

    @Override
    public boolean isReverse() {
        return false;
    }

    public ZlMuxConfig getZlMuxConfig() {
        return zlMuxConfig;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public SharedPermutationConfig getSharedPermutationConfig() {
        return sharedPermutationConfig;
    }

    public PrefixAggConfig getPrefixAggConfig() {
        return prefixAggConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public B2aConfig getB2aConfig() {
        return b2aConfig;
    }

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
    }

    public PlainPayloadMuxConfig getPlainPayloadMuxConfig() {
        return plainPayloadMuxConfig;
    }

    public PermutationConfig getReversePermutationConfig() {
        return reversePermutationConfig;
    }

    public PermutationConfig getPermutationConfig() {
        return permutationConfig;
    }

    public PermGenConfig getPermGenConfig() {
        return permGenConfig;
    }

    public A2bConfig getA2bConfig() {
        return a2bConfig;
    }

    @Override
    public PrefixAggTypes getAggType() {
        return prefixAggConfig.getPrefixType();
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BitmapSortingGroupAggConfig> {
        /**
         * Osn config.
         */
        private final OsnConfig osnConfig;
        /**
         * Zl mux config.
         */
        private final ZlMuxConfig zlMuxConfig;
        /**
         * Shared permutation config.
         */
        private final SharedPermutationConfig sharedPermutationConfig;
        /**
         * Prefix aggregation config.
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
         * B2a config.
         */
        private final B2aConfig b2aConfig;
        /**
         * Plain bit mux config.
         */
        private final PlainPayloadMuxConfig plainPayloadMuxConfig;
        /**
         * Permutation config.
         */
        private final PermutationConfig reversePermutationConfig;
        /**
         * Permutation config.
         */
        private final PermutationConfig permutationConfig;
        /**
         * Permutation generation protocol config.
         */
        private final PermGenConfig permGenConfig;
        /**
         * A2b config.
         */
        private final A2bConfig a2bConfig;
        /**
         * Zl
         */
        private final Zl zl;

        public Builder(Zl zl, boolean silent, PrefixAggTypes type) {
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            sharedPermutationConfig = SharedPermutationFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            b2aConfig = new TupleB2aConfig.Builder(zl, silent).build();
            plainPayloadMuxConfig = PlainPlayloadMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            prefixAggConfig = PrefixAggFactory.createDefaultPrefixAggConfig(SecurityModel.SEMI_HONEST, zl, silent, type, true);
            reversePermutationConfig = PermutationFactory.createDefaultReverseConfig(SecurityModel.SEMI_HONEST, zl, silent);
            permutationConfig = PermutationFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            permGenConfig = new BitmapPermGenConfig.Builder(zl, silent).build();
            a2bConfig = A2bFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            this.zl = zl;
        }

        @Override
        public BitmapSortingGroupAggConfig build() {
            return new BitmapSortingGroupAggConfig(this);
        }
    }
}
