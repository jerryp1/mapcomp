package edu.alibaba.mpc4j.s2pc.aby.millionaire.cryptflow2;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.aby.millionaire.MillionaireConfig;
import edu.alibaba.mpc4j.s2pc.aby.millionaire.MillionaireFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * Cryptflow2 Millionaire Protocol Config
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public class Cryptflow2MillionaireConfig implements MillionaireConfig {
    /**
     * COT协议配置项
     */
    private final CotConfig cotConfig;

    private Cryptflow2MillionaireConfig(Cryptflow2MillionaireConfig.Builder builder) {
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public MillionaireFactory.MillionaireType getPtoType() {
        return MillionaireFactory.MillionaireType.CRYPTFLOW2;
    }

    @Override
    public void setEnvType(EnvType envType) {
        cotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return cotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (cotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = cotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cryptflow2MillionaireConfig> {
        /**
         * COT协议配置项
         */
        private CotConfig cotConfig;

        public Builder() {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public Cryptflow2MillionaireConfig.Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Cryptflow2MillionaireConfig build() {
            return new Cryptflow2MillionaireConfig(this);
        }
    }
}
