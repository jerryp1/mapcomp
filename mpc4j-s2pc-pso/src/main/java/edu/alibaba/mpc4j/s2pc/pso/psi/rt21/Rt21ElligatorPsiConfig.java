package edu.alibaba.mpc4j.s2pc.pso.psi.rt21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
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
    /**
     * OKVS类型
     */
    private final Gf2eDokvsFactory.Gf2eDokvsType okvsType;

    private Rt21ElligatorPsiConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        filterType = builder.filterType;
        okvsType = builder.okvsType;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.RT21;
    }

    public FilterFactory.FilterType getFilterType() {
        return filterType;
    }

    public Gf2eDokvsFactory.Gf2eDokvsType getOkvsType() {
        return okvsType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rt21ElligatorPsiConfig> {
        /**
         * 过滤器类型
         */
        private FilterFactory.FilterType filterType;
        /**
         * OKVS类型
         * 使用H3NaiveCluster会快
         */
        private Gf2eDokvsFactory.Gf2eDokvsType okvsType;

        public Builder() {
            filterType = FilterFactory.FilterType.SET_FILTER;
            okvsType = Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT;
//            okvsType = Gf2eDokvsType.MEGA_BIN;
        }

        public Builder setFilterType(FilterFactory.FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        public Builder setOkvsType(Gf2eDokvsFactory.Gf2eDokvsType okvsType) {
            this.okvsType = okvsType;
            return this;
        }

        @Override
        public Rt21ElligatorPsiConfig build() {
            return new Rt21ElligatorPsiConfig(this);
        }
    }
}