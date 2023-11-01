package edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23b;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory.ShuffleTypes;

/**
 * Xxx+23b un-shuffle config.
 *
 * @author Li Peng
 * @date 2023/5/26
 */
public class Xxx23bShuffleConfig extends AbstractMultiPartyPtoConfig implements ShuffleConfig {
    /**
     * Osn config.
     */
    private final OsnConfig osnConfig;

    private Xxx23bShuffleConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.osnConfig);
        this.osnConfig = builder.osnConfig;
    }

    @Override
    public ShuffleTypes getPtoType() {
        return ShuffleTypes.XXX23b;
    }

    @Override
    public boolean isReverse() {
        return true;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Xxx23bShuffleConfig> {
        /**
         * Osn config.
         */
        private final OsnConfig osnConfig;

        public Builder(boolean silent) {
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        @Override
        public Xxx23bShuffleConfig build() {
            return new Xxx23bShuffleConfig(this);
        }
    }
}
