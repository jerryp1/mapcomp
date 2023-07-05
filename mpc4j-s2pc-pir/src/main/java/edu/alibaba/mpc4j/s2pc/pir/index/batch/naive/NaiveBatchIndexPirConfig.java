package edu.alibaba.mpc4j.s2pc.pir.index.batch.naive;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
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
public class NaiveBatchIndexPirConfig extends AbstractMultiPartyPtoConfig implements BatchIndexPirConfig {
    /**
     * single index PIR config
     */
    private final SingleIndexPirConfig singleIndexPirConfig;
    /**
     * cuckoo hash type
     */
    private final IntCuckooHashBinFactory.IntCuckooHashBinType cuckooHashBinType;

    public NaiveBatchIndexPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.singleIndexPirConfig);
        singleIndexPirConfig = builder.singleIndexPirConfig;
        // the root single PIR are limited to the following types
        Preconditions.checkArgument(
            (singleIndexPirConfig instanceof Ayaa21SingleIndexPirConfig) ||
            (singleIndexPirConfig instanceof Mcr21SingleIndexPirConfig) ||
            (singleIndexPirConfig instanceof Acls18SingleIndexPirConfig) ||
            (singleIndexPirConfig instanceof Mbfk16SingleIndexPirConfig),
            "Invalid " + SingleIndexPirConfig.class.getSimpleName() + ": "
                + singleIndexPirConfig.getClass().getSimpleName());
        cuckooHashBinType = builder.cuckooHashBinType;
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
            singleIndexPirConfig = new Acls18SingleIndexPirConfig.Builder().build();
            cuckooHashBinType = IntCuckooHashBinFactory.IntCuckooHashBinType.NO_STASH_NAIVE;
        }

        public Builder setSingleIndexPirConfig(SingleIndexPirConfig singleIndexPirConfig) {
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
