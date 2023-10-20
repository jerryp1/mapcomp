package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.psty19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.BopprfPlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiFactory.PlpsiType;

/**
 * PSTY19 payload-circuit PSI config.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class Psty19PlpsiConfig extends AbstractMultiPartyPtoConfig implements BopprfPlpsiConfig {
    /**
     * whether the payload should be shared in binary form
     */
    private final boolean isBinaryShare;
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

    private Psty19PlpsiConfig(Psty19PlpsiConfig.Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bopprfConfig, builder.peqtConfig);
        bopprfConfig = builder.bopprfConfig;
        peqtConfig = builder.peqtConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
        isBinaryShare = builder.isBinaryShare;
    }

    @Override
    public PlpsiType getPtoType() {
        return PlpsiType.PSTY19;
    }

    @Override
    public int getOutputBitNum(int serverElementSize, int clientElementSize) {
        return CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
    }

    @Override
    public boolean isBinaryShare() {
        return isBinaryShare;
    }

    @Override
    public BopprfConfig getBopprfConfig() {
        return bopprfConfig;
    }

    @Override
    public PeqtConfig getPeqtConfig() {
        return peqtConfig;
    }

    @Override
    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Psty19PlpsiConfig> {
        /**
         * whether the payload should be shared in binary form
         */
        private boolean isBinaryShare;
        /**
         * Batch OPPRF config
         */
        private final BopprfConfig bopprfConfig;
        /**
         * private equality test config
         */
        private PeqtConfig peqtConfig;
        /**
         * cuckoo hash bin type
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder(boolean silent) {
            bopprfConfig = BopprfFactory.createDefaultConfig();
            peqtConfig = PeqtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
            isBinaryShare = true;
        }

        public Builder setPeqtConfig(PeqtConfig peqtConfig) {
            this.peqtConfig = peqtConfig;
            return this;
        }

        public Builder setShareType(boolean isBinaryShare) {
            this.isBinaryShare = isBinaryShare;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Psty19PlpsiConfig build() {
            return new Psty19PlpsiConfig(this);
        }
    }
}
