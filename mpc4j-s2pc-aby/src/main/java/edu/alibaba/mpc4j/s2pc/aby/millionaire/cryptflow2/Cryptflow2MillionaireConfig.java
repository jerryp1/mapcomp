package edu.alibaba.mpc4j.s2pc.aby.millionaire.cryptflow2;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.millionaire.MillionaireConfig;
import edu.alibaba.mpc4j.s2pc.aby.millionaire.MillionaireFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotConfig;

/**
 * Cryptflow2 Millionaire Protocol Config.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public class Cryptflow2MillionaireConfig implements MillionaireConfig {
    /**
     * 1-out-of-n (with n = 2^l) ot protocol config.
     */
    private final LnotConfig lnotConfig;
    /**
     * boolean circuit config.
     */
    private final BcConfig bcConfig;

    private Cryptflow2MillionaireConfig(Cryptflow2MillionaireConfig.Builder builder) {
        lnotConfig = builder.lnotConfig;
        bcConfig = builder.bcConfig;
    }

    public LnotConfig getLnotConfig() {
        return lnotConfig;
    }

    public BcConfig getBcConfig() {
        return bcConfig;
    }

    @Override
    public MillionaireFactory.MillionaireType getPtoType() {
        return MillionaireFactory.MillionaireType.CRYPTFLOW2;
    }

    @Override
    public void setEnvType(EnvType envType) {
        lnotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return lnotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (lnotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = lnotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cryptflow2MillionaireConfig> {
        /**
         * 1-out-of-n (with n = 2^l) ot protocol config
         */
        private LnotConfig lnotConfig;
        /**
         * boolean circuit config.
         */
        private BcConfig bcConfig;

        public Cryptflow2MillionaireConfig.Builder setLnotConfig(LnotConfig lnotConfig) {
            this.lnotConfig = lnotConfig;
            return this;
        }

        public Cryptflow2MillionaireConfig.Builder setBcConfig(BcConfig bcConfig) {
            this.bcConfig = bcConfig;
            return this;
        }

        @Override
        public Cryptflow2MillionaireConfig build() {
            return new Cryptflow2MillionaireConfig(this);
        }
    }
}
