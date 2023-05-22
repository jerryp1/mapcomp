package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.rb.RbopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.PsmFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;

/**
 * CGS22 server-payload circuit PSI config.
 *
 * @author Weiran Liu
 * @date 2023/4/19
 */
public class Cgs22CcpsiConfig implements CcpsiConfig {
    /**
     * related batch OPPRF config
     */
    private final RbopprfConfig rbopprfConfig;
    /**
     * private set membership config
     */
    private final PsmConfig psmConfig;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Cgs22CcpsiConfig(Builder builder) {
        assert builder.rbopprfConfig.getEnvType().equals(builder.psmConfig.getEnvType());
        rbopprfConfig = builder.rbopprfConfig;
        psmConfig = builder.psmConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public CcpsiFactory.CcpsiType getPtoType() {
        return CcpsiFactory.CcpsiType.CGS22;
    }

    @Override
    public int getOutputBitNum(int serverElementSize, int clientElementSize) {
        return CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
    }

    @Override
    public void setEnvType(EnvType envType) {
        rbopprfConfig.setEnvType(envType);
        psmConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return rbopprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public RbopprfConfig getRbopprfConfig() {
        return rbopprfConfig;
    }

    public PsmConfig getPsmConfig() {
        return psmConfig;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22CcpsiConfig> {
        /**
         * related batch OPPRF config
         */
        private RbopprfConfig rbopprfConfig;
        /**
         * private set membership config
         */
        private PsmConfig psmConfig;
        /**
         * cuckoo hash bin type
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder(boolean silent) {
            rbopprfConfig = RbopprfFactory.createDefaultConfig();
            psmConfig = PsmFactory.createDefaultConfig(silent);
            cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        }

        public Builder setRbopprfConfig(RbopprfConfig rbopprfConfig) {
            this.rbopprfConfig = rbopprfConfig;
            return this;
        }

        public Builder setPsmConfig(PsmConfig psmConfig) {
            this.psmConfig = psmConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Cgs22CcpsiConfig build() {
            return new Cgs22CcpsiConfig(this);
        }
    }
}
