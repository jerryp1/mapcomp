package edu.alibaba.mpc4j.s2pc.aby.basics.b2a.dsz15;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.b2a.B2aFactory.B2aTypes;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * DSZ15 B2a Protocol Config.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
public class Dsz15B2aConfig extends AbstractMultiPartyPtoConfig implements B2aConfig {
    /**
     * COT config.
     */
    private final CotConfig cotConfig;
    /**
     * Zl.
     */
    private final Zl zl;

    private Dsz15B2aConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        cotConfig = builder.cotConfig;
        zl = builder.zl;
    }

    @Override
    public B2aTypes getPtoType() {
        return B2aTypes.DSZ15;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Dsz15B2aConfig> {
        /**
         * COT config
         */
        private final CotConfig cotConfig;
        /**
         * Zl
         */
        private final Zl zl;

        public Builder(Zl zl, boolean silent) {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            this.zl = zl;
        }

        @Override
        public Dsz15B2aConfig build() {
            return new Dsz15B2aConfig(this);
        }
    }
}
