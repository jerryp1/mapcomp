package edu.alibaba.mpc4j.s2pc.pso.cpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.CpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.CpsiFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;

/**
 * PSTY19 circuit PSI config.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class Psty19CpsiConfig implements CpsiConfig {
    /**
     * Batch OPPRF config
     */
    private final BopprfConfig bopprfConfig;
    /**
     * private equality test config
     */
    private final PeqtConfig peqtConfig;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Psty19CpsiConfig(Builder builder) {
        assert builder.bopprfConfig.getEnvType().equals(builder.peqtConfig.getEnvType());
        bopprfConfig = builder.bopprfConfig;
        peqtConfig = builder.peqtConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public CpsiFactory.CpsiType getPtoType() {
        return CpsiFactory.CpsiType.PSTY19;
    }

    @Override
    public void setEnvType(EnvType envType) {
        bopprfConfig.setEnvType(envType);
        peqtConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return bopprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public BopprfConfig getBopprfConfig() {
        return bopprfConfig;
    }

    public PeqtConfig getPeqtConfig() {
        return peqtConfig;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Psty19CpsiConfig> {
        /**
         * Batch OPPRF config
         */
        private BopprfConfig bopprfConfig;
        /**
         * private equality test config
         */
        private PeqtConfig peqtConfig;
        /**
         * cuckoo hash bin type
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder() {
            bopprfConfig = BopprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            peqtConfig = PeqtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
            cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        }

        public Builder setBopprfConfig(BopprfConfig bopprfConfig) {
            this.bopprfConfig = bopprfConfig;
            return this;
        }

        public Builder setPeqtConfig(PeqtConfig peqtConfig) {
            this.peqtConfig = peqtConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Psty19CpsiConfig build() {
            return new Psty19CpsiConfig(this);
        }
    }
}
