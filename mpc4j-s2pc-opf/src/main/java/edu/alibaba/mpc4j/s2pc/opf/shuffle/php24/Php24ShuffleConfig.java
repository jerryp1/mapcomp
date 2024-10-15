package edu.alibaba.mpc4j.s2pc.opf.shuffle.php24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory.ShuffleTypes;

/**
 * Php+24 shuffle config.
 *
 */
public class Php24ShuffleConfig extends AbstractMultiPartyPtoConfig implements ShuffleConfig {
    /**
     * Osn config.
     */
    private final OsnConfig osnConfig;

    private Php24ShuffleConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.osnConfig);
        this.osnConfig = builder.osnConfig;
    }

    @Override
    public ShuffleTypes getPtoType() {
        return ShuffleTypes.PHP24;
    }

    @Override
    public boolean isReverse() {
        return false;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Php24ShuffleConfig> {
        /**
         * Osn config.
         */
        private final OsnConfig osnConfig;

        public Builder(boolean silent) {
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        @Override
        public Php24ShuffleConfig build() {
            return new Php24ShuffleConfig(this);
        }
    }
}
