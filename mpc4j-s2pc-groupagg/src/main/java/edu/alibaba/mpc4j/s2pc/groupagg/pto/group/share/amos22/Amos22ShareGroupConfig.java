package edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share.amos22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share.ShareGroupConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.share.ShareGroupFactory.ShareGroupType;

/**
 * AMOS22 shared group aggregation Config.
 *
 * @author Feng Han
 * @date 2023/11/28
 */
public class Amos22ShareGroupConfig extends AbstractMultiPartyPtoConfig implements ShareGroupConfig {
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * Z2 mux circuit config.
     */
    private final Z2MuxConfig z2MuxConfig;
    /**
     * max bit length in one batch
     */
    private final int maxBitLenOneBatch;

    private Amos22ShareGroupConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig, builder.z2MuxConfig);
        z2cConfig = builder.z2cConfig;
        z2MuxConfig = builder.z2MuxConfig;
        maxBitLenOneBatch = builder.maxBitLenOneBatch;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public Z2MuxConfig getZ2MuxConfig() {
        return z2MuxConfig;
    }

    public int getMaxBitLenOneBatch() {
        return maxBitLenOneBatch;
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
        /**
         * Z2 mux circuit config.
         */
        private final Z2MuxConfig z2MuxConfig;
        /**
         * max bit length in 1 batch.
         */
        private final int maxBitLenOneBatch;

        public Builder(boolean silent) {
            this.z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            this.z2MuxConfig = Z2MuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            maxBitLenOneBatch = 1 << 28;
        }

        @Override
        public Amos22ShareGroupConfig build() {
            return new Amos22ShareGroupConfig(this);
        }
    }
}
