package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.php24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPayloadMuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.PlainPlayloadMuxFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewFactory.ViewPtoType;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory;

/**
 * @author Feng Han
 * @date 2024/7/19
 */
public class Php24PkFkViewConfig extends AbstractMultiPartyPtoConfig implements PkFkViewConfig {
    /**
     * plain text mux config
     */
    private final PlainPayloadMuxConfig plainPayloadMuxConfig;
    /**
     * pmap config
     */
    private final PmapConfig pmapConfig;
    /**
     * osn config
     */
    private final OsnConfig osnConfig;
    /**
     * traversal config
     */
    private final PrefixAggConfig prefixAggConfig;

    private Php24PkFkViewConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.plainPayloadMuxConfig, builder.pmapConfig, builder.osnConfig, builder.prefixAggConfig);
        plainPayloadMuxConfig = builder.plainPayloadMuxConfig;
        pmapConfig = builder.pmapConfig;
        osnConfig = builder.osnConfig;
        prefixAggConfig = builder.prefixAggConfig;
    }

    @Override
    public ViewPtoType getPtoType() {
        return ViewPtoType.PHP24;
    }

    public PlainPayloadMuxConfig getPlainPayloadMuxConfig() {
        return plainPayloadMuxConfig;
    }

    public PmapConfig getPmapConfig() {
        return pmapConfig;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public PrefixAggConfig getPrefixAggConfig() {
        return prefixAggConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Php24PkFkViewConfig> {
        /**
         * plain text mux config
         */
        private final PlainPayloadMuxConfig plainPayloadMuxConfig;
        /**
         * pmap config
         */
        private PmapConfig pmapConfig;
        /**
         * osn config
         */
        private final OsnConfig osnConfig;
        /**
         * traversal config
         */
        private final PrefixAggConfig prefixAggConfig;

        public Builder(boolean silent) {
            plainPayloadMuxConfig = PlainPlayloadMuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            pmapConfig = PmapFactory.createDefaultConfig(silent);
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            prefixAggConfig = PrefixAggFactory.createDefaultPrefixAggConfig(SecurityModel.SEMI_HONEST,
                ZlFactory.createInstance(EnvType.STANDARD, 64), silent, PrefixAggTypes.XOR, false);
        }

        public Builder setPmapConfig(PmapConfig pmapConfig) {
            this.pmapConfig = pmapConfig;
            return this;
        }

        @Override
        public Php24PkFkViewConfig build() {
            return new Php24PkFkViewConfig(this);
        }
    }
}
