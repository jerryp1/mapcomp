package edu.alibaba.mpc4j.s2pc.opf.spermutation.php24b;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory.SharedPermutationTypes;

/**
 * Php+24 shared permutation config.
 *
 * @author Li Peng
 * @date 2023/5/25
 */
public class Php24bSharedPermutationConfig extends AbstractMultiPartyPtoConfig implements SharedPermutationConfig {
    /**
     * Shuffle config.
     */
    private final ShuffleConfig shuffleConfig;

    private Php24bSharedPermutationConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.shuffleConfig);
        this.shuffleConfig = builder.shuffleConfig;
    }

    @Override
    public SharedPermutationTypes getPtoType() {
        return SharedPermutationTypes.PHP24B;
    }

    @Override
    public boolean isReverse() {
        return true;
    }

    public ShuffleConfig getShuffleConfig() {
        return shuffleConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Php24bSharedPermutationConfig> {
        /**
         * Shuffle config.
         */
        private final ShuffleConfig shuffleConfig;

        public Builder(boolean silent) {
            shuffleConfig = ShuffleFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        @Override
        public Php24bSharedPermutationConfig build() {
            return new Php24bSharedPermutationConfig(this);
        }
    }
}
