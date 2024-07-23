package edu.alibaba.mpc4j.s2pc.pjc.pmap.psibased;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory.PmapPtoType;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf.rr22.Rr22PsiConfig;

/**
 * @author Feng Han
 * @date 2024/7/22
 */
public class PsiBasedPmapConfig extends AbstractMultiPartyPtoConfig implements PmapConfig {
    /**
     * psi config
     */
    private final PsiConfig psiConfig;

    private PsiBasedPmapConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        psiConfig = builder.psiConfig;
    }

    @Override
    public PmapPtoType getPtoType() {
        return PmapPtoType.PSI_BASED;
    }

    public PsiConfig getPsiConfig() {
        return psiConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PsiBasedPmapConfig> {
        /**
         * psi config
         */
        private PsiConfig psiConfig;

        public Builder(boolean silent) {
            psiConfig = new Rr22PsiConfig.Builder(SecurityModel.SEMI_HONEST).build();
        }

        public Builder setPsiConfig(PsiConfig psiConfig) {
            this.psiConfig = psiConfig;
            return this;
        }

        @Override
        public PsiBasedPmapConfig build() {
            return new PsiBasedPmapConfig(this);
        }
    }
}
