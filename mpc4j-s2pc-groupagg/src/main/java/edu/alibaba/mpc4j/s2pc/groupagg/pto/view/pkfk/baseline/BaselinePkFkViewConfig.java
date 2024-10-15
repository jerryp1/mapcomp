package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewFactory.ViewPtoType;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiFactory;

/**
 * configure of baseline PkFk view
 *
 */
public class BaselinePkFkViewConfig extends AbstractMultiPartyPtoConfig implements PkFkViewConfig {
    /**
     * z2 mux config
     */
    private final Z2MuxConfig z2MuxConfig;
    /**
     * payload psi config
     */
    private final PlpsiConfig plpsiConfig;
    /**
     * osn config
     */
    private final OsnConfig osnConfig;
    /**
     * traversal config
     */
    private final PrefixAggConfig prefixAggConfig;

    private BaselinePkFkViewConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2MuxConfig, builder.plpsiConfig, builder.osnConfig, builder.prefixAggConfig);
        z2MuxConfig = builder.z2MuxConfig;
        plpsiConfig = builder.plpsiConfig;
        osnConfig = builder.osnConfig;
        prefixAggConfig = builder.prefixAggConfig;
    }

    @Override
    public ViewPtoType getPtoType() {
        return ViewPtoType.BASELINE;
    }

    public Z2MuxConfig getZ2MuxConfig() {
        return z2MuxConfig;
    }

    public PlpsiConfig getPlpsiConfig() {
        return plpsiConfig;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public PrefixAggConfig getPrefixAggConfig() {
        return prefixAggConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BaselinePkFkViewConfig> {
        /**
         * z2 mux config
         */
        private final Z2MuxConfig z2MuxConfig;
        /**
         * payload psi config
         */
        private PlpsiConfig plpsiConfig;
        /**
         * osn config
         */
        private final OsnConfig osnConfig;
        /**
         * traversal config
         */
        private final PrefixAggConfig prefixAggConfig;

        public Builder(boolean silent) {
            z2MuxConfig = Z2MuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            plpsiConfig = PlpsiFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            prefixAggConfig = PrefixAggFactory.createDefaultPrefixAggConfig(SecurityModel.SEMI_HONEST,
                ZlFactory.createInstance(EnvType.STANDARD, 64), silent, PrefixAggTypes.XOR, false);
        }

        public Builder setPlpsiConfig(PlpsiConfig plpsiConfig) {
            this.plpsiConfig = plpsiConfig;
            return this;
        }

        @Override
        public BaselinePkFkViewConfig build() {
            return new BaselinePkFkViewConfig(this);
        }
    }
}
