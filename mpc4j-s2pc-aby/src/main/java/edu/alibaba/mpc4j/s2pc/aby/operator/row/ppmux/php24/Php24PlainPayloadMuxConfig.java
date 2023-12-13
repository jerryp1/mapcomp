package edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.php24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
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
public class Php24PlainPayloadMuxConfig extends AbstractMultiPartyPtoConfig implements PlainPayloadMuxConfig {
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private Php24PlainPayloadMuxConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public PlainMuxType getPtoType() {
        return PlainMuxType.PHP24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Php24PlainPayloadMuxConfig> {
        /**
         * COT config
         */
        private CotConfig cotConfig;

        public Builder(boolean silent) {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Php24PlainPayloadMuxConfig build() {
            return new Php24PlainPayloadMuxConfig(this);
        }
    }
}
