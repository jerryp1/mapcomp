package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixmax.php24;

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

/**
 * Php+24 prefix max Config.
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public class Php24PrefixMaxConfig extends AbstractMultiPartyPtoConfig implements PrefixMaxConfig {
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
     * Osn config.
     */
    private final OsnConfig osnConfig;
    /**
     * Zl greater config.
     */
    private final ZlGreaterConfig zlGreaterConfig;
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
     * Need shuffle
     */
    private final boolean needShuffle;
    /**
     * plain output
     */
    private boolean plainOutput;
    private final B2aConfig b2aConfig;
    private final A2bConfig a2bConfig;
    private final Z2MuxConfig z2MuxConfig;

    private Php24PrefixMaxConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig, builder.zlcConfig, builder.zlMuxConfig,
            builder.zlGreaterConfig, builder.osnConfig, builder.plainBitMuxConfig);
        z2cConfig = builder.z2cConfig;
        zlcConfig = builder.zlcConfig;
        zlMuxConfig = builder.zlMuxConfig;
        zlGreaterConfig = builder.zlGreaterConfig;
        shuffleConfig = builder.shuffleConfig;
        plainBitMuxConfig = builder.plainBitMuxConfig;
        prefixTreeType = builder.prefixTreeType;
        needShuffle = builder.needShuffle;
        osnConfig = builder.osnConfig;
        zl = builder.zl;
        plainOutput = builder.plainOutput;
        b2aConfig = builder.b2aConfig;
        a2bConfig = builder.a2bConfig;
        z2MuxConfig = builder.z2MuxConfig;
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

    @Override
    public PrefixAggTypes getPrefixType() {
        return PrefixAggTypes.MAX;
    }

    public PrefixTreeTypes getPrefixTreeType() {
        return prefixTreeType;
    }

    @Override
    public PrefixMaxTypes getPtoType() {
        return PrefixMaxTypes.PHP24;
    }

    public ShuffleConfig getShuffleConfig() {
        return shuffleConfig;
    }

    public ZlGreaterConfig getZlGreaterConfig() {
        return zlGreaterConfig;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public PlainBitMuxConfig getPlainBitMuxConfig() {
        return plainBitMuxConfig;
    }

    public boolean isPlainOutput() {
        return plainOutput;
    }

    public Z2MuxConfig getZ2MuxConfig() {
        return z2MuxConfig;
    }

    public B2aConfig getB2aConfig() {
        return b2aConfig;
    }

    public A2bConfig getA2bConfig() {
        return a2bConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Php24PrefixMaxConfig> {
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
            b2aConfig = B2aFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl,silent);
            a2bConfig = A2bFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            z2MuxConfig = Z2MuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        @Override
        public Php24PrefixMaxConfig build() {
            return new Php24PrefixMaxConfig(this);
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
