package edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.kk13.Kk13OptLcotConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.FilterPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;

/**
 * PRTY20 semi-honest PSI config.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public class Prty20ShPsiConfig extends AbstractMultiPartyPtoConfig implements FilterPsiConfig {
    /**
     * LCOT config
     */
    private final LcotConfig lcotConfig;
    /**
     * PaXoS type
     */
    private final Gf2eDokvsType paxosType;
    /**
     * filter type
     */
    private final FilterType filterType;


    private Prty20ShPsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.lcotConfig);
        lcotConfig = builder.lcotConfig;
        paxosType = builder.paxosType;
        filterType = builder.filterType;
    }

    @Override
    public PsiType getPtoType() {
        return PsiType.PRTY20_SEMI_HONEST;
    }

    public LcotConfig getLcotConfig() {
        return lcotConfig;
    }

    public Gf2eDokvsType getPaxosType() {return paxosType; }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Prty20ShPsiConfig> {
        /**
         * LCOT config
         */
        private final LcotConfig lcotConfig;
        /**
         * PaXoS type
         */
        private Gf2eDokvsType paxosType;
        /**
         * filter type
         */
        private FilterType filterType;

        public Builder() {
            lcotConfig = new Kk13OptLcotConfig.Builder().build();
            paxosType = Gf2eDokvsType.H2_TWO_CORE_GCT;
            filterType = FilterType.SET_FILTER;
        }

        public Builder setPaxosType(Gf2eDokvsType paxosType) {
            Preconditions.checkArgument(Gf2eDokvsFactory.isBinary(paxosType));
            this.paxosType = paxosType;
            return this;
        }

        public Builder setFilterType(FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Prty20ShPsiConfig build() {
            return new Prty20ShPsiConfig(this);
        }
    }
}
