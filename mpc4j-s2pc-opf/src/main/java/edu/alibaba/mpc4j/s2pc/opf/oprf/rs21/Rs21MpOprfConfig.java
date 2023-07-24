package edu.alibaba.mpc4j.s2pc.opf.oprf.rs21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2k.Gf2kDokvsFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.Gf2kNcVoleFactory;

/**
 * RS21-MP-OPRF config.
 *
 * @author Weiran Liu
 * @date 2023/7/24
 */
public class Rs21MpOprfConfig extends AbstractMultiPartyPtoConfig implements MpOprfConfig {
    /**
     * GF2K-NC-VOLE config
     */
    private final Gf2kNcVoleConfig ncVoleConfig;
    /**
     * GF2K-DOUBLY_OKVS type
     */
    private final Gf2kDokvsFactory.Gf2kDokvsType dokvsType;

    private Rs21MpOprfConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.ncVoleConfig);
        ncVoleConfig = builder.ncVoleConfig;
        dokvsType = builder.dokvsType;
    }

    public Gf2kNcVoleConfig getNcVoleConfig() {
        return ncVoleConfig;
    }

    public Gf2kDokvsFactory.Gf2kDokvsType getDokvsType() {
        return dokvsType;
    }

    @Override
    public OprfFactory.OprfType getPtoType() {
        return OprfFactory.OprfType.RS21;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rs21MpOprfConfig> {
        /**
         * GF2K-NC-VOLE config
         */
        private Gf2kNcVoleConfig ncVoleConfig;
        /**
         * GF2K-DOUBLY_OKVS type
         */
        private Gf2kDokvsFactory.Gf2kDokvsType dokvsType;

        public Builder(SecurityModel securityModel) {
            ncVoleConfig = Gf2kNcVoleFactory.createDefaultConfig(securityModel, true);
            dokvsType = Gf2kDokvsFactory.Gf2kDokvsType.H3_CLUSTER_FIELD_BLAZE_GCT;
        }

        public Builder setNcVoleConfig(Gf2kNcVoleConfig ncVoleConfig) {
            this.ncVoleConfig = ncVoleConfig;
            return this;
        }

        public Builder setDokvsType(Gf2kDokvsFactory.Gf2kDokvsType dokvsType) {
            this.dokvsType = dokvsType;
            return this;
        }

        @Override
        public Rs21MpOprfConfig build() {
            return new Rs21MpOprfConfig(this);
        }
    }
}
