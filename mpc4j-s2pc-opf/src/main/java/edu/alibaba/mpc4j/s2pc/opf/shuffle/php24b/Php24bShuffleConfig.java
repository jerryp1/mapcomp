package edu.alibaba.mpc4j.s2pc.opf.shuffle.php24b;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory.ShuffleTypes;

/**
 * Php+24b un-shuffle config.
 *
 * @author Li Peng
 * @date 2023/5/26
 */
public class Php24bShuffleConfig extends AbstractMultiPartyPtoConfig implements ShuffleConfig {
    /**
     * Osn config.
     */
    private final OsnConfig osnConfig;

    private Php24bShuffleConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.osnConfig);
        this.osnConfig = builder.osnConfig;
    }

    @Override
    public ShuffleTypes getPtoType() {
        return ShuffleTypes.PHP24b;
    }

    @Override
    public boolean isReverse() {
        return true;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Php24bShuffleConfig> {
        /**
         * Osn config.
         */
        private final OsnConfig osnConfig;

        public Builder(boolean silent) {
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        @Override
        public Php24bShuffleConfig build() {
            return new Php24bShuffleConfig(this);
        }
    }
}
