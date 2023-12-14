package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax.amos22;

import edu.alibaba.mpc4j.common.circuit.prefix.PrefixTreeFactory.PrefixTreeTypes;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.share.amos22.Amos22ShareGroupConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;

import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax.PrefixMaxConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax.PrefixMaxFactory.PrefixMaxTypes;

public class Amos22PrefixMaxConfig extends AbstractMultiPartyPtoConfig implements PrefixMaxConfig {
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * Shuffle config
     */
    private final ShuffleConfig shuffleConfig;
    /**
     * Osn config.
     */
    private final OsnConfig osnConfig;
    /**
     * Zl.
     */
    private final Zl zl;
    /**
     * Need shuffle
     */
    private final boolean needShuffle;
    /**
     * plain output
     */
    private final boolean plainOutput;
    private final Z2MuxConfig z2MuxConfig;
    private final Amos22ShareGroupConfig shareGroupConfig;

    private Amos22PrefixMaxConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig, builder.zlcConfig, builder.zlMuxConfig,
            builder.zlGreaterConfig, builder.osnConfig, builder.plainBitMuxConfig);
        z2cConfig = builder.z2cConfig;
        shuffleConfig = builder.shuffleConfig;
        needShuffle = builder.needShuffle;
        osnConfig = builder.osnConfig;
        zl = builder.zl;
        plainOutput = builder.plainOutput;
        z2MuxConfig = builder.z2MuxConfig;
        shareGroupConfig = builder.shareGroupConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public boolean needShuffle() {
        return needShuffle;
    }

    @Override
    public PrefixAggTypes getPrefixType() {
        return PrefixAggTypes.MAX;
    }

    @Override
    public PrefixMaxTypes getPtoType() {
        return PrefixMaxTypes.AMOS22;
    }

    public ShuffleConfig getShuffleConfig() {
        return shuffleConfig;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public boolean isPlainOutput() {
        return plainOutput;
    }

    public Z2MuxConfig getZ2MuxConfig() {
        return z2MuxConfig;
    }

    public Amos22ShareGroupConfig getShareGroupConfig() {
        return shareGroupConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Amos22PrefixMaxConfig> {
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
         * Zl greater config.
         */
        private final ZlGreaterConfig zlGreaterConfig;
        /**
         * Shuffle config
         */
        private final ShuffleConfig shuffleConfig;
        /**
         * Osn config.
         */
        private final OsnConfig osnConfig;
        /**
         * Plain bit mux config
         */
        private final PlainBitMuxConfig plainBitMuxConfig;
        /**
         * Prefix tree type.
         */
        private final PrefixTreeTypes prefixTreeType;
        /**
         * Zl.
         */
        private final Zl zl;
        /**
         * need shuffle
         */
        private boolean needShuffle;
        /**
         * plain output
         */
        private boolean plainOutput;

        private B2aConfig b2aConfig;
        private A2bConfig a2bConfig;
        private Z2MuxConfig z2MuxConfig;
        private final Amos22ShareGroupConfig shareGroupConfig;

        public Builder(Zl zl, boolean silent) {
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            zlMuxConfig = ZlMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlGreaterConfig = ZlGreaterFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent, zl);
            shuffleConfig = ShuffleFactory.createDefaultUnShuffleConfig(SecurityModel.SEMI_HONEST, silent);
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            plainBitMuxConfig = PlainBitMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            prefixTreeType = PrefixTreeTypes.BRENT_KUNG;
            needShuffle = false;
            this.zl = zl;
            b2aConfig = B2aFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            a2bConfig = A2bFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            z2MuxConfig = Z2MuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            shareGroupConfig = new Amos22ShareGroupConfig.Builder(silent).build();
        }

        @Override
        public Amos22PrefixMaxConfig build() {
            return new Amos22PrefixMaxConfig(this);
        }

        public Builder setNeedShuffle(boolean needShuffle) {
            this.needShuffle = needShuffle;
            return this;
        }

        public Builder setPlainOutput(boolean plainOutput) {
            this.plainOutput = plainOutput;
            return this;
        }
    }
}
