package edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;

/**
 * RRK+20 Millionaire Protocol Config.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public class Rrk20MillionaireConfig implements MillionaireConfig {
    /**
     * 1-out-of-n (with n = 2^l) ot protocol config.
     */
    private final LnotConfig lnotConfig;
    /**
     * boolean circuit config.
     */
    private final Z2cConfig bcConfig;

    private Rrk20MillionaireConfig(Rrk20MillionaireConfig.Builder builder) {
        lnotConfig = builder.lnotConfig;
        bcConfig = builder.bcConfig;
    }

    public LnotConfig getLnotConfig() {
        return lnotConfig;
    }

    public Z2cConfig getBcConfig() {
        return bcConfig;
    }

    @Override
    public MillionaireFactory.MillionaireType getPtoType() {
        return MillionaireFactory.MillionaireType.RRK20;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrk20MillionaireConfig> {
        /**
         * 1-out-of-n (with n = 2^l) ot protocol config.
         */
        private final LnotConfig lnotConfig;
        /**
         * Z2 circuit config.
         */
        private Z2cConfig bcConfig;

        public Builder(SecurityModel securityModel, boolean silent) {
            bcConfig = Z2cFactory.createDefaultConfig(securityModel, silent);
            if (silent) {
                lnotConfig = LnotFactory.createCacheConfig(securityModel);
            } else {
                lnotConfig = LnotFactory.createDirectConfig(securityModel);
            }
        }

        public Rrk20MillionaireConfig.Builder setBcConfig(Z2cConfig bcConfig) {
            this.bcConfig = bcConfig;
            return this;
        }

        @Override
        public Rrk20MillionaireConfig build() {
            return new Rrk20MillionaireConfig(this);
        }
    }
}
