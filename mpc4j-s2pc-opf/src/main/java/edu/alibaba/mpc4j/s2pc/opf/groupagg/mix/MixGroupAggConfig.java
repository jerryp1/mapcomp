package edu.alibaba.mpc4j.s2pc.opf.groupagg.mix;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggConfig;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.GroupAggFactory.GroupAggTypes;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory.ShuffleTypes;

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

    private MixGroupAggConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.osnConfig);
        this.osnConfig = builder.osnConfig;
    }

    @Override
    public GroupAggTypes getPtoType() {
        return GroupAggTypes.MIX;
    }

    @Override
    public boolean isReverse() {
        return false;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<MixGroupAggConfig> {
        /**
         * Osn config.
         */
        private final OsnConfig osnConfig;

        public Builder(boolean silent) {
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        @Override
        public MixGroupAggConfig build() {
            return new MixGroupAggConfig(this);
        }
    }
}
