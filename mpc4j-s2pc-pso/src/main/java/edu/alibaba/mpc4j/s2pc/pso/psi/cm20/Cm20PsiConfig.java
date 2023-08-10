package edu.alibaba.mpc4j.s2pc.pso.psi.cm20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.cm20.Cm20MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

/**
 * <p>
 * Chase M, Miao P. Private Set Intersection in the Internet Setting from Lightweight Oblivious PRF. CRYPTO 2020.
 * pp. 34-63.
 * <p>
 * CM20-PSI协议配置项。
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2022/03/03
 */
public class Cm20PsiConfig extends AbstractMultiPartyPtoConfig implements PsiConfig {
    /**
     * MPOPRF配置项
     */
    private final MpOprfConfig mpOprfConfig;
    /**
     * 过滤器类型
     */
    private final FilterFactory.FilterType filterType;

    private Cm20PsiConfig(Cm20PsiConfig.Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.mpOprfConfig);
        mpOprfConfig = builder.mpOprfConfig;
        filterType = builder.filterType;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.CM20;
    }

    @Override
    public void setEnvType(EnvType envType) {
        mpOprfConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return mpOprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (mpOprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = mpOprfConfig.getSecurityModel();
        }
        return securityModel;
    }

    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    public FilterFactory.FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cm20PsiConfig> {
        /**
         * OPRF类型
         */
        private MpOprfConfig mpOprfConfig;
        /**
         * 过滤器类型
         */
        private FilterFactory.FilterType filterType;

        public Builder() {
            mpOprfConfig = new Cm20MpOprfConfig.Builder().build();
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
        public Cm20PsiConfig build() {
            return new Cm20PsiConfig(this);
        }
    }
}
