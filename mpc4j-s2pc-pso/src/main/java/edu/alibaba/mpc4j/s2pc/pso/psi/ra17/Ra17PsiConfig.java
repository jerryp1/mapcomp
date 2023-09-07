package edu.alibaba.mpc4j.s2pc.pso.psi.ra17;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17ByteEccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17EccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;

public class Ra17PsiConfig extends AbstractMultiPartyPtoConfig implements PsiConfig {
    /**
     * MPOPRF配置项
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * 过滤器类型
     */
    private final FilterFactory.FilterType filterType;

    private Ra17PsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.sqOprfConfig);
        sqOprfConfig = builder.sqOprfConfig;
        filterType = builder.filterType;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiType.RA17;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public FilterFactory.FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ra17PsiConfig> {
        /**
         * OPRF类型
         */
        private SqOprfConfig sqOprfConfig;
        /**
         * 过滤器类型
         */
        private FilterFactory.FilterType filterType;

        public Builder() {
            sqOprfConfig = new Ra17ByteEccSqOprfConfig.Builder().build();
            filterType = FilterFactory.FilterType.SET_FILTER;
        }

        public Builder setSqOprfConfig(SqOprfConfig sqOprfConfig) {
            this.sqOprfConfig = sqOprfConfig;
            return this;
        }

        public Builder setFilterType(FilterFactory.FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Ra17PsiConfig build() {
            return new Ra17PsiConfig(this);
        }
    }
}
