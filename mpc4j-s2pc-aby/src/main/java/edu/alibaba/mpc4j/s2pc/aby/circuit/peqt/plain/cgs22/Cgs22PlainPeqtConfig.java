package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.PlainPeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.PlainPeqtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;

/**
 * CGS22 plain private equality test config.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class Cgs22PlainPeqtConfig implements PlainPeqtConfig {
    /**
     * Boolean circuit config
     */
    private final BcConfig bcConfig;
    /**
     * LNOT config
     */
    private final LnotConfig lnotConfig;

    private Cgs22PlainPeqtConfig(Builder builder) {
        assert builder.bcConfig.getEnvType().equals(builder.lnotConfig.getEnvType());
        bcConfig = builder.bcConfig;
        lnotConfig = builder.lnotConfig;
    }

    public BcConfig getBcConfig() {
        return bcConfig;
    }

    public LnotConfig getLnotConfig() {
        return lnotConfig;
    }

    @Override
    public PlainPeqtFactory.PlainPeqtType getPtoType() {
        return PlainPeqtFactory.PlainPeqtType.CGS22;
    }

    @Override
    public void setEnvType(EnvType envType) {
        bcConfig.setEnvType(envType);
        lnotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return bcConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (bcConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = bcConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22PlainPeqtConfig> {
        /**
         * Boolean circuit config
         */
        private BcConfig bcConfig;
        /**
         * LNOT config
         */
        private LnotConfig lnotConfig;

        public Builder(SecurityModel securityModel) {
            bcConfig = BcFactory.createDefaultConfig(securityModel);
            lnotConfig = LnotFactory.createCacheConfig(securityModel);
        }

        public Builder setBcConfig(BcConfig bcConfig) {
            this.bcConfig = bcConfig;
            return this;
        }

        public Builder setLnotConfig(LnotConfig lnotConfig) {
            this.lnotConfig = lnotConfig;
            return this;
        }

        @Override
        public Cgs22PlainPeqtConfig build() {
            return new Cgs22PlainPeqtConfig(this);
        }
    }
}
