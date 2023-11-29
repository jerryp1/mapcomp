package edu.alibaba.mpc4j.s2pc.aby.basics.a2b.dsz15;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory.A2bTypes;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * DSZ15 A2b Protocol Config.
 *
 * @author Li Peng
 * @date 2023/10/20
 */
public class Dsz15A2bConfig extends AbstractMultiPartyPtoConfig implements A2bConfig {
    /**
     * COT config.
     */
    private final CotConfig cotConfig;
    /**
     * z2c config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * Zl.
     */
    private final Zl zl;

    private Dsz15A2bConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig, builder.z2cConfig);
        cotConfig = builder.cotConfig;
        z2cConfig = builder.z2cConfig;
        zl = builder.zl;
    }

    @Override
    public A2bTypes getPtoType() {
        return A2bTypes.DSZ15;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Dsz15A2bConfig> {
        /**
         * COT config
         */
        private final CotConfig cotConfig;
        /**
         * z2c config.
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
        public Dsz15A2bConfig build() {
            return new Dsz15A2bConfig(this);
        }
    }
}
