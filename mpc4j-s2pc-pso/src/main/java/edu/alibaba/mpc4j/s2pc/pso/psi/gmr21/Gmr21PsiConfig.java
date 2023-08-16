package edu.alibaba.mpc4j.s2pc.pso.psi.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

/**
 * GMR21-PSI配置信息。论文来源：
 * <p>
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class Gmr21PsiConfig extends AbstractMultiPartyPtoConfig implements PsiConfig {
    /**
     * mqrpmt配置项
     */
    private final MqRpmtConfig mqRpmtConfig;

    /**
     * cot 配置项
     */
    private final CoreCotConfig coreCotConfig;

    private Gmr21PsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.mqRpmtConfig, builder.coreCotConfig);
        mqRpmtConfig = builder.mqRpmtConfig;
        coreCotConfig = builder.coreCotConfig;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.GMR21;
    }

    public MqRpmtConfig getMqRpmtConfig() {
        return mqRpmtConfig;
    }

    public CoreCotConfig getCoreCotConfig() {return coreCotConfig; }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gmr21PsiConfig> {
        /**
         * mqrpmt类型
         */
        private MqRpmtConfig mqRpmtConfig;
        private CoreCotConfig coreCotConfig;

        public Builder() {
            mqRpmtConfig = new Gmr21MqRpmtConfig.Builder(true).build();
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder(boolean silent) {
            mqRpmtConfig = new Gmr21MqRpmtConfig.Builder(silent).build();
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setMqRpmtConfig(MqRpmtConfig mqRpmtConfig) {
            this.mqRpmtConfig = mqRpmtConfig;
            return this;
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        @Override
        public Gmr21PsiConfig build() {
            return new Gmr21PsiConfig(this);
        }
    }
}