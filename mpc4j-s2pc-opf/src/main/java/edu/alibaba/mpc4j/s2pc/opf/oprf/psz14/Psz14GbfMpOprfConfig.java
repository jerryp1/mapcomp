package edu.alibaba.mpc4j.s2pc.opf.oprf.psz14;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

public class Psz14GbfMpOprfConfig extends AbstractMultiPartyPtoConfig implements MpOprfConfig {
    /**
     * 核LOT协议配置项
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * OKVS类型
     */
    private final Gf2eDokvsFactory.Gf2eDokvsType binaryOkvsType;

    private Psz14GbfMpOprfConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig);
        coreCotConfig = builder.coreCotConfig;
        binaryOkvsType = builder.binaryOkvsType;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public Gf2eDokvsFactory.Gf2eDokvsType getBinaryOkvsType() {return binaryOkvsType; }

    @Override
    public OprfFactory.OprfType getPtoType() {
        return OprfFactory.OprfType.PSZ14_GBF;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Psz14GbfMpOprfConfig> {
        /**
         * 核COT协议配置项
         */
        private CoreCotConfig coreCotConfig;
        /**
         * OKVS类型
         */
        private Gf2eDokvsFactory.Gf2eDokvsType binaryOkvsType;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            binaryOkvsType = Gf2eDokvsType.RANDOM_GBF;
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setBinaryOkvsType(Gf2eDokvsFactory.Gf2eDokvsType binaryOkvsType) {
            this.binaryOkvsType = binaryOkvsType;
            return this;
        }

        @Override
        public Psz14GbfMpOprfConfig build() {
            return new Psz14GbfMpOprfConfig(this);
        }
    }
}
