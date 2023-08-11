package edu.alibaba.mpc4j.s2pc.pso.psi.czz22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz22.Czz22ByteEccCwMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

/**
 * CZZ22-PSI协议配置项。
 * <p>
 * Chen, Yu, Min Zhang, Cong Zhang, and Minglang Dong. Private Set Operations from Multi-Query Reverse Private
 * Membership Test. Cryptology ePrint Archive (2022).
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class Czz22PsiConfig implements PsiConfig {
    /**
     * mqrpmt配置项
     */
    private final MqRpmtConfig mqRpmtConfig;

    /**
     * cot 配置项
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * JDK无填充AES-ECB模式名称
     */
    public static final String Cipher_MODE_NAME = "AES/ECB/NoPadding";
    /**
     * JDK的AES算法名称
     */
    public static final String Cipher_ALGORITHM_NAME = "AES";

    private Czz22PsiConfig(Czz22PsiConfig.Builder builder) {
        mqRpmtConfig = builder.mqRpmtConfig;
        coreCotConfig = builder.coreCotConfig;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.CZZ22;
    }

    @Override
    public void setEnvType(EnvType envType) {
        mqRpmtConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return mqRpmtConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (mqRpmtConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = mqRpmtConfig.getSecurityModel();
        }
        if (coreCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = coreCotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public MqRpmtConfig getMqRpmtConfig() {
        return mqRpmtConfig;
    }

    public CoreCotConfig getCoreCotConfig() {return coreCotConfig; }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Czz22PsiConfig> {
        /**
         * mqrpmt类型
         */
        private MqRpmtConfig mqRpmtConfig;
        private CoreCotConfig coreCotConfig;

        public Builder() {
            mqRpmtConfig = new Czz22ByteEccCwMqRpmtConfig.Builder().build();
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setMqRpmtConfig(MqRpmtConfig mqRpmtConfig) {
            this.mqRpmtConfig = mqRpmtConfig;
            return this;
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        @Override
        public Czz22PsiConfig build() {
            return new Czz22PsiConfig(this);
        }
    }
}
