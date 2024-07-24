package edu.alibaba.mpc4j.s2pc.pjc.pmap.php24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.rrg21.Rrg21Z2cConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.gmr21.Gmr21OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.PermGenConfig;
import edu.alibaba.mpc4j.s2pc.opf.pgenerator.smallfield.ahi22.Ahi22SmallFieldPermGenConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.php24.Php24SharedPermutationConfig;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.php24b.Php24bSharedPermutationConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory.PmapPtoType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.rs21.Rs21PlpsiConfig;

/**
 * level 2 secure private map configure
 *
 * @author Feng Han
 * @date 2023/10/24
 */
public class Php24PmapConfig extends AbstractMultiPartyPtoConfig implements PmapConfig {
    /**
     * z2 configure
     */
    private final Z2cConfig z2cConfig;
    /**
     * plpsi configure
     */
    private final PlpsiConfig plpsiconfig;
    /**
     * osn configure
     */
    private final OsnConfig osnConfig;
    /**
     * the bit length of an index in result permutation
     */
    private final int bitLen;
    /**
     * permutation generator
     */
    private final PermGenConfig smallFieldPermGenConfig;
    /**
     * A2b config.
     */
    private final A2bConfig a2bConfig;
    /**
     * permutation configure
     */
    private final SharedPermutationConfig permutationConfig, invPermutationConfig;

    private Php24PmapConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        plpsiconfig = builder.plpsiconfig;
        osnConfig = builder.osnConfig;
        bitLen = builder.bitLen;
        smallFieldPermGenConfig = builder.smallFieldPermGenConfig;
        z2cConfig = builder.z2cConfig;
        permutationConfig = builder.permutationConfig;
        invPermutationConfig = builder.invPermutationConfig;
        a2bConfig = builder.a2bConfig;
    }

    @Override
    public PmapPtoType getPtoType() {
        return PmapPtoType.PHP24;
    }

    public PlpsiConfig getPlpsiconfig() {
        return plpsiconfig;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public int getBitLen() {
        return bitLen;
    }

    public PermGenConfig getPermutableSorterConfig() {
        return smallFieldPermGenConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public SharedPermutationConfig getPermutationConfig() {
        return permutationConfig;
    }

    public SharedPermutationConfig getInvPermutationConfig() {
        return invPermutationConfig;
    }

    public A2bConfig getA2bConfig() {
        return a2bConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Php24PmapConfig> {
        /**
         * z2 configure
         */
        private Z2cConfig z2cConfig;
        /**
         * plpsi configure
         */
        private PlpsiConfig plpsiconfig;
        /**
         * osn configure
         */
        private OsnConfig osnConfig;
        /**
         * the bit length of an index in result permutation
         */
        private int bitLen;
        /**
         * permutation generator
         */
        private PermGenConfig smallFieldPermGenConfig;
        /**
         * A2b config.
         */
        private A2bConfig a2bConfig;
        /**
         * permutation configure
         */
        private final SharedPermutationConfig permutationConfig, invPermutationConfig;

        public Builder(boolean silent) {
            plpsiconfig = new Rs21PlpsiConfig.Builder(silent).build();
            osnConfig = new Gmr21OsnConfig.Builder(silent).build();
            bitLen = Long.SIZE;
            Bit2aConfig bit2aConfig = Bit2aFactory.createDefaultConfig(SecurityModel.SEMI_HONEST,  ZlFactory.createInstance(EnvType.STANDARD, bitLen), silent);
            smallFieldPermGenConfig = new Ahi22SmallFieldPermGenConfig.Builder(bit2aConfig, silent).build();
            z2cConfig = new Rrg21Z2cConfig.Builder().build();
            permutationConfig = new Php24SharedPermutationConfig.Builder(silent).build();
            invPermutationConfig = new Php24bSharedPermutationConfig.Builder(silent).build();
            a2bConfig = A2bFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, ZlFactory.createInstance(EnvType.STANDARD, bitLen), silent);
        }

        public Builder setPlpsiconfig(PlpsiConfig plpsiconfig) {
            this.plpsiconfig = plpsiconfig;
            return this;
        }

        public Builder setOsnConfig(OsnConfig osnConfig) {
            this.osnConfig = osnConfig;
            return this;
        }

        public Builder setBitLength(int bitLen, boolean silent) {
            this.bitLen = bitLen;
            Bit2aConfig bit2aConfig = Bit2aFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, ZlFactory.createInstance(EnvType.STANDARD, bitLen), silent);
            smallFieldPermGenConfig = new Ahi22SmallFieldPermGenConfig.Builder(bit2aConfig, silent).build();
            a2bConfig = A2bFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, ZlFactory.createInstance(EnvType.STANDARD, bitLen), silent);
            return this;
        }

        public Builder setBit2aConfig(Bit2aConfig bit2aConfig, boolean silent) {
            bitLen = bit2aConfig.getZl().getL();
            smallFieldPermGenConfig = new Ahi22SmallFieldPermGenConfig.Builder(bit2aConfig, silent).build();
            return this;
        }

        public Builder setZ2cConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        @Override
        public Php24PmapConfig build() {
            return new Php24PmapConfig(this);
        }
    }
}
