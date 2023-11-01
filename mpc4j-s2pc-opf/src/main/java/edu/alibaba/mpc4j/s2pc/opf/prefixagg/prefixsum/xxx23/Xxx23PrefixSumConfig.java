package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum.xxx23;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixTreeFactory.PrefixTreeTypes;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum.PrefixSumConfig;
import edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixsum.PrefixSumFactory.PrefixSumTypes;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;

/**
 * Xxx23 prefix sum Config.
 *
 * @author Li Peng
 * @date 2023/10/30
 */
public class Xxx23PrefixSumConfig extends AbstractMultiPartyPtoConfig implements PrefixSumConfig {
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * Zl circuit config.
     */
    private final ZlcConfig zlcConfig;
    /**
     * Zl mux config.
     */
    private final ZlMuxConfig zlMuxConfig;
    /**
     * Shuffle config
     */
    private final ShuffleConfig shuffleConfig;
    /**
     * Prefix tree type.
     */
    private final PrefixTreeTypes prefixTreeType;
    /**
     * Zl.
     */
    private final Zl zl;
    /**
     * Need shuffle
     */
    private final boolean needShuffle;

    private Xxx23PrefixSumConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig, builder.zlcConfig, builder.zlMuxConfig);
        z2cConfig = builder.z2cConfig;
        zlcConfig = builder.zlcConfig;
        zlMuxConfig = builder.zlMuxConfig;
        shuffleConfig = builder.shuffleConfig;
        prefixTreeType = builder.prefixTreeType;
        needShuffle = builder.needShuffle;
        zl = builder.zl;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
    }

    public ZlMuxConfig getZlMuxConfig() {
        return zlMuxConfig;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public boolean needShuffle() {
        return needShuffle;
    }

    public PrefixTreeTypes getPrefixTreeType() {
        return prefixTreeType;
    }

    @Override
    public PrefixSumTypes getPtoType() {
        return PrefixSumTypes.Xxx23;
    }

    public ShuffleConfig getShuffleConfig() {
        return shuffleConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Xxx23PrefixSumConfig> {
        /**
         * Z2 circuit config.
         */
        private final Z2cConfig z2cConfig;
        /**
         * Zl circuit config.
         */
        private final ZlcConfig zlcConfig;
        /**
         * Zl mux config.
         */
        private final ZlMuxConfig zlMuxConfig;
        /**
         * Shuffle config
         */
        private final ShuffleConfig shuffleConfig;
        /**
         * Prefix tree type.
         */
        private final PrefixTreeTypes prefixTreeType;
        /**
         * Zl.
         */
        private final Zl zl;

        private boolean needShuffle;

        public Builder(Zl zl, boolean silent) {
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            shuffleConfig = ShuffleFactory.createDefaultUnShuffleConfig(SecurityModel.SEMI_HONEST, zl, silent);
            prefixTreeType = PrefixTreeTypes.BRENT_KUNG;
            needShuffle = false;
            this.zl = zl;
        }

        @Override
        public Xxx23PrefixSumConfig build() {
            return new Xxx23PrefixSumConfig(this);
        }

        public Builder setNeedShuffle(boolean needShuffle) {
            this.needShuffle = needShuffle;
            return this;
        }
    }
}
