package edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.MillionaireFactory;

/**
 * RRK+20 Zl DReLU Config.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Rrk20ZlDreluConfig extends AbstractMultiPartyPtoConfig implements ZlDreluConfig {
    /**
     * Millionaire config
     */
    private final MillionaireConfig millionaireConfig;
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;

    private Rrk20ZlDreluConfig(Rrk20ZlDreluConfig.Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.millionaireConfig, builder.z2cConfig);
        millionaireConfig = builder.millionaireConfig;
        z2cConfig = builder.z2cConfig;
    }

    public MillionaireConfig getMillionaireConfig() {
        return millionaireConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    @Override
    public ZlDreluFactory.ZlDreluType getPtoType() {
        return ZlDreluFactory.ZlDreluType.RRK20;
    }

    @Override
    public void setEnvType(EnvType envType) {
        millionaireConfig.setEnvType(envType);
        z2cConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return millionaireConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (millionaireConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = millionaireConfig.getSecurityModel();
        }
        if (z2cConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = z2cConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrk20ZlDreluConfig> {
        /**
         * Millionaire config
         */
        private final MillionaireConfig millionaireConfig;
        /**
         * Z2 circuit config.
         */
        private final Z2cConfig z2cConfig;

        public Builder() {
            millionaireConfig = MillionaireFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        @Override
        public Rrk20ZlDreluConfig build() {
            return new Rrk20ZlDreluConfig(this);
        }
    }
}
