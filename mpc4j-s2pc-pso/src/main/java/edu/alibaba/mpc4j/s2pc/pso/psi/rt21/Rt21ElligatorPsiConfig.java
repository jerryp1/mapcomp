package edu.alibaba.mpc4j.s2pc.pso.psi.rt21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

/**
 * RT21-PSI协议配置。论文来源：
 * <p>
 * Mike Rosulek and Ni Trieu. 2021.
 * Compact and Malicious Private Set Intersection for Small Sets.
 * In Proceedings of the 2021 ACM SIGSAC Conference on Computer and Communications Security (CCS '21).
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class Rt21ElligatorPsiConfig extends AbstractMultiPartyPtoConfig implements PsiConfig {
    /**
     * 过滤器类型
     */
    private final FilterFactory.FilterType filterType;

    private Rt21ElligatorPsiConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        filterType = builder.filterType;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.RT21;
    }

    public FilterFactory.FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rt21ElligatorPsiConfig> {
        /**
         * 过滤器类型
         */
        private FilterFactory.FilterType filterType;

        public Builder() {
            filterType = FilterFactory.FilterType.SET_FILTER;
        }

        public Builder setFilterType(FilterFactory.FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Rt21ElligatorPsiConfig build() {
            return new Rt21ElligatorPsiConfig(this);
        }
    }
}