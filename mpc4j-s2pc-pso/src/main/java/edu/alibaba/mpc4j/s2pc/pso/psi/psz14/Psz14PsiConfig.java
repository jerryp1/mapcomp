package edu.alibaba.mpc4j.s2pc.pso.psi.psz14;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.psz14.Psz14OptOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

public class Psz14PsiConfig extends AbstractMultiPartyPtoConfig implements PsiConfig {
    /**
     * OPRF配置项
     */
    private final OprfConfig oprfConfig;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
    /**
     * 过滤器类型
     */
    private final FilterFactory.FilterType filterType;

    private Psz14PsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.oprfConfig);
        oprfConfig = builder.oprfConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
        filterType = builder.filterType;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.PSZ14;
    }

    public OprfConfig getOprfConfig() {
        return oprfConfig;
    }

    public CuckooHashBinFactory.CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public FilterFactory.FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Psz14PsiConfig> {
        /**
         * OPRF类型
         */
        private OprfConfig oprfConfig;
        /**
         * 布谷鸟哈希类型
         */
        private CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
        /**
         * 过滤器类型
         */
        private FilterFactory.FilterType filterType;

        public Builder() {
            oprfConfig = new Psz14OptOprfConfig.Builder().build();
            cuckooHashBinType = CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH;
            filterType = FilterFactory.FilterType.SET_FILTER;
        }

        public Builder setOprfConfig(OprfConfig oprfConfig) {
            this.oprfConfig = oprfConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        public Builder setFilterType(FilterFactory.FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Psz14PsiConfig build() {
            return new Psz14PsiConfig(this);
        }
    }
}
