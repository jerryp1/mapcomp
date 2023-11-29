package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory.Z2MuxType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

public class Rrk20Z2MuxConfig extends AbstractMultiPartyPtoConfig implements Z2MuxConfig {
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private Rrk20Z2MuxConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public Z2MuxType getPtoType() {
        return Z2MuxType.RRK20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrk20Z2MuxConfig> {
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
        public Rrk20Z2MuxConfig build() {
            return new Rrk20Z2MuxConfig(this);
        }
    }
}
