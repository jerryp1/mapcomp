package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pmt;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiConfig;

import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiFactory.UcpsiType;

/**
 * SJ23 pmt unbalanced circuit PSI config.
 *
 * @author Liqiang Peng
 * @date 2023/7/17
 */
public class Sj23PmtUcpsiConfig extends AbstractMultiPartyPtoConfig implements UcpsiConfig {
    /**
     * pmt config
     */
    private final PsmConfig psmConfig;
    /**
     * Z2C config
     */
    private final Z2cConfig z2cConfig;

    private Sj23PmtUcpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.psmConfig, builder.z2cConfig);
        psmConfig = builder.psmConfig;
        z2cConfig = builder.z2cConfig;
    }

    @Override
    public UcpsiType getPtoType() {
        return UcpsiType.SJ23_PMT;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public PsmConfig getPsmConfig() {
        return psmConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Sj23PmtUcpsiConfig> {
        /**
         * pmt config
         */
        private PsmConfig psmConfig;
        /**
         * Z2C config
         */
        private Z2cConfig z2cConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            psmConfig = PsmFactory.createDefaultConfig(securityModel, silent);
            z2cConfig = Z2cFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setZ2cConfig(Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        public Builder setPsmConfig(PsmConfig psmConfig) {
            this.psmConfig = psmConfig;
            return this;
        }

        @Override
        public Sj23PmtUcpsiConfig build() {
            return new Sj23PmtUcpsiConfig(this);
        }
    }
}
