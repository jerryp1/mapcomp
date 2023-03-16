package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.bp.BpDpprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core.Gf2kCoreVoleFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.SspGf2kVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp.SspGf2kVoleFactory;

/**
 * WYKW21-SSP-GF2K-VOLE (semi-honest) config.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Wykw21ShSspGf2kVoleConfig implements SspGf2kVoleConfig {
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * GF2K core VOLE config
     */
    private final Gf2kCoreVoleConfig gf2kCoreVoleConfig;
    /**
     * DPPRF config
     */
    private final BpDpprfConfig bpDpprfConfig;

    private Wykw21ShSspGf2kVoleConfig(Builder builder) {
        assert builder.coreCotConfig.getEnvType().equals(builder.bpDpprfConfig.getEnvType());
        assert builder.coreCotConfig.getEnvType().equals(builder.gf2kCoreVoleConfig.getEnvType());
        coreCotConfig = builder.coreCotConfig;
        gf2kCoreVoleConfig = builder.gf2kCoreVoleConfig;
        bpDpprfConfig = builder.bpDpprfConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public Gf2kCoreVoleConfig getGf2kCoreVoleConfig() {
        return gf2kCoreVoleConfig;
    }

    public BpDpprfConfig getDpprfConfig() {
        return bpDpprfConfig;
    }

    @Override
    public SspGf2kVoleFactory.SspGf2kVoleType getPtoType() {
        return SspGf2kVoleFactory.SspGf2kVoleType.WYKW21_SEMI_HONEST;
    }

    @Override
    public void setEnvType(EnvType envType) {
        coreCotConfig.setEnvType(envType);
        gf2kCoreVoleConfig.setEnvType(envType);
        bpDpprfConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return coreCotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (coreCotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = coreCotConfig.getSecurityModel();
        }
        if (gf2kCoreVoleConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = gf2kCoreVoleConfig.getSecurityModel();
        }
        if (bpDpprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = bpDpprfConfig.getSecurityModel();
        }
        return securityModel;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Wykw21ShSspGf2kVoleConfig> {
        /**
         * core COT config
         */
        private CoreCotConfig coreCotConfig;
        /**
         * GF2K core VOLE config
         */
        private Gf2kCoreVoleConfig gf2kCoreVoleConfig;
        /**
         * DPPRF config
         */
        private BpDpprfConfig bpDpprfConfig;

        public Builder() {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            gf2kCoreVoleConfig = Gf2kCoreVoleFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            bpDpprfConfig = BpDpprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        public Builder setGf2kCoreVoleConfig(Gf2kCoreVoleConfig gf2kCoreVoleConfig) {
            this.gf2kCoreVoleConfig = gf2kCoreVoleConfig;
            return this;
        }

        public Builder setDpprfConfig(BpDpprfConfig bpDpprfConfig) {
            this.bpDpprfConfig = bpDpprfConfig;
            return this;
        }

        @Override
        public Wykw21ShSspGf2kVoleConfig build() {
            return new Wykw21ShSspGf2kVoleConfig(this);
        }
    }
}
