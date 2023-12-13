package edu.alibaba.mpc4j.s2pc.opf.permutation.php24;

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
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory.PermutationTypes;

/**
 * Php+24 permutation config.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Php24PermutationConfig extends AbstractMultiPartyPtoConfig implements PermutationConfig {
    /**
     * Osn config.
     */
    private final OsnConfig osnConfig;
    /**
     * Zl circuit config.
     */
    private final ZlcConfig zlcConfig;
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * A2b config.
     */
    private final A2bConfig a2bConfig;
    /**
     * B2a config.
     */
    private final B2aConfig b2aConfig;
    /**
     * Zl instance.
     */
    private final Zl zl;

    private Php24PermutationConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.osnConfig, builder.zlcConfig, builder.z2cConfig,
            builder.a2bConfig, builder.b2aConfig);
        this.osnConfig = builder.osnConfig;
        this.zl = builder.zl;
        this.zlcConfig = builder.zlcConfig;
        this.z2cConfig = builder.z2cConfig;
        this.a2bConfig = builder.a2bConfig;
        this.b2aConfig = builder.b2aConfig;
    }

    @Override
    public PermutationTypes getPtoType() {
        return PermutationTypes.PHP24;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public boolean isReverse() {
        return false;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public A2bConfig getA2bConfig() {
        return a2bConfig;
    }

    public B2aConfig getB2aConfig() {
        return b2aConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Php24PermutationConfig> {
        /**
         * Osn config.
         */
        private final OsnConfig osnConfig;
        /**
         * Zl circuit config.
         */
        private final ZlcConfig zlcConfig;
        /**
         * Z2 circuit config.
         */
        private final Z2cConfig z2cConfig;
        /**
         * A2b config.
         */
        private final A2bConfig a2bConfig;
        /**
         * B2a config.
         */
        private final B2aConfig b2aConfig;
        /**
         * Zl instance of plaintext.
         */
        private final Zl zl;

        public Builder(Zl zl, boolean silent) {
            this.zl = zl;
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            a2bConfig = A2bFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
            b2aConfig = B2aFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl, silent);
        }

        @Override
        public Php24PermutationConfig build() {
            return new Php24PermutationConfig(this);
        }
    }
}
