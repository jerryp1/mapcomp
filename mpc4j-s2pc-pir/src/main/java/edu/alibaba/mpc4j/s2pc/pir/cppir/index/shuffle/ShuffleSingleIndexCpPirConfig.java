package edu.alibaba.mpc4j.s2pc.pir.cppir.index.shuffle;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirFactory.SingleIndexCpPirType;

/**
 * Shuffle client-specific preprocessing PIR config.
 *
 * @author Weiran Liu
 * @date 2023/9/23
 */
public class ShuffleSingleIndexCpPirConfig extends AbstractMultiPartyPtoConfig implements SingleIndexCpPirConfig {

    public ShuffleSingleIndexCpPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexCpPirType getProType() {
        return SingleIndexCpPirType.LLP23_SHUFFLE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<ShuffleSingleIndexCpPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public ShuffleSingleIndexCpPirConfig build() {
            return new ShuffleSingleIndexCpPirConfig(this);
        }
    }
}
