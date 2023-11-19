package edu.alibaba.mpc4j.s2pc.opf.groupagg.sorting;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggFactory.GroupAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.PrefixMaxFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum.PrefixSumFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;

/**
 * Sorting-based group aggregation config.
 *
 * @author Li Peng
 * @date 2023/11/3
 */
public class SortingGroupAggConfig extends AbstractMultiPartyPtoConfig implements GroupAggConfig {
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

    private final Zl zl;

    private SortingGroupAggConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.osnConfig, builder.zlMuxConfig,
            builder.sharedPermutationConfig, builder.z2cConfig,
            builder.zlcConfig, builder.b2aConfig);
        this.osnConfig = builder.osnConfig;
        this.zlMuxConfig = builder.zlMuxConfig;
        this.sharedPermutationConfig = builder.sharedPermutationConfig;
        this.prefixAggConfig = builder.prefixAggConfig;
        this.z2cConfig = builder.z2cConfig;
        this.zlcConfig = builder.zlcConfig;
        this.b2aConfig = builder.b2aConfig;
        this.zl = builder.zl;
    }

    @Override
    public GroupAggTypes getPtoType() {
        return GroupAggTypes.SORTING;
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

    @Override
    public PrefixAggTypes getAggType() {
        return prefixAggConfig.getPrefixType();
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SortingGroupAggConfig> {
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
         * Zl
         */
        private final Zl zl;

        public Builder(Zl zl, boolean silent, PrefixAggTypes type) {
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            sharedPermutationConfig = SharedPermutationFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            b2aConfig = B2aFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            this.zl = zl;
            switch (type) {
                case SUM:
                    prefixAggConfig = PrefixSumFactory.createDefaultPrefixSumConfig(SecurityModel.SEMI_HONEST, zl, silent);
                    break;
                case MAX:
                    prefixAggConfig = PrefixMaxFactory.createDefaultPrefixMaxConfig(SecurityModel.SEMI_HONEST, zl, silent);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid " + PrefixAggTypes.class.getSimpleName() + ": " + type.name());
            }
        }

        @Override
        public SortingGroupAggConfig build() {
            return new SortingGroupAggConfig(this);
        }
    }
}
