package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.php24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.PlainAndConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.PlainAndFactory.PlainAndType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * Plain and config.
 *
 * @author Li Peng
 * @date 2023/11/8
 */
public class Php24PlainAndConfig extends AbstractMultiPartyPtoConfig implements PlainAndConfig {
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private final Zl zl;

    private Php24PlainAndConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        cotConfig = builder.cotConfig;
        zl = builder.zl;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public PlainAndType getPtoType() {
        return PlainAndType.PHP24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Php24PlainAndConfig> {
        /**
         * COT config
         */
        private CotConfig cotConfig;
        /**
         * Zl
         */
        private final Zl zl;

        public Builder(Zl zl, boolean silent) {
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            this.zl = zl;
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Php24PlainAndConfig build() {
            return new Php24PlainAndConfig(this);
        }
    }
}
