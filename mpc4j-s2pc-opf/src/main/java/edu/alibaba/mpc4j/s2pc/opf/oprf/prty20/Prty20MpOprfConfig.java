package edu.alibaba.mpc4j.s2pc.opf.oprf.prty20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory.OprfType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;

/**
 * PRTY20的OPRF协议配置，论文为：
 * <p>
 * Pinkas B, Rosulek M, Trieu N, et al. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020,
 * Springer, Cham, 2020, pp. 739-767.
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/14
 */
public class Prty20MpOprfConfig extends AbstractMultiPartyPtoConfig implements MpOprfConfig {
    /**
     * 核LOT协议配置项
     * TODO 确认下原来的CoreLotConfig是不是现在的LcotConfig
     * TODO 为什么ot选择malicious的，但是OKVS选择malicious的，OKVS malicious的选项在哪里？
     */
    private final LcotConfig lcotConfig;
    /**
     * OKVS类型
     */
    private final Gf2eDokvsType binaryOkvsType;

    private Prty20MpOprfConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreLotConfig);
        lcotConfig = builder.coreLotConfig;
        binaryOkvsType = builder.binaryOkvsType;
    }

    public LcotConfig getLcotConfig() {
        return lcotConfig;
    }

    public Gf2eDokvsType getBinaryOkvsType() {return binaryOkvsType; }

    @Override
    public OprfType getPtoType() {
        return OprfFactory.OprfType.PRTY20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Prty20MpOprfConfig> {
        /**
         * 核COT协议配置项
         */
        private LcotConfig coreLotConfig;
        /**
         * OKVS类型
         */
        private Gf2eDokvsType binaryOkvsType;

        public Builder() {
            coreLotConfig = LcotFactory.createDefaultConfig(SecurityModel.MALICIOUS);
            binaryOkvsType = Gf2eDokvsType.H2_SINGLETON_GCT;
        }

        public Builder setCoreLotConfig(LcotConfig coreLotConfig) {
            this.coreLotConfig = coreLotConfig;
            return this;
        }

        public Builder setBinaryOkvsType(Gf2eDokvsType binaryOkvsType) {
            this.binaryOkvsType = binaryOkvsType;
            return this;
        }

        @Override
        public Prty20MpOprfConfig build() {
            return new Prty20MpOprfConfig(this);
        }
    }
}