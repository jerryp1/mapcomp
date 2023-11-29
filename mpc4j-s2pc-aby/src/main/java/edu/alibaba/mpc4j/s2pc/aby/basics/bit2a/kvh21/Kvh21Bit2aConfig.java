package edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory.Bit2aTypes;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * KVH+21 Bit2a Protocol Config.
 *
 * @author Li Peng
 * @date 2023/10/12
 */
public class Kvh21Bit2aConfig extends AbstractMultiPartyPtoConfig implements Bit2aConfig {
    /**
     * COT config.
     */
    private final CotConfig cotConfig;
    /**
     * zlc config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * Zl.
     */
    private final Zl zl;

    private Kvh21Bit2aConfig(Kvh21Bit2aConfig.Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig, builder.z2cConfig);
        cotConfig = builder.cotConfig;
        z2cConfig = builder.z2cConfig;
        zl = builder.zl;
    }

    @Override
    public Bit2aTypes getPtoType() {
        return Bit2aTypes.KVH21;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kvh21Bit2aConfig> {
        /**
         * COT config
         */
        private final CotConfig cotConfig;
        /**
         * zlc config.
         */
        private final Z2cConfig z2cConfig;
        /**
         * Zl
         */
        private final Zl zl;

        public Builder(Zl zl, boolean silent) {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            this.zl = zl;
        }

        @Override
        public Kvh21Bit2aConfig build() {
            return new Kvh21Bit2aConfig(this);
        }
    }
}
