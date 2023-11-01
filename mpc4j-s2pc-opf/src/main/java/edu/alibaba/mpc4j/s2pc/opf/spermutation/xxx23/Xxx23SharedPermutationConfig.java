package edu.alibaba.mpc4j.s2pc.opf.spermutation.xxx23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory.SharedPermutationTypes;

/**
 * Xxx+23 shared permutation config.
 *
 * @author Li Peng
 * @date 2023/5/25
 */
public class Xxx23SharedPermutationConfig extends AbstractMultiPartyPtoConfig implements SharedPermutationConfig {
    /**
     * Shuffle config.
     */
    private final ShuffleConfig shuffleConfig;
    /**
     * Un-shuffle config.
     */
    private final ShuffleConfig unShuffleConfig;

    private Xxx23SharedPermutationConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.shuffleConfig, builder.unShuffleConfig);
        this.shuffleConfig = builder.shuffleConfig;
        this.unShuffleConfig = builder.unShuffleConfig;
    }

    @Override
    public SharedPermutationTypes getPtoType() {
        return SharedPermutationTypes.XXX23;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Xxx23SharedPermutationConfig> {
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
        public Xxx23SharedPermutationConfig build() {
            return new Xxx23SharedPermutationConfig(this);
        }
    }
}
