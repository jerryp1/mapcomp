package edu.alibaba.mpc4j.s2pc.opf.oprf.psz14;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;

public class Psz14OriOprfConfig extends AbstractMultiPartyPtoConfig implements MpOprfConfig {
    /**
     * 核LOT协议配置项
     */
    private final LcotConfig lcotConfig;


    private Psz14OriOprfConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.lcotConfig);
        lcotConfig = builder.lcotConfig;
    }

    public LcotConfig getLcotConfig() {
        return lcotConfig;
    }


    @Override
    public OprfFactory.OprfType getPtoType() {
        return OprfFactory.OprfType.PSZ14_ORI;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Psz14OriOprfConfig> {
        /**
         * 核COT协议配置项
         */
        private LcotConfig lcotConfig;

        public Builder() {
            lcotConfig = LcotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setLcotConfig(LcotConfig lcotConfig) {
            this.lcotConfig = lcotConfig;
            return this;
        }

        @Override
        public Psz14OriOprfConfig build() {
            return new Psz14OriOprfConfig(this);
        }
    }
}
