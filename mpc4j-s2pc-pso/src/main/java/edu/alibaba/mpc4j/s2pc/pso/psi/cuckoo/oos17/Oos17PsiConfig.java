package edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.oos17;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.kk13.Kk13OptLcotConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.FilterPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;

/**
 * OOS17-PSI config.
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
public class Oos17PsiConfig extends AbstractMultiPartyPtoConfig implements FilterPsiConfig {
    /**
     * LCOT config
     */
    private final LcotConfig lcotConfig;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * filter type
     */
    private final FilterType filterType;

    private Oos17PsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.lcotConfig);
        lcotConfig = builder.lcotConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
        filterType = builder.filterType;
    }

    public LcotConfig getLcotConfig() {
        return lcotConfig;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    @Override
    public PsiType getPtoType() {
        return PsiType.OOS17;
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Oos17PsiConfig> {
        /**
         * LCOT config
         */
        private final LcotConfig lcotConfig;
        /**
         * cuckoo hash bin type
         */
        private CuckooHashBinType cuckooHashBinType;
        /**
         * filter type
         */
        private FilterType filterType;

        public Builder() {
            lcotConfig = new Kk13OptLcotConfig.Builder().build();
            cuckooHashBinType = CuckooHashBinType.NAIVE_3_HASH;
            filterType = FilterType.SET_FILTER;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        public Builder setFilterType(FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Oos17PsiConfig build() {
            return new Oos17PsiConfig(this);
        }
    }
}
