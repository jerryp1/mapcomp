package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
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

    private Sj23UcpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.peqtConfig);
        peqtConfig = builder.peqtConfig;
    }

    @Override
    public UcpsiType getPtoType() {
        return UcpsiType.CGS22;
    }

    public PeqtConfig getPeqtConfig() {
        return peqtConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Sj23UcpsiConfig> {
        /**
         * peqt config
         */
        private PeqtConfig peqtConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            peqtConfig = PeqtFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setPeqtConfig(PeqtConfig peqtConfig) {
            this.peqtConfig = peqtConfig;
            return this;
        }

        @Override
        public Sj23UcpsiConfig build() {
            return new Sj23UcpsiConfig(this);
        }
    }
}
