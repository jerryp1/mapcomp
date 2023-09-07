package edu.alibaba.mpc4j.s2pc.pso.psi.prty20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.prty20.Prty20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;

/**
 * PRTY20 PSI协议配置，论文为：
 * <p>
 * Pinkas B, Rosulek M, Trieu N, et al. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020,
 * Springer, Cham, 2020, pp. 739-767.
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/14
 */
public class Prty20PsiConfig extends AbstractMultiPartyPtoConfig implements PsiConfig {
    /**
     * MPOPRF配置项
     */
    private final MpOprfConfig mpOprfConfig;
    /**
     * 过滤器类型
     */
    private final FilterFactory.FilterType filterType;
    /**
     * OKVS类型
     */
    private final Gf2eDokvsFactory.Gf2eDokvsType binaryOkvsType;

    private Prty20PsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.mpOprfConfig);
        mpOprfConfig = builder.mpOprfConfig;
        filterType = builder.filterType;
        binaryOkvsType = builder.binaryOkvsType;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiType.PRTY20;
    }

    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    public FilterFactory.FilterType getFilterType() {
        return filterType;
    }

    public Gf2eDokvsFactory.Gf2eDokvsType getOkvsType() {
        return binaryOkvsType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Prty20PsiConfig> {
        /**
         * OPRF类型
         */
        private MpOprfConfig mpOprfConfig;
        /**
         * 过滤器类型
         */
        private FilterFactory.FilterType filterType;

        private Gf2eDokvsFactory.Gf2eDokvsType binaryOkvsType;

        public Builder() {
            mpOprfConfig = new Prty20MpOprfConfig.Builder().build();
            filterType = FilterFactory.FilterType.SET_FILTER;
        }

        public Builder setMpOprfConfig(MpOprfConfig mpOprfConfig) {
            this.mpOprfConfig = mpOprfConfig;
            return this;
        }

        public Builder setFilterType(FilterFactory.FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        public Builder setBinaryOkvsType(Gf2eDokvsFactory.Gf2eDokvsType binaryOkvsType) {
            if (Gf2eDokvsFactory.isBinary(binaryOkvsType))
                this.mpOprfConfig = new Prty20MpOprfConfig.Builder().setBinaryOkvsType(binaryOkvsType).build();
            return this;
        }

        @Override
        public Prty20PsiConfig build() {
            return new Prty20PsiConfig(this);
        }
    }
}