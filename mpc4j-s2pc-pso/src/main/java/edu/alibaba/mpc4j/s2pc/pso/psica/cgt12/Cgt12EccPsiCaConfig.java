package edu.alibaba.mpc4j.s2pc.pso.psica.cgt12;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaFactory;

/**
 * ECC-based CGT12 PSI Cardinality config.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Cgt12EccPsiCaConfig implements PsiCaConfig {
    /**
     * environment
     */
    private EnvType envType;
    /**
     * compress encode
     */
    private final boolean compressEncode;

    private Cgt12EccPsiCaConfig(Cgt12EccPsiCaConfig.Builder builder) {
        this.compressEncode = builder.compressEncode;
        envType = EnvType.STANDARD;
    }

    @Override
    public PsiCaFactory.PsiCaType getPtoType() {
        return PsiCaFactory.PsiCaType.CGT12_ECC;
    }

    @Override
    public void setEnvType(EnvType envType) {
        this.envType = envType;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgt12EccPsiCaConfig> {
        /**
         * compress encode
         */
        private boolean compressEncode;

        public Builder() {
            compressEncode = true;
        }

        public Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        @Override
        public Cgt12EccPsiCaConfig build() {
            return new Cgt12EccPsiCaConfig(this);
        }
    }
}

