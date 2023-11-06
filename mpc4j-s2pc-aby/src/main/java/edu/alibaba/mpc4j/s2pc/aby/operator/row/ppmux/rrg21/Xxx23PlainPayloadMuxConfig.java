package edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.rrg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory.PlainMuxType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * Plain mux config.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
public class Xxx23PlainPayloadMuxConfig extends AbstractMultiPartyPtoConfig implements PlainPayloadMuxConfig {
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private final Zl zl;

    private Xxx23PlainPayloadMuxConfig(Builder builder) {
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
    public PlainMuxType getPtoType() {
        return PlainMuxType.Xxx23;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Xxx23PlainPayloadMuxConfig> {
        /**
         * COT config
         */
        private CotConfig cotConfig;
        /**
         * Zl
         */
        private final Zl zl;

        public Builder(Zl zl) {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
            this.zl = zl;
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Xxx23PlainPayloadMuxConfig build() {
            return new Xxx23PlainPayloadMuxConfig(this);
        }
    }
}
