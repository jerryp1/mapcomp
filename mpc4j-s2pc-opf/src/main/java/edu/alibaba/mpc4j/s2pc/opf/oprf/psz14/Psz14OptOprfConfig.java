package edu.alibaba.mpc4j.s2pc.opf.oprf.psz14;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;

public class Psz14OptOprfConfig extends AbstractMultiPartyPtoConfig implements OprfConfig {
    /**
     * 核LOT协议配置项
     */
    private final LcotConfig lcotConfig;

    private Psz14OptOprfConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.LcotConfig);
        lcotConfig = builder.LcotConfig;
    }

    public LcotConfig getLcotConfig() {
        return lcotConfig;
    }

    @Override
    public OprfFactory.OprfType getPtoType() {
        return OprfFactory.OprfType.PSZ14_OPT;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Psz14OptOprfConfig> {
        /**
         * 核COT协议配置项
         */
        private LcotConfig LcotConfig;

        public Builder() {
            LcotConfig = LcotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setLcotConfig(LcotConfig lcotConfig) {
            this.LcotConfig = lcotConfig;
            return this;
        }

        @Override
        public Psz14OptOprfConfig build() {
            return new Psz14OptOprfConfig(this);
        }
    }
}
