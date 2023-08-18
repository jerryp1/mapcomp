package edu.alibaba.mpc4j.s2pc.pso.psi.prty19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.prty19.Prty19FastMpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

public class Prty19FastPsiConfig extends AbstractMultiPartyPtoConfig implements PsiConfig {
    /**
     * MPOPRF配置项
     */
    private final MpOprfConfig mpOprfConfig;
    /**
     * 过滤器类型
     */
    private final FilterFactory.FilterType filterType;

    private Prty19FastPsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.mpOprfConfig);
        mpOprfConfig = builder.mpOprfConfig;
        filterType = builder.filterType;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.PRTY19_FAST;
    }

    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    public FilterFactory.FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Prty19FastPsiConfig> {
        /**
         * OPRF类型
         */
        private MpOprfConfig mpOprfConfig;
        /**
         * 过滤器类型
         */
        private FilterFactory.FilterType filterType;

        public Builder() {
            mpOprfConfig = new Prty19FastMpOprfConfig.Builder().build();
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

        public Builder setOkvsType(Gf2eDokvsType okvsType) {
            return setMpOprfConfig(new Prty19FastMpOprfConfig.Builder().setOkvsType(okvsType).build());
        }

        @Override
        public Prty19FastPsiConfig build() {
            return new Prty19FastPsiConfig(this);
        }
    }
}