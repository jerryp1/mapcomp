package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.tsorting;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
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
 * Trivial sorting-based group aggregation config.
 *
 * @author Li Peng
 * @date 2023/11/19
 */
public class TrivialSortingGroupAggConfig extends AbstractMultiPartyPtoConfig implements GroupAggConfig {
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
     * Permutation config.
     */
    private final PermutationConfig permutationConfig;
    /**
     * A2b config
     */
    private final A2bConfig a2bConfig;
    /**
     * Plain payload mux config.
     */
    private final PlainPayloadMuxConfig plainPayloadMuxConfig;
    /**
     * Zl zl.
     */
    private final Zl zl;

    private TrivialSortingGroupAggConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.zlMuxConfig,
            builder.sharedPermutationConfig, builder.z2cConfig,
            builder.zlcConfig, builder.permutationConfig,
            builder.a2bConfig, builder.plainPayloadMuxConfig);
        this.zlMuxConfig = builder.zlMuxConfig;
        this.sharedPermutationConfig = builder.sharedPermutationConfig;
        this.prefixAggConfig = builder.prefixAggConfig;
        this.z2cConfig = builder.z2cConfig;
        this.zlcConfig = builder.zlcConfig;
        this.permutationConfig = builder.permutationConfig;
        this.plainPayloadMuxConfig = builder.plainPayloadMuxConfig;
        this.a2bConfig = builder.a2bConfig;
        this.zl = builder.zl;
    }

    @Override
    public GroupAggTypes getPtoType() {
        return GroupAggTypes.T_SORTING;
    }

    @Override
    public boolean isReverse() {
        return false;
    }

    public ZlMuxConfig getZlMuxConfig() {
        return zlMuxConfig;
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

    public A2bConfig getA2bConfig() {
        return a2bConfig;
    }

    public PermutationConfig getPermutationConfig() {
        return permutationConfig;
    }

    public PlainPayloadMuxConfig getPlainPayloadMuxConfig() {
        return plainPayloadMuxConfig;
    }

    @Override
    public PrefixAggTypes getAggType() {
        return prefixAggConfig.getPrefixType();
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<TrivialSortingGroupAggConfig> {
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
         * Permutation config.
         */
        private final PermutationConfig permutationConfig;
        /**
         * Plain payload mux config.
         */
        private final PlainPayloadMuxConfig plainPayloadMuxConfig;
        /**
         * A2b config
         */
        private final A2bConfig a2bConfig;
        /**
         * Zl
         */
        private final Zl zl;

        public Builder(Zl zl, boolean silent, PrefixAggTypes type) {
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            sharedPermutationConfig = SharedPermutationFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            prefixAggConfig = PrefixAggFactory.createDefaultPrefixAggConfig(SecurityModel.SEMI_HONEST, zl, silent, type, true);
            permutationConfig = PermutationFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            plainPayloadMuxConfig = PlainPlayloadMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            a2bConfig = A2bFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            this.zl = zl;
        }

        @Override
        public TrivialSortingGroupAggConfig build() {
            return new TrivialSortingGroupAggConfig(this);
        }
    }
}
