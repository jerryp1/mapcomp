package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.baseline;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.Z2MuxFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.oneside.OneSideGroupConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.group.oneside.OneSideGroupFactory;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewFactory.PtoType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.PlpsiFactory;

/**
 * configure of baseline PkFk view
 *
 * @author Feng Han
 * @date 2024/7/19
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
     * traversal config
     */
    private final OneSideGroupConfig oneSideGroupConfig;

    private BaselinePkFkViewConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2MuxConfig, builder.plpsiConfig, builder.oneSideGroupConfig);
        z2MuxConfig = builder.z2MuxConfig;
        plpsiConfig = builder.plpsiConfig;
        oneSideGroupConfig = builder.oneSideGroupConfig;
    }

    @Override
    public PtoType getPtoType() {
        return PtoType.BASELINE;
    }

    public Z2MuxConfig getZ2MuxConfig() {
        return z2MuxConfig;
    }

    public PlpsiConfig getPlpsiConfig() {
        return plpsiConfig;
    }

    public OneSideGroupConfig getOneSideGroupConfig() {
        return oneSideGroupConfig;
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
         * traversal config
         */
        private final OneSideGroupConfig oneSideGroupConfig;

        public Builder(boolean silent) {
            z2MuxConfig = Z2MuxFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            plpsiConfig = PlpsiFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            oneSideGroupConfig = OneSideGroupFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
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
