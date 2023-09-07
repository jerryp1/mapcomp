package edu.alibaba.mpc4j.s2pc.pso.psi.psz14;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.psz14.Psz14GbfMpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

public class Psz14GbfPsiConfig extends AbstractMultiPartyPtoConfig implements PsiConfig {
    /**
     * MPOPRF配置项
     */
    private final MpOprfConfig mpOprfConfig;
    /**
     * 过滤器类型
     */
    private final FilterFactory.FilterType filterType;


    private Psz14GbfPsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.mpOprfConfig);
        mpOprfConfig = builder.mpOprfConfig;
        filterType = builder.filterType;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.PSZ14_GBF;
    }

    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    public FilterFactory.FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Psz14GbfPsiConfig> {
        /**
         * OPRF类型
         */
        private MpOprfConfig mpOprfConfig;
        /**
         * 过滤器类型
         */
        private FilterFactory.FilterType filterType;

        public Builder() {
            mpOprfConfig = new Psz14GbfMpOprfConfig.Builder().build();
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


        @Override
        public Psz14GbfPsiConfig build() {
            return new Psz14GbfPsiConfig(this);
        }
    }
}