package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirFactory.SingleIndexCpPirType;

/**
 * Simple PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/18
 */
public class SimpleSingleIndexCpPirConfig extends AbstractMultiPartyPtoConfig implements SingleIndexCpPirConfig {

    public SimpleSingleIndexCpPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexCpPirType getProType() {
        return SingleIndexCpPirType.HHCM23_SIMPLE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SimpleSingleIndexCpPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public SimpleSingleIndexCpPirConfig build() {
            return new SimpleSingleIndexCpPirConfig(this);
        }
    }
}
