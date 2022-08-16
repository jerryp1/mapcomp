package edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k.Gf2kDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k.Gf2kDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;

/**
 * YWL20-GF2K-DPPRF协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class Ywl20Gf2kDpprfConfig implements Gf2kDpprfConfig {
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;

    private Ywl20Gf2kDpprfConfig(Builder builder) {
        coreCotConfig = builder.coreCotConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    @Override
    public Gf2kDpprfFactory.Gf2kDpprfType getPtoType() {
        return Gf2kDpprfFactory.Gf2kDpprfType.YWL20;
    }

    @Override
    public EnvType getEnvType() {
        return coreCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.MALICIOUS;
        if (coreCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = coreCotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20Gf2kDpprfConfig> {
        /**
         * 核COT协议配置项
         */
        private CoreCotConfig coreCotConfig;

        public Builder(SecurityModel securityModel) {
            coreCotConfig = CoreCotFactory.createDefaultConfig(securityModel);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        @Override
        public Ywl20Gf2kDpprfConfig build() {
            return new Ywl20Gf2kDpprfConfig(this);
        }
    }
}
