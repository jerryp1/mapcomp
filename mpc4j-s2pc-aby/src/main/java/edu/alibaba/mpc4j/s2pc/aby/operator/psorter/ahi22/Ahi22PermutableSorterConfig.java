package edu.alibaba.mpc4j.s2pc.aby.operator.psorter.ahi22;

import edu.alibaba.mpc4j.common.circuit.zl.MpcZlParty;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.Bit2aFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.bcp13.Bcp13ShHammingConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.bcp13.Bcp13ShHammingConfig.Builder;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20.Rrk20ZlMaxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.PermutableSorterConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.PermutableSorterFactory.PermutableSorterTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.greater.zl.ZlGreaterFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;

/**
 * @author Li Peng
 * @date 2023/10/11
 */
public class Ahi22PermutableSorterConfig extends AbstractMultiPartyPtoConfig implements PermutableSorterConfig {
    /**
     * bit2a config.
     */
    private final Bit2aConfig bit2aConfig;
    /**
     * Zl circuit config.
     */
    private final ZlcConfig zlcConfig;

    private Ahi22PermutableSorterConfig(Ahi22PermutableSorterConfig.Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.bit2aConfig, builder.zlcConfig);
        this.bit2aConfig = builder.bit2aConfig;
        this.zlcConfig = builder.zlcConfig;
    }

    @Override
    public PermutableSorterTypes getPtoType() {
        return PermutableSorterTypes.AHI22;
    }

    @Override
    public Zl getZl() {
        return zlcConfig.getZl();
    }

    public Bit2aConfig getBit2aConfig() {
        return bit2aConfig;
    }

    public ZlcConfig getZlcConfig() {
        return zlcConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ahi22PermutableSorterConfig> {
        /**
         * bit2a config.
         */
        private final Bit2aConfig bit2aConfig;
        /**
         * Zl circuit config.
         */
        private ZlcConfig zlcConfig;

        public Builder(Bit2aConfig bit2aConfig) {
            this.bit2aConfig = bit2aConfig;
            this.zlcConfig = ZlcFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, bit2aConfig.getZl());
        }

        public Builder setCotConfig(ZlcConfig zlcConfig) {
            this.zlcConfig = zlcConfig;
            return this;
        }

        @Override
        public Ahi22PermutableSorterConfig build() {
            return new Ahi22PermutableSorterConfig(this);
        }
    }
}
