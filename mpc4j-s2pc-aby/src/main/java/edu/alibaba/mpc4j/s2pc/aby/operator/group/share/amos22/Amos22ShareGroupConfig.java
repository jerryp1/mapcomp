package edu.alibaba.mpc4j.s2pc.aby.operator.group.share.amos22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.share.ShareGroupConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.share.ShareGroupFactory.ShareGroupType;

public class Amos22ShareGroupConfig extends AbstractMultiPartyPtoConfig implements ShareGroupConfig {

    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    private Amos22ShareGroupConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig);
        z2cConfig = builder.z2cConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    @Override
    public ShareGroupType getPtoType() {
        return ShareGroupType.AMOS22_SHARE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Amos22ShareGroupConfig> {
        /**
         * Z2 circuit config.
         */
        private final Z2cConfig z2cConfig;

        public Builder(boolean silent) {
            this.z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        @Override
        public Amos22ShareGroupConfig build() {
            return new Amos22ShareGroupConfig(this);
        }
    }
}
