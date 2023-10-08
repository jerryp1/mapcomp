package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirFactory.SingleKeywordCpPirType;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.*;

/**
 * ALPR21 client-specific preprocessing PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public class Alpr21SingleKeywordCpPirConfig extends AbstractMultiPartyPtoConfig implements SingleKeywordCpPirConfig {
    /**
     * single index client-specific preprocessing PIR config
     */
    private final SingleIndexCpPirConfig indexCpPirConfig;
    /**
     * sq-OPRF config
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * hash byte length (for keyword)
     */
    private final int hashByteLength;

    public Alpr21SingleKeywordCpPirConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.indexCpPirConfig);
        indexCpPirConfig = builder.indexCpPirConfig;
        sqOprfConfig = builder.sqOprfConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
        hashByteLength = builder.hashByteLength;
    }

    @Override
    public SingleKeywordCpPirType getProType() {
        return SingleKeywordCpPirType.ALPR21;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public SingleIndexCpPirConfig getIndexCpPirConfig() {
        return indexCpPirConfig;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public int hashByteLength() {
        return hashByteLength;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Alpr21SingleKeywordCpPirConfig> {
        /**
         * single index client-specific preprocessing PIR config
         */
        private SingleIndexCpPirConfig indexCpPirConfig;
        /**
         * sq-OPRF config
         */
        private SqOprfConfig sqOprfConfig;
        /**
         * cuckoo hash
         */
        private CuckooHashBinType cuckooHashBinType;
        /**
         * hash byte length (for keyword)
         */
        private int hashByteLength;

        public Builder() {
            indexCpPirConfig = new PianoSingleIndexCpPirConfig.Builder().build();
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            cuckooHashBinType = CuckooHashBinType.NO_STASH_NAIVE;
            hashByteLength = CommonConstants.BLOCK_BYTE_LENGTH;
        }

        public Builder setSingleIndexCpPirConfig(SingleIndexCpPirConfig indexCpPirConfig) {
            this.indexCpPirConfig = indexCpPirConfig;
            return this;
        }

        public Builder setSqOprfConfig(SqOprfConfig sqOprfConfig) {
            this.sqOprfConfig = sqOprfConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        public Builder setHashByteLength(int hashByteLength) {
            MathPreconditions.checkGreaterOrEqual(
                "hash_byte_length", hashByteLength, CommonConstants.STATS_BYTE_LENGTH
            );
            this.hashByteLength = hashByteLength;
            return this;
        }

        @Override
        public Alpr21SingleKeywordCpPirConfig build() {
            return new Alpr21SingleKeywordCpPirConfig(this);
        }
    }
}
