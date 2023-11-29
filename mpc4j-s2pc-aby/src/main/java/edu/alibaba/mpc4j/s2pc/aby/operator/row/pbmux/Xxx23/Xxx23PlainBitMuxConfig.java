package edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.Xxx23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory.PlainBitMuxType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * Plain bit mux config.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public class Xxx23PlainBitMuxConfig extends AbstractMultiPartyPtoConfig implements PlainBitMuxConfig {
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private final Zl zl;

    private Xxx23PlainBitMuxConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        cotConfig = builder.cotConfig;
        zl = builder.zl;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public PlainBitMuxType getPtoType() {
        return PlainBitMuxType.Xxx23;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Xxx23PlainBitMuxConfig> {
        /**
         * COT config
         */
        private CotConfig cotConfig;
        /**
         * Zl
         */
        private final Zl zl;

        public Builder(Zl zl, boolean silent) {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            this.zl = zl;
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Xxx23PlainBitMuxConfig build() {
            return new Xxx23PlainBitMuxConfig(this);
        }
    }
}
