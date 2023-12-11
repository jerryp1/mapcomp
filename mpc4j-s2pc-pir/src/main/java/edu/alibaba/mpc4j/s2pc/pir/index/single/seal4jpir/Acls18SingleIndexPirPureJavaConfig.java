package edu.alibaba.mpc4j.s2pc.pir.index.single.seal4jpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;




public class Acls18SingleIndexPirPureJavaConfig extends AbstractMultiPartyPtoConfig implements SingleIndexPirConfig {

    public Acls18SingleIndexPirPureJavaConfig(Acls18SingleIndexPirPureJavaConfig.Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public SingleIndexPirFactory.SingleIndexPirType getProType() {
        return SingleIndexPirFactory.SingleIndexPirType.SEAL_4J_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Acls18SingleIndexPirPureJavaConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Acls18SingleIndexPirPureJavaConfig build() {
            return new Acls18SingleIndexPirPureJavaConfig(this);
        }
    }
}
