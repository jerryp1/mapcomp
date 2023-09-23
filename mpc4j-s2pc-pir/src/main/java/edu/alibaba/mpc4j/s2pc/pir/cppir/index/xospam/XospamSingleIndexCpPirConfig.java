package edu.alibaba.mpc4j.s2pc.pir.cppir.index.xospam;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirFactory.SingleIndexCpPirType;

/**
 * XOSPAM client-specific preprocessing PIR config.
 *
 * @author Weiran Liu
 * @date 2023/9/23
 */
public class XospamSingleIndexCpPirConfig extends AbstractMultiPartyPtoConfig implements SingleIndexCpPirConfig {

    public XospamSingleIndexCpPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexCpPirType getProType() {
        return SingleIndexCpPirType.LLP23_XOSPAM;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<XospamSingleIndexCpPirConfig> {

        public Builder() {
            // empty
        }

        @Override
        public XospamSingleIndexCpPirConfig build() {
            return new XospamSingleIndexCpPirConfig(this);
        }
    }
}
