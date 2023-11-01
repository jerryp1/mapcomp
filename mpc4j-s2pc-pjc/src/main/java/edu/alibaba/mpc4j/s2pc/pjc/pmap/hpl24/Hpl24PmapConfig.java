package edu.alibaba.mpc4j.s2pc.pjc.pmap.hpl24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21.Kvh21Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.rrg21.Rrg21Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.PermutableSorterConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.ahi22.Ahi22PermutableSorterConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.gmr21.Gmr21OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.xxx23.Xxx23ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory.PmapType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.rs21.Rs21PlpsiConfig;

public class Hpl24PmapConfig extends AbstractMultiPartyPtoConfig implements PmapConfig {
    /**
     * 所需的z2
     */
    private final Z2cConfig z2cConfig;
    /**
     * 使用的plpsi配置
     */
    private final PlpsiConfig plpsiconfig;
    /**
     * 使用的osn配置
     */
    private final OsnConfig osnConfig;
    /**
     * 在得到map的过程中需要用到B2A的类型转换，在此需要定义域的大小
     */
    private final int bitLen;
    /**
     * b2A配置
     */
    private final Bit2aConfig bit2aConfig;
    /**
     * 生成稳定排序对应置换的配置
     */
    private final PermutableSorterConfig permutableSorterConfig;
    /**
     * 生成稳定排序对应置换的配置
     */
    private ShuffleConfig shuffleConfig;

    private Hpl24PmapConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        plpsiconfig = builder.plpsiconfig;
        osnConfig = builder.osnConfig;
        bitLen = builder.bitLen;
        bit2aConfig = builder.bit2aConfig;
        permutableSorterConfig = builder.permutableSorterConfig;
        z2cConfig = builder.z2cConfig;
        shuffleConfig = builder.shuffleConfig;
    }

    @Override
    public PmapType getPtoType() {
        return PmapType.HPL24;
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

    public Bit2aConfig getBit2aConfig() {
        return bit2aConfig;
    }

    public PermutableSorterConfig getPermutableSorterConfig() {
        return permutableSorterConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public ShuffleConfig getShuffleConfig() {
        return shuffleConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hpl24PmapConfig> {
        /**
         * 所需的z2
         */
        private Z2cConfig z2cConfig;
        /**
         * 使用的plpsi配置
         */
        private PlpsiConfig plpsiconfig;
        /**
         * 使用的osn配置
         */
        private OsnConfig osnConfig;
        /**
         * 在得到map的过程中需要用到B2A的类型转换，在此需要定义域的大小
         */
        private int bitLen;
        /**
         * b2A配置
         */
        private Bit2aConfig bit2aConfig;
        /**
         * 生成稳定排序对应置换的配置
         */
        private PermutableSorterConfig permutableSorterConfig;

        /**
         * 生成稳定排序对应置换的配置
         */
        private ShuffleConfig shuffleConfig;

        public Builder(boolean silent) {
            plpsiconfig = new Rs21PlpsiConfig.Builder(silent).build();
            osnConfig = new Gmr21OsnConfig.Builder(silent).build();
            bitLen = Long.SIZE;
            bit2aConfig = new Kvh21Bit2aConfig.Builder(ZlFactory.createInstance(EnvType.STANDARD, bitLen)).build();
            permutableSorterConfig = new Ahi22PermutableSorterConfig.Builder(bit2aConfig).build();
            z2cConfig = new Rrg21Z2cConfig.Builder().build();
            shuffleConfig = new Xxx23ShuffleConfig.Builder(silent).build();
        }

        public Builder setPlpsiconfig(PlpsiConfig plpsiconfig) {
            this.plpsiconfig = plpsiconfig;
            return this;
        }

        public Builder setOsnConfig(OsnConfig osnConfig) {
            this.osnConfig = osnConfig;
            return this;
        }

        public Builder setBitLength(int bitLen) {
            this.bitLen = bitLen;
            bit2aConfig = new Kvh21Bit2aConfig.Builder(ZlFactory.createInstance(EnvType.STANDARD, bitLen)).build();
            permutableSorterConfig = new Ahi22PermutableSorterConfig.Builder(bit2aConfig).build();
            return this;
        }

        public Builder setBit2aConfig(Bit2aConfig bit2aConfig) {
            bitLen = bit2aConfig.getZl().getL();
            this.bit2aConfig = bit2aConfig;
            permutableSorterConfig = new Ahi22PermutableSorterConfig.Builder(bit2aConfig).build();
            return this;
        }

        public Builder setZ2cConfig(Z2cConfig z2cConfig){
            this.z2cConfig = z2cConfig;
            return this;
        }

        @Override
        public Hpl24PmapConfig build() {
            return new Hpl24PmapConfig(this);
        }
    }
}
