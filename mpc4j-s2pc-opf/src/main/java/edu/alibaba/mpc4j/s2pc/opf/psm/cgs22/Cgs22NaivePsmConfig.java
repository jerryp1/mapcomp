package edu.alibaba.mpc4j.s2pc.opf.psm.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.pesm.PesmConfig;
import edu.alibaba.mpc4j.s2pc.opf.pesm.PesmFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory;

/**
 * CGS22 naive based PSM config.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22NaivePsmConfig extends AbstractMultiPartyPtoConfig implements PsmConfig {
    /**
     * PESM config
     */
    private final PesmConfig pesmConfig;

    private Cgs22NaivePsmConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.pesmConfig);
        pesmConfig = builder.pesmConfig;
    }

    public PesmConfig getPesmConfig() {
        return pesmConfig;
    }

    @Override
    public PsmFactory.PsmType getPtoType() {
        return PsmFactory.PsmType.CGS22_NAIVE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22NaivePsmConfig> {
        /**
         * PESM config
         */
        private PesmConfig pesmConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            pesmConfig = PesmFactory.createDefaultConfig(securityModel, silent);
        }

        public Builder setPesmConfig(PesmConfig pesmConfig) {
            this.pesmConfig = pesmConfig;
            return this;
        }

        @Override
        public Cgs22NaivePsmConfig build() {
            return new Cgs22NaivePsmConfig(this);
        }
    }
}
