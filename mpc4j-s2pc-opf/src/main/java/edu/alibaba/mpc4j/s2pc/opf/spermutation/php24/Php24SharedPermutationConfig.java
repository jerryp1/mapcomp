package edu.alibaba.mpc4j.s2pc.opf.spermutation.php24;

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
public class Php24SharedPermutationConfig extends AbstractMultiPartyPtoConfig implements SharedPermutationConfig {
    /**
     * Shuffle config.
     */
    private final ShuffleConfig shuffleConfig;
    /**
     * Un-shuffle config.
     */
    private final ShuffleConfig unShuffleConfig;

    private Php24SharedPermutationConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.shuffleConfig, builder.unShuffleConfig);
        this.shuffleConfig = builder.shuffleConfig;
        this.unShuffleConfig = builder.unShuffleConfig;
    }

    @Override
    public SharedPermutationTypes getPtoType() {
        return SharedPermutationTypes.PHP24;
    }

    @Override
    public boolean isReverse() {
        return false;
    }

    public ShuffleConfig getShuffleConfig() {
        return shuffleConfig;
    }

    public ShuffleConfig getUnShuffleConfig() {
        return unShuffleConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Php24SharedPermutationConfig> {
        /**
         * Shuffle config.
         */
        private final ShuffleConfig shuffleConfig;
        /**
         * Un-shuffle config.
         */
        private final ShuffleConfig unShuffleConfig;

        public Builder(boolean silent) {
            shuffleConfig = ShuffleFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            unShuffleConfig = ShuffleFactory.createDefaultUnShuffleConfig(SecurityModel.SEMI_HONEST, silent);
        }

        @Override
        public Php24SharedPermutationConfig build() {
            return new Php24SharedPermutationConfig(this);
        }
    }
}
