package edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.tuple;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory.Bit2aTypes;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTupleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTupleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * Tuple Bit2a Protocol Config.
 *
 * @author Li Peng
 * @date 2023/11/21
 */
public class TupleBit2aConfig extends AbstractMultiPartyPtoConfig implements Bit2aConfig {
    /**
     * COT config.
     */
    private final CotConfig cotConfig;
    /**
     * zlc config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * b2a tuple config.
     */
    private final B2aTupleConfig b2aTupleConfig;
    /**
     * Zl.
     */
    private final Zl zl;

    private TupleBit2aConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig, builder.z2cConfig
            , builder.b2aTupleConfig);
        cotConfig = builder.cotConfig;
        z2cConfig = builder.z2cConfig;
        b2aTupleConfig = builder.b2aTupleConfig;
        zl = builder.zl;
    }

    @Override
    public Bit2aTypes getPtoType() {
        return Bit2aTypes.TUPLE;
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

    public B2aTupleConfig getB2aTupleConfig() {
        return b2aTupleConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<TupleBit2aConfig> {
        /**
         * COT config
         */
        private final CotConfig cotConfig;
        /**
         * zlc config.
         */
        private final Z2cConfig z2cConfig;
        /**
         * b2a tuple config.
         */
        private final B2aTupleConfig b2aTupleConfig;
        /**
         * Zl
         */
        private final Zl zl;

        public Builder(Zl zl, boolean silent) {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            b2aTupleConfig = B2aTupleFactory.createDefaultConfig(zl);
            this.zl = zl;
        }

        @Override
        public TupleBit2aConfig build() {
            return new TupleBit2aConfig(this);
        }
    }
}
