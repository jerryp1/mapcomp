package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory.OneSideGroupType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.PlainBitMuxFactory;

public class Amos22OneSideGroupConfig extends AbstractMultiPartyPtoConfig implements OneSideGroupConfig {

    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;
    /**
     * plain Bit Mux Config
     */
    private final PlainBitMuxConfig plainBitMuxConfig;
    private final int maxBitLenOneBatch;

    private Amos22OneSideGroupConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig, builder.plainBitMuxConfig);
        z2cConfig = builder.z2cConfig;
        plainBitMuxConfig = builder.plainBitMuxConfig;
        maxBitLenOneBatch = builder.maxBitLenOneBatch;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public PlainBitMuxConfig getPlainBitMuxConfig() {
        return plainBitMuxConfig;
    }

    public int getMaxBitLenOneBatch() {
        return maxBitLenOneBatch;
    }

    @Override
    public OneSideGroupType getPtoType() {
        return OneSideGroupType.AMOS22_ONE_SIDE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Amos22OneSideGroupConfig> {
        /**
         * Z2 circuit config.
         */
        private final Z2cConfig z2cConfig;
        /**
         * plain Bit Mux Config
         */
        private final PlainBitMuxConfig plainBitMuxConfig;
        private int maxBitLenOneBatch;

        public Builder(boolean silent) {
            this.z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            this.plainBitMuxConfig = PlainBitMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, null, silent);
            maxBitLenOneBatch = 1 << 28;
        }

        public void setMaxBitLenOneBatch(int maxBitLenOneBatch) {
            this.maxBitLenOneBatch = maxBitLenOneBatch;
        }

        @Override
        public Amos22OneSideGroupConfig build() {
            return new Amos22OneSideGroupConfig(this);
        }
    }
}
