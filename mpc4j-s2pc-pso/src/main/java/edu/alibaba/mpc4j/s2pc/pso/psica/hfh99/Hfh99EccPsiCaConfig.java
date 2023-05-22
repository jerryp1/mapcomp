package edu.alibaba.mpc4j.s2pc.pso.psica.hfh99;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaFactory;

/**
 * ECC-based HFH99 PSI Cardinality config.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class Hfh99EccPsiCaConfig implements PsiCaConfig {
    /**
     * environment
     */
    private EnvType envType;
    /**
     * compress encode
     */
    private final boolean compressEncode;

    private Hfh99EccPsiCaConfig(Builder builder) {
        this.compressEncode = builder.compressEncode;
        envType = EnvType.STANDARD;
    }

    @Override
    public PsiCaFactory.PsiCaType getPtoType() {
        return PsiCaFactory.PsiCaType.HFH99_ECC;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hfh99EccPsiCaConfig> {
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
        public Hfh99EccPsiCaConfig build() {
            return new Hfh99EccPsiCaConfig(this);
        }
    }
}
