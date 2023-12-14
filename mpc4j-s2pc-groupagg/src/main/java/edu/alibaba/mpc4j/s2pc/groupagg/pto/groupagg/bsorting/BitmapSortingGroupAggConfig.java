package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.bsorting;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.bitmap.BitmapPermGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.GroupAggFactory.GroupAggTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;

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
    private final Z2MuxConfig z2MuxConfig;

    private BitmapSortingGroupAggConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.osnConfig,
            builder.sharedPermutationConfig, builder.z2cConfig,
            builder.zlcConfig,
            builder.reversePermutationConfig, builder.permutationConfig,
            builder.permGenConfig, builder.a2bConfig);
        this.osnConfig = builder.osnConfig;
        this.sharedPermutationConfig = builder.sharedPermutationConfig;
        this.prefixAggConfig = builder.prefixAggConfig;
        this.z2cConfig = builder.z2cConfig;
        this.zlcConfig = builder.zlcConfig;
        this.reversePermutationConfig = builder.reversePermutationConfig;
        this.permutationConfig = builder.permutationConfig;
        this.permGenConfig = builder.permGenConfig;
        this.a2bConfig = builder.a2bConfig;
        this.zl = builder.zl;
        z2MuxConfig = builder.z2MuxConfig;
    }

    @Override
    public GroupAggTypes getPtoType() {
        return GroupAggTypes.B_SORTING;
    }

    @Override
    public boolean isReverse() {
        return false;
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

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
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

    public Z2MuxConfig getZ2MuxConfig() {
        return z2MuxConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BitmapSortingGroupAggConfig> {
        /**
         * Osn config.
         */
        private final OsnConfig osnConfig;
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
        private final Z2MuxConfig z2MuxConfig;

        public Builder(Zl zl, boolean silent, PrefixAggTypes type) {
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            sharedPermutationConfig = SharedPermutationFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            prefixAggConfig = PrefixAggFactory.createDefaultPrefixAggConfig(SecurityModel.SEMI_HONEST, zl, silent, type, true);
            reversePermutationConfig = PermutationFactory.createDefaultReverseConfig(SecurityModel.SEMI_HONEST, zl, silent);
            permutationConfig = PermutationFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            permGenConfig = new BitmapPermGenConfig.Builder(zl, silent).build();
            a2bConfig = A2bFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            this.zl = zl;
            z2MuxConfig = Z2MuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        @Override
        public BitmapSortingGroupAggConfig build() {
            return new BitmapSortingGroupAggConfig(this);
        }
    }
}
