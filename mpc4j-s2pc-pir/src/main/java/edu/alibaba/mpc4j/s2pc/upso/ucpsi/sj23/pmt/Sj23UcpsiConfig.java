package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pmt;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;

import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiFactory.UcpsiType;

/**
 * SJ23 unbalanced circuit PSI config.
 *
 * @author Liqiang Peng
 * @date 2023/7/17
 */
public class Sj23UcpsiConfig extends AbstractMultiPartyPtoConfig implements UcpsiConfig {
    /**
     * peqt config
     */
    private final PeqtConfig peqtConfig;
    /**
     * private set membership config
     */
    private final PsmConfig psmConfig;

    private Sj23UcpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.peqtConfig, builder.psmConfig);
        peqtConfig = builder.peqtConfig;
        psmConfig = builder.psmConfig;
    }

    @Override
    public UcpsiType getPtoType() {
        return UcpsiType.CGS22;
    }

    public PeqtConfig getPeqtConfig() {
        return peqtConfig;
    }

    public PsmConfig getPsmConfig() {
        return psmConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Sj23UcpsiConfig> {
        /**
         * peqt config
         */
        private PeqtConfig peqtConfig;
        /**
         * private set membership config
         */
        private PsmConfig psmConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            peqtConfig = PeqtFactory.createDefaultConfig(securityModel, silent);
            psmConfig = PsmFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setPeqtConfig(PeqtConfig peqtConfig) {
            this.peqtConfig = peqtConfig;
            return this;
        }

        public Builder setPsmConfig(PsmConfig psmConfig) {
            this.psmConfig = psmConfig;
            return this;
        }

        @Override
        public Sj23UcpsiConfig build() {
            return new Sj23UcpsiConfig(this);
        }
    }
}
