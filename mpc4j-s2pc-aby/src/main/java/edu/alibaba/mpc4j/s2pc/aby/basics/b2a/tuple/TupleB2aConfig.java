package edu.alibaba.mpc4j.s2pc.aby.basics.b2a.tuple;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory.B2aTypes;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.tuple.TupleBit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTupleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTupleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * Hardcode B2a Protocol Config.
 *
 * @author Li Peng
 * @date 2023/11/21
 */
public class TupleB2aConfig extends AbstractMultiPartyPtoConfig implements B2aConfig {
    /**
     * COT config.
     */
    private final CotConfig cotConfig;
    /**
     * B2a tuple config.
     */
    private final B2aTupleConfig b2aTupleConfig;
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * Tuple bit2a config.
     */
    private final TupleBit2aConfig tupleBit2aConfig;
    /**
     * Zl.
     */
    private final Zl zl;

    private TupleB2aConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig, builder.b2aTupleConfig,
            builder.z2cConfig, builder.tupleBit2aConfig);
        cotConfig = builder.cotConfig;
        b2aTupleConfig = builder.b2aTupleConfig;
        z2cConfig = builder.z2cConfig;
        tupleBit2aConfig = builder.tupleBit2aConfig;
        zl = builder.zl;
    }

    @Override
    public B2aTypes getPtoType() {
        return B2aTypes.TUPLE;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    public B2aTupleConfig getB2aTupleConfig() {
        return b2aTupleConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public TupleBit2aConfig getTupleBit2aConfig() {
        return tupleBit2aConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<TupleB2aConfig> {
        /**
         * COT config
         */
        private final CotConfig cotConfig;
        /**
         * B2a tuple config.
         */
        private final B2aTupleConfig b2aTupleConfig;
        /**
         * Z2 circuit config.
         */
        private final Z2cConfig z2cConfig;
        /**
         * Zl
         */
        private final Zl zl;
        /**
         * Tuple bit2a config.
         */
        private final TupleBit2aConfig tupleBit2aConfig;

        public Builder(Zl zl, boolean silent) {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            b2aTupleConfig = B2aTupleFactory.createDefaultConfig(zl);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            tupleBit2aConfig = new TupleBit2aConfig.Builder(zl, silent).build();
            this.zl = zl;
        }

        @Override
        public TupleB2aConfig build() {
            return new TupleB2aConfig(this);
        }
    }
}
