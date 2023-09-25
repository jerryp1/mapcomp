package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirFactory;

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
     * cuckoo hash type
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;
    /**
     * digest byte length
     */
    private final int digestByteLength;

    public Alpr21SingleKeywordCpPirConfig(Builder builder) {
        super(builder.indexCpPirConfig.getSecurityModel(), builder.indexCpPirConfig);
        indexCpPirConfig = builder.indexCpPirConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
        digestByteLength = builder.digestByteLength;
    }

    @Override
    public SingleKeywordCpPirFactory.SingleKeywordCpPirType getProType() {
        return SingleKeywordCpPirFactory.SingleKeywordCpPirType.ALPR21_SIMPLE_PIR;
    }

    public CuckooHashBinFactory.CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public SingleIndexCpPirConfig getIndexCpPirConfig() {
        return indexCpPirConfig;
    }

    public int getDigestByteLength() {
        return digestByteLength;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Alpr21SingleKeywordCpPirConfig> {

        /**
         * single index client-specific preprocessing PIR config
         */
        private SingleIndexCpPirConfig indexCpPirConfig;
        /**
         * cuckoo hash
         */
        private CuckooHashBinType cuckooHashBinType;
        /**
         * digest byte length
         */
        private int digestByteLength;

        public Builder() {
            indexCpPirConfig = new PianoSingleIndexCpPirConfig.Builder().build();
            cuckooHashBinType = CuckooHashBinType.NO_STASH_NAIVE;
            digestByteLength = CommonConstants.BLOCK_BYTE_LENGTH;
        }

        public Builder setSingleIndexCpPirConfig(SingleIndexCpPirConfig indexCpPirConfig) {
            this.indexCpPirConfig = indexCpPirConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        public Builder setDigestByteLength(int digestByteLength) {
            this.digestByteLength = digestByteLength;
            return this;
        }

        @Override
        public Alpr21SingleKeywordCpPirConfig build() {
            return new Alpr21SingleKeywordCpPirConfig(this);
        }
    }
}
