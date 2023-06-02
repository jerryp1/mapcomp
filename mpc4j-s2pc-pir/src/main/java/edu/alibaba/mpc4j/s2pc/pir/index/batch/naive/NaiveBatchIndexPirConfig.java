package edu.alibaba.mpc4j.s2pc.pir.index.batch.naive;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntNoStashCuckooHashBin;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir.Acls18SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirConfig;

/**
 * naive batch index PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class NaiveBatchIndexPirConfig implements BatchIndexPirConfig {
    /**
     * single index PIR config
     */
    private final SingleIndexPirConfig singleIndexPirConfig;
    /**
     * cuckoo hash type
     */
    private final IntCuckooHashBinFactory.IntCuckooHashBinType cuckooHashBinType;

    public NaiveBatchIndexPirConfig(Builder builder) {
        singleIndexPirConfig = builder.singleIndexPirConfig;
        assert (singleIndexPirConfig instanceof Ayaa21SingleIndexPirConfig) ||
            (singleIndexPirConfig instanceof Mcr21SingleIndexPirConfig) ||
            (singleIndexPirConfig instanceof Acls18SingleIndexPirConfig) ||
            (singleIndexPirConfig instanceof Mbfk16SingleIndexPirConfig);
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    @Override
    public void setEnvType(EnvType envType) {
        if (envType.equals(EnvType.STANDARD_JDK) || envType.equals(EnvType.INLAND_JDK)) {
            throw new IllegalArgumentException("Protocol using " + CommonConstants.MPC4J_NATIVE_FHE_NAME
                + " must not be " + EnvType.STANDARD_JDK.name() + " or " + EnvType.INLAND_JDK.name()
                + ": " + envType.name());
        }
    }

    @Override
    public EnvType getEnvType() {
        return EnvType.STANDARD;
    }

    public SingleIndexPirConfig getSingleIndexPirConfig() {
        return singleIndexPirConfig;
    }

    public IntCuckooHashBinFactory.IntCuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    @Override
    public BatchIndexPirFactory.BatchIndexPirType getPtoType() {
        return BatchIndexPirFactory.BatchIndexPirType.NAIVE_BATCH_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<NaiveBatchIndexPirConfig> {
        /**
         * single index PIR config
         */
        private SingleIndexPirConfig singleIndexPirConfig;
        /**
         * cuckoo hash
         */
        private IntCuckooHashBinFactory.IntCuckooHashBinType cuckooHashBinType;

        public Builder() {
            singleIndexPirConfig = new Acls18SingleIndexPirConfig();
            cuckooHashBinType = IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE;
        }

        public Builder setSingIndexPirConfig(SingleIndexPirConfig singleIndexPirConfig) {
            this.singleIndexPirConfig = singleIndexPirConfig;
            return this;
        }

        public Builder setCuckooHashBinType(IntCuckooHashBinFactory.IntCuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public NaiveBatchIndexPirConfig build() {
            return new NaiveBatchIndexPirConfig(this);
        }
    }
}
