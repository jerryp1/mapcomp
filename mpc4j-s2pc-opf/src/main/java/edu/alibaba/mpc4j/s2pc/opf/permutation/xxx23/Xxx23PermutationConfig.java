package edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.permutation.PermutationFactory.PermutationTypes;

/**
 * Xxx+23 permutation Config.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Xxx23PermutationConfig extends AbstractMultiPartyPtoConfig implements PermutationConfig {
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
     * Zl.
     */
    private final Zl zl;

    private Xxx23PermutationConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.osnConfig, builder.zlcConfig, builder.z2cConfig);
        this.osnConfig = builder.osnConfig;
        this.zl = builder.zl;
        this.zlcConfig = builder.zlcConfig;
        this.z2cConfig = builder.z2cConfig;
    }

    @Override
    public PermutationTypes getPtoType() {
        return PermutationTypes.XXX23;
    }

    @Override
    public Zl getZl() {
        return zl;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Xxx23PermutationConfig> {
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
         * zl
         */
        private final Zl zl;

        public Builder(Zl zl) {
            this.zl = zl;
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
            zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        @Override
        public Xxx23PermutationConfig build() {
            return new Xxx23PermutationConfig(this);
        }
    }
}
